package com.docusign.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String DOCUMENT = "Document";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;



    @Override
    public String generateAndSendOtp(String email) {

        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        EmailQueue otp = new EmailQueue();
        otp.setEmail(email);
        otp.setOtp(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        otp.setSubject(EmailTemplates.OTP_SUBJECT);

        emailRepo.save(otp);
        otp.setId(otp.getObjectId());
        emailRepo.save(otp);

        sendTemplatedEmail(
                email,
                EmailTemplates.OTP_SUBJECT,
                String.format(EmailTemplates.OTP_BODY_TEMPLATE, otpCode)
        );

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

            var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent successfully to {} via Resend. Status: {}", to, response.statusCode());
            } else {
                log.error("Failed to send email to {}. Status: {}, Response: {}", to, response.statusCode(), response.body());
                throw new InternalError("Resend API returned error: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send email to {}: ", to, e);
            throw new InternalError("Failed to send email to " + to, e);
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
        log.info("Expired OTPs cleaned up at " + LocalDateTime.now());
    }



    @Override
    public void sendUserCreationMail(User user, String password) {

        EmailQueue emailQueue = new EmailQueue();
        emailQueue.setEmail(user.getEmail());
        emailQueue.setOtp(password);
        emailQueue.setSubject(EmailTemplates.USER_CREATION_SUBJECT);
        emailRepo.save(emailQueue);
        emailQueue.setId(emailQueue.getObjectId());
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
            String.format(EmailTemplates.FINAL_DOCUMENT_SUBJECT, documentTitle != null ? documentTitle : DOCUMENT),
            String.format(
                EmailTemplates.FINAL_DOCUMENT_BODY_TEMPLATE,
                userName != null ? userName : "User",
                documentTitle != null ? documentTitle : DOCUMENT,
                finalLink
            )
        );
    }

}
