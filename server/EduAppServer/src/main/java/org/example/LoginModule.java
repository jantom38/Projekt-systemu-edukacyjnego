package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Główna klasa startowa aplikacji Spring Boot.
 * Odpowiedzialna za uruchomienie kontekstu Spring i zainicjowanie wszystkich komponentów.
 */
@SpringBootApplication
public class LoginModule {

    /**
     * Główna metoda uruchamiająca aplikację Spring Boot.
     *
     * @param args Argumenty wiersza poleceń.
     */
    public static void main(String[] args) {
        SpringApplication.run(org.example.LoginModule.class, args);
    }
}