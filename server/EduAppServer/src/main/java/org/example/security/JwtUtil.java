package org.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String secret = "your_secret_key";
    private final int expirationTime = 1000 * 60 * 60; // 1 godzina

    public String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(Algorithm.HMAC512(secret));
    }

    public DecodedJWT getClaimsFromToken(String token) {
        return JWT.require(Algorithm.HMAC512(secret))
                .build()
                .verify(token);
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secret)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}