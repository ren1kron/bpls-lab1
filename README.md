# Lab1 BPMN Campaign Workflow (Spring Boot + PostgreSQL)

Spring Boot приложение реализует бизнес-процесс запуска рекламной кампании по предоставленной BPMN-схеме:
- подготовка кампании менеджером;
- авто-валидация и модерация;
- биллинг (счёт, ожидание оплаты, просрочка);
- активация/пауза/возобновление/остановка;
- таймеры `startAt`, `invoiceDueAt`, `endAt`.

## Запуск

### Локально (без Docker для приложения)

1. Поднять PostgreSQL и Kafka:
```bash
docker compose up -d postgres creative-postgres zookeeper kafka kafka-init
```
2. Запустить приложение:
```bash
./gradlew bootRun
```

### Полностью в Docker (2 app nodes + postgres + Kafka)

1. Собрать JAR локально:
```bash
./gradlew clean bootJar
```
2. Поднять контейнеры:
```bash
docker compose up --build
```

Важно: JAR собирается на хосте, в Docker он только копируется из `build/libs`.

По умолчанию используются:
- `DB_URL=jdbc:postgresql://localhost:6262/lab1`
- `DB_USERNAME=lab1`
- `DB_PASSWORD=lab1`
- `CREATIVE_DB_URL=jdbc:postgresql://localhost:6263/lab1_creatives`
- `CREATIVE_DB_USERNAME=lab1_creatives`
- `CREATIVE_DB_PASSWORD=lab1_creatives`
- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`

Оба PostgreSQL-контейнера запускаются с `max_prepared_transactions=100`, потому что Atomikos XA использует two-phase commit и PostgreSQL должен поддерживать prepared transactions.

В Docker Compose доступны два узла приложения:
- `http://localhost:26125` -> `app-node-1`
- `http://localhost:26126` -> `app-node-2`

Kafka-топики создаются контейнером `kafka-init`:
- `creative-upload-requests`
- `creative-upload-results`

## Основные REST API

Базовый префикс: `/api/campaigns`

- `POST /api/campaigns` - создать черновик кампании
- `PUT /api/campaigns/{id}` - обновить черновик кампании (только в статусе `DRAFT`)
- `POST /api/campaigns/{id}/configure` - настроить кампанию
- `POST /advertisement/client/{id}/creatives` - поставить загрузку креатива в очередь, ответ `202 Accepted`
- `GET /advertisement/client/{id}/creative-loads/{taskId}` - проверить статус загрузки креатива
- `POST /api/campaigns/{id}/submit` - отправить на проверку (авто-валидация выполняется автоматически)
- `POST /api/campaigns/{id}/moderation` - решение модерации
- `POST /api/campaigns/{id}/moderation/fix` - исправить замечания модерации
- `POST /api/campaigns/{id}/billing/payment-received` - зафиксировать оплату счёта
- `POST /api/campaigns/{id}/billing/due-date-reached` - зафиксировать истечение срока оплаты
- `POST /api/campaigns/{id}/pause` - запросить паузу активной кампании
- `POST /api/campaigns/{id}/resume` - решение возобновить или остановить
- `POST /api/campaigns/{id}/events/budget-exhausted` - событие исчерпания бюджета
- `POST /api/campaigns/{id}/events/end-at-reached` - событие достижения endAt
- `POST /api/campaigns/{id}/timers/tick` - обработать таймеры для кампании
- `POST /api/campaigns/timers/tick` - обработать таймеры для всех кампаний
- `GET /api/campaigns` - список кампаний
- `GET /api/campaigns/{id}` - карточка кампании
- `GET /api/campaigns/{id}/history` - история переходов

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Тесты

```bash
./gradlew test
```

`H2` используется только в тестовом runtime (`testRuntimeOnly`) и только для `./gradlew test`.

## Проверка async creative loading

1. Собрать и поднять окружение:
```bash
./gradlew clean bootJar
docker compose up --build
```
2. Зарегистрировать клиента, создать кампанию, настроить ее, затем вызвать:
```bash
curl -i -X POST http://localhost:26125/advertisement/client/{campaignId}/creatives \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://cdn.example.com/banner.png","type":"IMAGE"}'
```
Ответ содержит `taskId`, `status=PENDING` и `campaignStatus=CREATIVES_LOADING`.
3. Проверить задачу:
```bash
curl -H "Authorization: Bearer {token}" \
  http://localhost:26125/advertisement/client/{campaignId}/creative-loads/{taskId}
```
4. Проверить карточку кампании после обработки:
```bash
curl -H "Authorization: Bearer {token}" http://localhost:26125/advertisement/{campaignId}
```
5. Посмотреть, какой узел обработал событие, и прочитать result topic:
```bash
docker compose logs app-node-1 app-node-2
docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic creative-upload-results --from-beginning
```
