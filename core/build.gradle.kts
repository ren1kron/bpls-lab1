dependencies {
    api("com.atomikos:transactions-jdbc:6.0.0:jakarta")
    api("com.atomikos:transactions-jta:6.0.0:jakarta")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")
}
