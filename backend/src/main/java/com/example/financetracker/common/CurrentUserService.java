package com.example.financetracker.common;

import com.example.financetracker.user.User;
import com.example.financetracker.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        final String resolvedUsername;
        if (principal instanceof UserDetails details) {
            resolvedUsername = details.getUsername();
        } else if (principal instanceof String s && !"anonymousUser".equals(s)) {
            resolvedUsername = s;
        } else {
            resolvedUsername = null;
        }

        if (resolvedUsername == null || resolvedUsername.isBlank()) {
            throw new IllegalStateException("No authenticated user in context");
        }

        return userRepository.findByUsername(resolvedUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + resolvedUsername));
    }
}
