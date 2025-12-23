package com.docusign.service;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.docusign.entity.User;
import com.docusign.repository.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final EmailService emailService;

    
    @Override
    public User register(User user) {
    	String plainPassword = generateRandomPassword(8); 
        user.setPassword(encoder.encode(plainPassword));
        user.setFirstTimeLogin(true);
        userRepo.save(user);
        user.setId(user.get_id());
        userRepo.save(user);
        emailService.sendUserCreationMail(user, plainPassword);
        return user;
    }
    
    private String generateRandomPassword(int length) {
        final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lower = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String specials = "!@#$%^&*()_-+=<>?";

        final String allChars = upper + lower + digits + specials;
        SecureRandom random = new SecureRandom();

        StringBuilder password = new StringBuilder();

        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specials.charAt(random.nextInt(specials.length())));

        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        return shuffleString(password.toString(), random);
    }

    /**
     * Shuffles characters in the string.
     */
    private String shuffleString(String input, SecureRandom random) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }


    @Override
    public Optional<User> validateUser(String userName, String password) {
        Optional<User> user = userRepo.findByUserName(userName);
        if (user.isPresent() && encoder.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> forgotPassword(String email) {
        Optional<User> user = userRepo.findByEmail(email);
        if (user.isPresent()) {
            emailService.generateAndSendOtp(email);
            return user;
        } else {
            throw new RuntimeException("User not found with email: " + email);
        }
    }

	@Override
	public boolean resetPassword(String email, String password) {
		 Optional<User> loUser = userRepo.findByEmail(email);
	     if (loUser.isPresent()) {
	        User user = loUser.get();
	       	user.setPassword(encoder.encode(password));
	        userRepo.save(user);
	        return true;
	     } else {
	    	 throw new RuntimeException("User not found with email: " + email);
	     }
	}

	@Override
	public Optional<User> updatePassword(String userName, String password) {
		 Optional<User> loUser = userRepo.findByUserName(userName);
	     if (loUser.isPresent()) {
	        User user = loUser.get();
	       	user.setPassword(encoder.encode(password));
	       	user.setFirstTimeLogin(false);
	        userRepo.save(user);
	     } else {
	    	 throw new RuntimeException("User not found with username: " + userName);
	     }
	     return loUser;
	}

}
