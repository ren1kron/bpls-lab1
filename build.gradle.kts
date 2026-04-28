plugins {
    id("org.springframework.boot") version "4.0.2" apply false
    java
}

group = "ifmo.se"
version = "0.0.1-SNAPSHOT"
description = "lab1-app"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    dependencies {
        add("implementation", platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))
        add("testImplementation", platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))

        add("compileOnly", "org.projectlombok:lombok:1.18.42")
        add("annotationProcessor", "org.projectlombok:lombok:1.18.42")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testRuntimeOnly", "com.h2database:h2")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    configurations.named("compileOnly") {
        extendsFrom(configurations.getByName("annotationProcessor"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
