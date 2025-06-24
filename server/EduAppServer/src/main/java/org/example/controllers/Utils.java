/**
 * @file Utils.java
 * @brief Klasa narzędziowa zawierająca pomocnicze metody statyczne.
 *
 * Zawiera metody do generowania losowych kodów, pobierania informacji
 * o aktualnie zalogowanym użytkowniku oraz sprawdzania jego ról.
 */
package org.example.controllers;

import org.example.DataBaseRepositories.UserRepository;
import org.example.database.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.SecureRandom;

/**
 * @brief Klasa pomocnicza zawierająca statyczne metody używane w kontrolerach.
 */
public class Utils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; ///< Znaki używane do generowania kodu.
    private static final int CODE_LENGTH = 6; ///< Długość generowanego kodu.
    private static final SecureRandom RANDOM = new SecureRandom(); ///< Obiekt do generowania bezpiecznych losowych liczb.

    /**
     * @brief Generuje losowy kod alfanumeryczny o określonej długości.
     * @return Wygenerowany losowy kod.
     */
    public static String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    /**
     * @brief Zwraca nazwę użytkownika aktualnie zalogowanego użytkownika.
     * @return Nazwa użytkownika.
     */
    public static String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * @brief Sprawdza, czy dany obiekt autentykacji ma rolę TEACHER.
     * @param auth Obiekt autentykacji.
     * @return true jeśli użytkownik ma rolę TEACHER, false w przeciwnym razie.
     */
    public static boolean isTeacher(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
    }

    /**
     * @brief Sprawdza, czy dany obiekt autentykacji ma rolę ADMIN.
     * @param auth Obiekt autentykacji.
     * @return true jeśli użytkownik ma rolę ADMIN, false w przeciwnym razie.
     */
    public static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * @brief Pobiera obiekt użytkownika dla aktualnie zalogowanego użytkownika.
     * @param userRepository Repozytorium użytkowników do wyszukania użytkownika.
     * @return Obiekt User aktualnie zalogowanego użytkownika.
     * @throws RuntimeException jeśli użytkownik nie zostanie znaleziony.
     */
    public static User getCurrentUser(UserRepository userRepository) {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
    }

    /**
     * @brief Pobiera ID użytkownika aktualnie zalogowanego użytkownika.
     * @param userRepository Repozytorium użytkowników do wyszukania ID użytkownika.
     * @return ID aktualnie zalogowanego użytkownika.
     * @throws RuntimeException jeśli użytkownik nie zostanie znaleziony.
     */
    public static Long getCurrentUserId(UserRepository userRepository) {
        String username = currentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    org.slf4j.LoggerFactory.getLogger(Utils.class)
                            .error("Użytkownik nie znaleziony z nazwą: {}", username);
                    return new RuntimeException("Użytkownik nie znaleziony");
                })
                .getId();
    }

    /**
     * @brief Zwraca aktualny obiekt autentykacji z kontekstu bezpieczeństwa.
     * @return Obiekt Authentication.
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}