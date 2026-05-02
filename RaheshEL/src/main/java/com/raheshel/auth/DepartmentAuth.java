package com.raheshel.auth;

import com.raheshel.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class DepartmentAuth {

    // Same rule as original DepartmentAuth.java:
    // username = departmentName, password = departmentName + "2026"
    public void login(String departmentName, String password) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid department login");
        }
        String expected = departmentName + "2026";
        if (!expected.equals(password)) {
            throw new UnauthorizedException("Invalid department login");
        }
    }
}
