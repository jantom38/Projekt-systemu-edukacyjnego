package org.example.security;

import org.example.DataBaseRepositories.UserRepository;
import org.example.database.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Usługa implementująca interfejs UserDetailsService z Spring Security.
 * Odpowiedzialna za ładowanie danych użytkownika na podstawie jego nazwy.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repozytorium do zarządzania operacjami na bazie danych użytkowników.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Ładuje dane użytkownika na podstawie podanej nazwy użytkownika.
     *
     * @param username Nazwa użytkownika, dla którego mają zostać załadowane dane.
     * @return Obiekt UserDetails zawierający dane użytkownika.
     * @throws UsernameNotFoundException Jeśli użytkownik o podanej nazwie nie zostanie znaleziony.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}