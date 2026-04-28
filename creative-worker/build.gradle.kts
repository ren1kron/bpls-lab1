plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.kafka:kafka-clients:3.9.0")
}

tasks.named("jar") {
    enabled = false
}
