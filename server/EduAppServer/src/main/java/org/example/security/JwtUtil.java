package org.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Klasa narzędziowa do generowania, walidacji i parsowania tokenów JWT.
 */
@Component
public class JwtUtil {

    /**
     * Sekretny klucz używany do podpisywania i weryfikacji tokenów JWT.
     * Powinien być przechowywany bezpiecznie i być odpowiednio złożony w środowisku produkcyjnym.
     */
    private final String secret = "your_secret_key";
    /**
     * Czas wygaśnięcia tokenu w milisekundach (domyślnie 1 godzina).
     */
    private final int expirationTime = 1000 * 60 * 60; // 1 godzina

    /**
     * Generuje nowy token JWT dla podanego użytkownika.
     * Token zawiera nazwę użytkownika jako podmiot, datę wydania oraz datę wygaśnięcia.
     *
     * @param username Nazwa użytkownika, dla którego ma zostać wygenerowany token.
     * @return Wygenerowany token JWT.
     */
    public String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(Algorithm.HMAC512(secret));
    }

    /**
     * Pobiera zdekodowany obiekt JWT z podanego tokenu.
     * Służy do wyodrębniania roszczeń (claims) z tokenu.
     *
     * @param token Token JWT do zdekodowania.
     * @return Zdekodowany obiekt JWT (DecodedJWT).
     */
    public DecodedJWT getClaimsFromToken(String token) {
        return JWT.require(Algorithm.HMAC512(secret))
                .build()
                .verify(token);
    }

    /**
     * Waliduje podany token JWT.
     * Sprawdza, czy token jest poprawnie podpisany i nie wygasł.
     *
     * @param token Token JWT do walidacji.
     * @return True, jeśli token jest ważny; false w przeciwnym razie.
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secret)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}