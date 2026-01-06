package com.docusign.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.docusign.entity.User;

@Service
public interface UserService {

    public User register(User user);

    public Optional<User> validateUser(String userName, String password);
    
    public Optional<User> forgotPassword(String userName);

	public boolean resetPassword(String userName, String password);

	public Optional<User> updatePassword(String userName, String password);

	public List<User> findAll();

    public User update(String id, User user);

    public Optional<User> findByUserName(String userName);
}
