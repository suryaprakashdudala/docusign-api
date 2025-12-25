package com.docusign.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.docusign.constants.EmailTemplates;
import com.docusign.entity.EmailQueue;
import com.docusign.entity.User;
import com.docusign.repository.EmailRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailRepo emailRepo;

    private static final String DOCUMENT = "Document";

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();


    @Override
    public String generateAndSendOtp(String email) {

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        EmailQueue otp = new EmailQueue();
        otp.setEmail(email);
        otp.setOtp(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        otp.setSubject(EmailTemplates.OTP_SUBJECT);
        emailRepo.save(otp);
        otp.setId(otp.get_id());
        emailRepo.save(otp);

        sendTemplatedEmail(email, EmailTemplates.OTP_SUBJECT, String.format(EmailTemplates.OTP_BODY_TEMPLATE, otpCode));

        return otpCode;
    }

    private void sendTemplatedEmail(String to, String subject, String body) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> payload = Map.of(
              "from", fromEmail,
              "to", List.of(to),
              "subject", subject,
              "text", body
            );

            String jsonPayload = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                log.error("Resend API returned error for {}: {}", to, response.body());
                throw new RuntimeException("Resend email failed: " + response.body());
            }
            log.info("Email sent successfully to {} via Resend", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: ", to, e);
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {

        Optional<EmailQueue> otpOptional =
            emailRepo.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

        if (otpOptional.isEmpty()) return false;

        EmailQueue otp = otpOptional.get();

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!otp.getOtp().equals(otpCode)) return false;

        otp.setUsed(true);
        emailRepo.save(otp);

        return true;
    }

    @Scheduled(fixedRate = 30 * 1000)
    public void deleteExpiredOtps() {
        emailRepo.findAll().forEach(otp -> {
            if (otp.getExpiresAt().isBefore(LocalDateTime.now()) || otp.isUsed()) {
                emailRepo.delete(otp);
            }
        });
        System.out.println("Expired OTPs cleaned up at " + LocalDateTime.now());
    }



    @Override
    public void sendUserCreationMail(User user, String password) {

        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setEmail(user.getEmail());
        emailQueue.setOtp(password);
        emailQueue.setSubject(EmailTemplates.USER_CREATION_SUBJECT);
        emailRepo.save(emailQueue);
        emailQueue.setId(emailQueue.get_id());
        emailRepo.save(emailQueue);

        sendTemplatedEmail(
            user.getEmail(),
            EmailTemplates.USER_CREATION_SUBJECT,
            String.format(
                EmailTemplates.USER_CREATION_BODY_TEMPLATE,
                user.getFirstName() + " " + user.getLastName(),
                user.getUserName(),
                password
            )
        );
    }

    @Override
    public void sendDocumentCompletionEmail(
            String email, String userName, String documentTitle, String completionLink) {

        sendTemplatedEmail(
            email,
            EmailTemplates.DOCUMENT_COMPLETION_SUBJECT,
            String.format(
                EmailTemplates.DOCUMENT_COMPLETION_BODY_TEMPLATE,
                userName != null ? userName : "User",
                documentTitle != null ? documentTitle : DOCUMENT,
                completionLink
            )
        );
    }

    @Override
    public void sendFinalDocumentEmail(
            String email, String userName, String documentTitle, String finalLink) {

        sendTemplatedEmail(
            email,
            "Document Completed: " + (documentTitle != null ? documentTitle : DOCUMENT),
            String.format(
                "Hello %s,\n\nThe document '%s' has been signed by all parties.\n\nView final document:\n%s\n\nThanks,\nDocuSign Clone Team",
                userName != null ? userName : "User",
                documentTitle != null ? documentTitle : DOCUMENT,
                finalLink
            )
        );
    }
}
