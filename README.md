# Lab1 BPMN Campaign Workflow (Spring Boot + PostgreSQL)

Spring Boot приложение реализует бизнес-процесс запуска рекламной кампании по предоставленной BPMN-схеме:
- подготовка кампании менеджером;
- авто-валидация и модерация;
- биллинг (счёт, ожидание оплаты, просрочка);
- активация/пауза/возобновление/остановка;
- таймеры `startAt`, `invoiceDueAt`, `endAt`.

## Запуск

### Локально (без Docker для приложения)

1. Поднять PostgreSQL:
```bash
docker compose up -d postgres
```
2. Запустить приложение:
```bash
./gradlew bootRun
```

### Полностью в Docker (app + postgres)

1. Собрать JAR локально:
```bash
./gradlew bootJar
```
2. Поднять контейнеры:
```bash
docker compose up --build -d
```

Важно: JAR собирается на хосте, в Docker он только копируется из `build/libs`.

По умолчанию используются:
- `DB_URL=jdbc:postgresql://localhost:5432/lab1`
- `DB_USERNAME=lab1`
- `DB_PASSWORD=lab1`

## Основные REST API

Базовый префикс: `/api/campaigns`

- `POST /api/campaigns` - создать черновик кампании
- `POST /api/campaigns/{id}/configure` - настроить кампанию
- `POST /api/campaigns/{id}/creatives` - загрузить креативы
- `POST /api/campaigns/{id}/submit` - отправить на проверку
- `POST /api/campaigns/{id}/validation` - результат авто-валидации
- `POST /api/campaigns/{id}/validation/fix` - исправить ошибки валидации
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
