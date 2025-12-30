package com.docusign.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docusign.entity.User;
import com.docusign.security.JwtService;
import com.docusign.service.EmailService;
import com.docusign.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is up and running");
    }

    private final UserService userService;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthController(UserService userService, JwtService jwtService, EmailService emailService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        Optional<User> user = userService.validateUser(body.get("userName"), body.get("password"));
        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get());
            log.info("User {} logged in successfully", user.get().getUserName());
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
            		.body(Map.of("token", token, "user", user.get(), "message", "Login Successful"));
        }
        log.warn("Login attempt failed for user {}", body.get("userName"));
        return ResponseEntity.status(401).body("Invalid credentials");
    }
    
    @PostMapping("/forgotpassword")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.forgotPassword(email);
        return ResponseEntity.ok("OTP sent to " + email);
    }

    @PostMapping("/verifyotp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        boolean verified = emailService.verifyOtp(email, otp);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OTP verified successfully"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                "status", "failed",
                "message", "Invalid or expired OTP"
            ));
        }
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        boolean verified = userService.resetPassword(email, password);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Password reset successfully"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                "status", "failed",
                "message", "Failed to reser password"
            ));
        }
    }
    


    @PostMapping("/updatepassword")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> request) {
        String userName = request.get("userName");
        String password = request.get("password");

        Optional<User> user = userService.updatePassword(userName, password);

        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get());
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
            		.body(Map.of("token", token, "userName", user.get().getUserName(), "message", "Login Successful"));
        }
        return ResponseEntity.status(401).body("Failed to Update password");
    }
}
