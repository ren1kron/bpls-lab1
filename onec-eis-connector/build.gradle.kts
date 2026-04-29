plugins {
    `java-library`
}

dependencies {
    compileOnly("jakarta.resource:jakarta.resource-api:2.1.0")
    compileOnly("jakarta.transaction:jakarta.transaction-api:2.0.1")
}

tasks.register<Zip>("rar") {
    archiveExtension.set("rar")
    from(tasks.named("jar"))
    from("src/main/rar")
}

tasks.named("assemble") {
    dependsOn("rar")
}
