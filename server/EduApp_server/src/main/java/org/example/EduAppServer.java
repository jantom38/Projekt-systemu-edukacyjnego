package org.example;

import com.google.gson.Gson;
import static spark.Spark.*;

public class EduAppServer {
    public static void main(String[] args) {
        // Configure port
        port(8080);

        // Enable CORS
        enableCORS();

        // Simple in-memory "database" of users
        UserDatabase userDatabase = new UserDatabase();
        userDatabase.addUser(new User("admin", "admin123"));
        userDatabase.addUser(new User("teacher", "teacher123"));
        userDatabase.addUser(new User("student", "student123"));

        Gson gson = new Gson();

        // Login endpoint
        post("/login", (req, res) -> {
            res.type("application/json");

            try {
                LoginRequest loginRequest = gson.fromJson(req.body(), LoginRequest.class);

                if (loginRequest == null || loginRequest.username == null || loginRequest.password == null) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Invalid request format"));
                }

                User user = userDatabase.getUser(loginRequest.username);

                if (user == null || !user.getPassword().equals(loginRequest.password)) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Invalid username or password"));
                }

                // Successful login
                return gson.toJson(new LoginResponse(true, "Login successful"));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
            }
        });

        // Simple test endpoint
        get("/test", (req, res) -> {
            return "Server is running";
        });
    }

    // Helper method to enable CORS
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
            response.header("Access-Control-Allow-Credentials", "true");
        });
    }
}

// Helper classes
class LoginRequest {
    String username;
    String password;
}

class LoginResponse {
    boolean success;
    String message;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

class ErrorResponse {
    String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}

class User {
    private String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

class UserDatabase {
    private java.util.Map<String, User> users = new java.util.HashMap<>();

    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    public User getUser(String username) {
        return users.get(username);
    }
}