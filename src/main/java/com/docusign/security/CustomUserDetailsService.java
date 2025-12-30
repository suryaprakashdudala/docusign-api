package com.docusign.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.docusign.entity.DocumentCompletion;
import com.docusign.entity.User;
import com.docusign.repository.DocumentCompletionRepo;
import com.docusign.repository.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    
    private final DocumentCompletionRepo documentCompletionRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepo.findByUserName(username);
        if (user.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                    user.get().getUserName(),
                    user.get().getPassword(),
                    getAuthorities(user.get().getRoles())
            );
        }
        
        // Handle external user lookup (potentially email:docToken)
        String lookupEmail = username;
        String docToken = null;
        
        if (username.contains(":")) {
            String[] parts = username.split(":");
            lookupEmail = parts[0];
            docToken = parts[1];
        }

        Optional<DocumentCompletion> externalUser = (docToken != null) 
            ? documentCompletionRepo.findByTokenAndUserId(docToken, lookupEmail)
            : documentCompletionRepo.findByUserId(lookupEmail);

        if (externalUser.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                    externalUser.get().getUserId(),
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_EXTERNAL_USER"))
            );
        }
        throw new UsernameNotFoundException("User not found: " + username);

    }

    private Collection<? extends GrantedAuthority> getAuthorities(List<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return authorities;
    }
}

