package com.docusign.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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


    private final UserService userService;
    private final JwtService jwtService;
    private final EmailService emailService;
    
    private static final String USER_NAME = "userName";
    private static final String PASSWORD = "password";
    private static final String MESSAGE = "message";
    private static final String EMAIL = "email";
    private static final String STATUS = "status";

    public AuthController(UserService userService, JwtService jwtService, EmailService emailService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is up and running");
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> body) {
        Optional<User> user = userService.validateUser(body.get(USER_NAME), body.get(PASSWORD));
        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get());
            log.info("User {} logged in successfully", user.get().getUserName());
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
            		.body(Map.of("token", token, "user", user.get(), MESSAGE, "Login Successful"));
        }
        log.warn("Login attempt failed for user {}", body.get(USER_NAME));
        return ResponseEntity.status(401).body("Invalid credentials");
    }
    
    @PostMapping("/forgotpassword")
    public ResponseEntity<Object> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get(EMAIL);
        userService.forgotPassword(email);
        return ResponseEntity.ok("OTP sent to " + email);
    }

    @PostMapping("/verifyotp")
    public ResponseEntity<Object> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get(EMAIL);
        String otp = request.get("otp");

        boolean verified = emailService.verifyOtp(email, otp);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                STATUS, "success",
                MESSAGE, "OTP verified successfully"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                STATUS, "failed",
                MESSAGE, "Invalid or expired OTP"
            ));
        }
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<Object> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get(EMAIL);
        String password = request.get(PASSWORD);

        boolean verified = userService.resetPassword(email, password);

        if (verified) {
            return ResponseEntity.ok(Map.of(
                STATUS, "success",
                MESSAGE, "Password reset successfully"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                STATUS, "failed",
                MESSAGE, "Failed to reset password"
            ));
        }
    }
    


    @PostMapping("/updatepassword")
    public ResponseEntity<Object> updatePassword(@RequestBody Map<String, String> request) {
        String userName = request.get(USER_NAME);
        String password = request.get(PASSWORD);

        Optional<User> user = userService.updatePassword(userName, password);

        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get());
            return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
            		.body(Map.of("token", token, USER_NAME, user.get().getUserName(), MESSAGE, "Login Successful"));
        }
        return ResponseEntity.status(401).body("Failed to Update password");
    }
}
