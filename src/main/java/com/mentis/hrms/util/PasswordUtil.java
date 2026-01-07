package com.mentis.hrms.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        // Handle null cases
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // Check if password is already hashed
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } else {
            // For backward compatibility with plain passwords
            return rawPassword.equals(encodedPassword);
        }
    }
}