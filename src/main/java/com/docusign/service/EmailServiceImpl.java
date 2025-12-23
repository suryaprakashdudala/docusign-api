package com.docusign.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.docusign.constants.EmailTemplates;
import com.docusign.entity.EmailQueue;
import com.docusign.entity.User;
import com.docusign.repository.EmailRepo;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailRepo emailRepo;
    private final JavaMailSender mailSender;
    
    private static final String DOCUMENT = "Document";

    @Override
    public String generateAndSendOtp(String email) {

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        EmailQueue otp = new EmailQueue();
        otp.setEmail(email);
        otp.setOtp(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // valid for 5 minutes
        otp.setUsed(false);
        otp.setSubject(EmailTemplates.OTP_SUBJECT);
        emailRepo.save(otp);
        otp.setId(otp.get_id());
        emailRepo.save(otp);

        sendTemplatedEmail(email, EmailTemplates.OTP_SUBJECT, EmailTemplates.OTP_BODY_TEMPLATE, otpCode);

        return otpCode;
    }

    private void sendTemplatedEmail(String to, String subject, String bodyTemplate, Object... args) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);

        String body = String.format(bodyTemplate, args);
        message.setText(body);

        mailSender.send(message);
    }
    
    @Override
    public boolean verifyOtp(String email, String otpCode) {
        Optional<EmailQueue> otpOptional = emailRepo.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email);

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
                EmailTemplates.USER_CREATION_BODY_TEMPLATE,
                user.getFirstName()+ " "+ user.getLastName(), user.getUserName(), password
            );

    }

    @Override
    public void sendDocumentCompletionEmail(String email, String userName, String documentTitle, String completionLink) {
        sendTemplatedEmail(
            email,
            EmailTemplates.DOCUMENT_COMPLETION_SUBJECT,
            EmailTemplates.DOCUMENT_COMPLETION_BODY_TEMPLATE,
            userName != null ? userName : "User",
            documentTitle != null ? documentTitle : DOCUMENT,
            completionLink
        );
    }

    @Override
    public void sendFinalDocumentEmail(String email, String userName, String documentTitle, String finalLink) {
        sendTemplatedEmail(
            email,
            "Document Completed: " + (documentTitle != null ? documentTitle : DOCUMENT),
            "Hello %s,\n\nThe document '%s' has been signed by all parties.\n\nYou can view the final completed document here:\n%s\n\nThank you,\nDocuSign Clone Team",
            userName != null ? userName : "User",
            documentTitle != null ? documentTitle : DOCUMENT,
            finalLink
        );
    }
}
