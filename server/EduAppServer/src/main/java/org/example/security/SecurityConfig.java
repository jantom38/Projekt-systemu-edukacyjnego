package org.example.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @brief
 * Klasa konfiguracji bezpieczeństwa dla aplikacji Spring Boot.
 * Definiuje reguły autoryzacji HTTP, zarządzanie sesjami oraz konfiguruje filtry JWT.
 */
@Configuration
@EnableWebSecurity
//@EnableMethodSecurity // Można odkomentować, aby włączyć zabezpieczenia na poziomie metod
public class SecurityConfig {

    /**
     * Filtr żądań JWT, odpowiedzialny za weryfikację tokenów.
     */
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * Ścieżka do katalogu, gdzie przechowywane są przesyłane pliki.
     * Wartość domyślna to "uploads", ale może być nadpisana przez właściwość `file.upload-dir` w `application.properties`.
     */
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Konfiguruje łańcuch filtrów bezpieczeństwa HTTP.
     * Definiuje reguły dostępu do zasobów, wyłącza CSRF i konfiguruje zarządzanie sesjami jako bezstanowe.
     * Dodaje filtr JWT do łańcucha filtrów.
     *
     * @param http Obiekt HttpSecurity do konfiguracji.
     * @return Skonfigurowany SecurityFilterChain.
     * @throws Exception W przypadku błędu konfiguracji.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Wyłącza ochronę CSRF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/files/**").permitAll() // Umożliwia dostęp do plików bez uwierzytelniania
                        .requestMatchers("/api/auth/login").permitAll() // Umożliwia dostęp do endpointu logowania bez uwierzytelniania
                        // .requestMatchers("/api/courses/quizzes/*/edit").hasRole("TEACHER") // Przykład reguły dostępu opartej na roli (zakomentowany)
                        .anyRequest().authenticated() // Wymaga uwierzytelnienia dla wszystkich pozostałych żądań
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Ustawia zarządzanie sesjami jako bezstanowe (nie używa sesji HTTP)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class); // Dodaje filtr JWT przed filtrem uwierzytelniania nazwy użytkownika i hasła

        return http.build();
    }

    /**
     * Udostępnia bean PasswordEncoder, używany do kodowania i weryfikacji haseł.
     * Używa BCryptPasswordEncoder.
     *
     * @return Instancja PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}