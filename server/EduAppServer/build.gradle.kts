plugins {
    java
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.example"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Podstawowe zależności Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Security - konieczne dla autentykacji i autoryzacji
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Baza danych
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.3.1.Final")

    // Dodatkowe przydatne zależności
    implementation("com.fasterxml.jackson.core:jackson-databind") // dla lepszej obsługi JSON
    implementation("jakarta.validation:jakarta.validation-api") // walidacja danych

    // (opcjonalnie) Lombok - upraszcza boilerplate code
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testy
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test") // testy zabezpieczeń
}

tasks.test {
    useJUnitPlatform()
}