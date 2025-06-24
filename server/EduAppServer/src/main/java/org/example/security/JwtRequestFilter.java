package org.example.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtr żądań JWT, który przechwytuje każde żądanie HTTP
 * w celu weryfikacji tokenu JWT i ustawienia kontekstu bezpieczeństwa.
 * Rozszerza OncePerRequestFilter, aby zapewnić wykonanie filtra tylko raz na żądanie.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    /**
     * Serwis do ładowania danych użytkownika.
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Narzędzie do obsługi tokenów JWT.
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Wykonuje logikę filtrowania dla każdego żądania HTTP.
     * Sprawdza nagłówek "Authorization" pod kątem tokenu Bearer JWT.
     * Jeśli token jest obecny i ważny, uwierzytelnia użytkownika
     * i ustawia go w kontekście bezpieczeństwa Spring.
     *
     * @param request  Obiekt HttpServletRequest.
     * @param response Obiekt HttpServletResponse.
     * @param chain    Obiekt FilterChain do przekazania żądania do kolejnego filtra.
     * @throws ServletException Jeśli wystąpi błąd serwletu.
     * @throws IOException      Jeśli wystąpi błąd wejścia/wyjścia.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.getClaimsFromToken(jwt).getSubject();
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        chain.doFilter(request, response);
    }
}