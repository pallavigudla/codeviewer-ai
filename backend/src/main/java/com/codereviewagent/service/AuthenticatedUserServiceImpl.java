package com.codereviewagent.service;

import com.codereviewagent.entity.User;
import com.codereviewagent.exception.UnauthorizedException;
import com.codereviewagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("Unauthenticated API access attempt blocked.");
            throw new UnauthorizedException("Authentication required. Please log in.");
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank()) {
            throw new UnauthorizedException("Invalid authentication session.");
        }

        Optional<User> userOpt = userRepository.findByEmail(principalName.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(principalName.trim());
        }

        User user = userOpt.orElseThrow(() -> new UnauthorizedException("Authenticated user record not found in database."));

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Account is not verified. Please verify your email.");
        }

        return user;
    }
}
