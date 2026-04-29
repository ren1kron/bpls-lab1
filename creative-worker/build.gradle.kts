plugins {
    id("org.springframework.boot")
    war
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
}

tasks.named("jar") {
    enabled = false
}
