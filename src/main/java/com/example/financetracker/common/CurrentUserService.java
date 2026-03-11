package com.example.financetracker.common;

import com.example.financetracker.user.User;
import com.example.financetracker.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in context");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}

