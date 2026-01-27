plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.0.21"
}

group = "com.datahub"
version = "0.0.1-SNAPSHOT"
description = "geo_poc"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // OpenSearch Client (AWS OpenSearch 2.11.0 호환)
    implementation("org.opensearch.client:opensearch-java:2.11.0")
    implementation("org.opensearch.client:opensearch-rest-client:2.11.0")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Kotlin 클래스 직렬화 지원 (data class, nullable 등)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // LocalDate, LocalDateTime 등 Java 8+ 날짜 타입 직렬화 지원
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.locationtech.jts:jts-core:1.20.0")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.uber:h3:4.1.1")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

