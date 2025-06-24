package org.example;

/**
 * @brief
 * Klasa DTO (Data Transfer Object) reprezentująca żądanie logowania.
 * Służy do przenoszenia danych (nazwy użytkownika i hasła) z żądania HTTP do serwisu uwierzytelniającego.
 */
public class LoginRequest {
    /**
     * Nazwa użytkownika (login) podana w żądaniu logowania.
     */
    private String username;
    /**
     * Hasło podane w żądaniu logowania.
     */
    private String password;

    /**
     * Zwraca nazwę użytkownika.
     * @return Nazwa użytkownika.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Ustawia nazwę użytkownika.
     * @param username Nazwa użytkownika.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Zwraca hasło.
     * @return Hasło.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Ustawia hasło.
     * @param password Hasło.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}