package com.raheshel.auth;

import com.raheshel.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class AdminAuth {

    // Same credentials as original AdminAuth.java
    private static final String ADMIN_USERNAME = "Administrator";
    private static final String ADMIN_PASSWORD = "Admin@123";

    public void login(String username, String password) {
        if (!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password)) {
            throw new UnauthorizedException("Invalid admin credentials");
        }
    }
}
