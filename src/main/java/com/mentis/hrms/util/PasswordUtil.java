package com.mentis.hrms.util;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class PasswordUtil {

    // Simple SHA-256 password hashing
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);

            System.out.println("[DEBUG] PasswordUtil: Encoded password");
            System.out.println("[DEBUG]   Input: " + rawPassword);
            System.out.println("[DEBUG]   Output (first 20 chars): " +
                    encoded.substring(0, Math.min(20, encoded.length())) + "...");

            return encoded;
        } catch (Exception e) {
            // Fallback to simple Base64 encoding
            System.err.println("[ERROR] SHA-256 failed: " + e.getMessage());
            String fallback = Base64.getEncoder().encodeToString(
                    rawPassword.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] Using Base64 fallback: " + fallback);
            return fallback;
        }
    }

    public boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            System.out.println("[DEBUG] Null password comparison");
            return false;
        }

        String encodedInput = encodePassword(rawPassword);
        boolean matches = encodedInput.equals(storedPassword);

        System.out.println("[DEBUG] PasswordUtil: Password match = " + matches);
        System.out.println("[DEBUG]   Raw input: " + rawPassword);
        System.out.println("[DEBUG]   Stored: " + storedPassword);
        System.out.println("[DEBUG]   Encoded input: " + encodedInput);

        return matches;
    }
}