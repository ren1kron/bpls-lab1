# WildFly JCA EIS deployment

The 1C integration is implemented as an outbound Jakarta Connectors resource adapter.

## Build artifacts

```bash
./gradlew clean assemble
```

Artifacts:

- `onec-eis-connector/build/distributions/onec-eis-connector-0.0.1-SNAPSHOT.rar`
- `api-app/build/libs/api-app-0.0.1-SNAPSHOT.war`
- `creative-worker/build/libs/creative-worker-0.0.1-SNAPSHOT.war`

Use the non-`plain` WAR files for WildFly.

## Configure WildFly

Deploy the resource adapter first:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --command="deploy onec-eis-connector/build/distributions/onec-eis-connector-0.0.1-SNAPSHOT.rar"
```

Create the JNDI connection factory:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar:add(archive=onec-eis-connector-0.0.1-SNAPSHOT.rar,transaction-support=NoTransaction)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory:add(class-name=ifmo.se.lab1app.eis.onec.ra.OneCManagedConnectionFactory,jndi-name=java:/eis/OneCConnectionFactory,enabled=true,use-java-context=true)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory/config-properties=endpointUrl:add(value=http://93.100.213.224:1337/aviasales/hs/campaigns/events)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory/config-properties=username:add(value=api)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory/config-properties=password:add(value=123)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory/config-properties=connectTimeoutMillis:add(value=5000)
/subsystem=resource-adapters/resource-adapter=onec-eis-connector-0.0.1-SNAPSHOT.rar/connection-definitions=OneCConnectionFactory/config-properties=requestTimeoutMillis:add(value=10000)
:reload
```

Enable the integration for deployed Spring applications with environment variables or JVM properties:

```bash
EIS_ENABLED=true
EIS_CONNECTION_FACTORY_JNDI_NAME=java:/eis/OneCConnectionFactory
EIS_FAIL_ON_ERROR=false
```

The database and Kafka properties are the same as for local/Docker launch:

```bash
DB_URL=jdbc:postgresql://localhost:6262/lab1
DB_USERNAME=lab1
DB_PASSWORD=lab1
CREATIVE_DB_URL=jdbc:postgresql://localhost:6263/lab1_creatives
CREATIVE_DB_USERNAME=lab1_creatives
CREATIVE_DB_PASSWORD=lab1_creatives
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Deploy applications after the resource adapter exists:

```bash
$WILDFLY_HOME/bin/jboss-cli.sh --connect --command="deploy api-app/build/libs/api-app-0.0.1-SNAPSHOT.war"
$WILDFLY_HOME/bin/jboss-cli.sh --connect --command="deploy creative-worker/build/libs/creative-worker-0.0.1-SNAPSHOT.war"
```

`api-app` is a regular web WAR. `creative-worker` is packaged as a WAR with a `ServletContextListener` that starts the non-web Spring context and the Kafka consumer when WildFly deploys it.
