package com.guanlong.trading;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordTest {

    @Test
    public void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("========================================");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("========================================");
    }

    @Test
    public void verifyPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String storedHash = "$2a$10$c4RkZxJLfK7VGsKmihJ2zedh2rRS3oQWweIJ3yOj9K8Duy1LBZAdm";

        boolean matches = encoder.matches(rawPassword, storedHash);
        System.out.println("========================================");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Stored hash: " + storedHash);
        System.out.println("Matches: " + matches);
        System.out.println("========================================");

        assertTrue(matches, "Password should match the hash");
    }
}
