package com.docusign.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.docusign.entity.User;

@Service
public interface UserService {

    public User register(User user);

    public Optional<User> validateUser(String userName, String password);
    
    public Optional<User> forgotPassword(String email);

	public boolean resetPassword(String email, String password);

	public Optional<User> updatePassword(String email, String password);
}
