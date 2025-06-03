package org.example.controllers;

import org.example.DataBaseRepositories.UserRepository;
import org.example.database.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.SecureRandom;

public class Utils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    public static String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public static boolean isTeacher(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
    }

    public static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public static User getCurrentUser(UserRepository userRepository) {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
    }

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

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}