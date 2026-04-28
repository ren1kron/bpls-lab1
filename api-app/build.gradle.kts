plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    developmentOnly("org.springframework.boot:spring-boot-devtools:4.0.2")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose:4.0.2")
}

tasks.named("jar") {
    enabled = false
}
