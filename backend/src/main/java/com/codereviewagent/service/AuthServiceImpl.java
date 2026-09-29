package com.codereviewagent.service;

import com.codereviewagent.dto.*;
import com.codereviewagent.entity.EmailOtp;
import com.codereviewagent.entity.EmailVerification;
import com.codereviewagent.entity.User;
import com.codereviewagent.entity.enums.UserRole;
import com.codereviewagent.exception.AuthException;
import com.codereviewagent.exception.EmailNotVerifiedException;
import com.codereviewagent.exception.InvalidOtpException;
import com.codereviewagent.exception.UserAlreadyExistsException;
import com.codereviewagent.repository.EmailOtpRepository;
import com.codereviewagent.repository.EmailVerificationRepository;
import com.codereviewagent.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticatedUserService authenticatedUserService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public ApiResponseDto<String> register(RegisterRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        // 1. Check if an ACTIVE / VERIFIED account already exists for email or username
        if (userRepository.existsByEmailAndIsEmailVerifiedTrue(email)) {
            throw new UserAlreadyExistsException("Email is already registered: " + email);
        }
        if (userRepository.existsByUsernameAndIsEmailVerifiedTrue(username)) {
            throw new UserAlreadyExistsException("Username is already taken: " + username);
        }

        // Clean up any unverified user entries if present
        userRepository.findByEmail(email).ifPresent(u -> {
            if (!u.isEmailVerified()) {
                userRepository.delete(u);
            }
        });
        userRepository.findByUsername(username).ifPresent(u -> {
            if (!u.isEmailVerified()) {
                userRepository.delete(u);
            }
        });

        // Clean up previous pending verification records for this email or username
        emailVerificationRepository.deleteByEmail(email);
        emailVerificationRepository.deleteByUsername(username);

        String otpCode = generate6DigitOtp();

        // Create PENDING verification record
        EmailVerification pendingRegistration = EmailVerification.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : UserRole.DEVELOPER)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        emailVerificationRepository.save(pendingRegistration);
        log.info("Pending registration generated OTP for email [{}]", email);

        // Send OTP email via EmailJS integration
        try {
            emailService.sendOtpEmail(email, username, otpCode, "EMAIL_VERIFICATION");
        } catch (Exception ex) {
            log.error("Failed to send OTP via EmailJS for recipient [{}]: {}", email, ex.getMessage());
        }

        return ApiResponseDto.success("Registration initiated. A 6-digit OTP has been sent to your email.");
    }

    @Override
    @Transactional
    public ApiResponseDto<String> verifyEmail(VerifyEmailRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        String otpCode = request.getOtpCode() != null ? request.getOtpCode().trim() : "";

        if (otpCode.length() != 6) {
            throw new InvalidOtpException("Invalid or Expired OTP.");
        }

        EmailVerification pending = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Invalid or Expired OTP."));

        if (!pending.getOtpCode().equals(otpCode)) {
            throw new InvalidOtpException("Invalid or Expired OTP.");
        }

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP.");
        }

        // Clean up unverified user record if present
        userRepository.findByEmail(email).ifPresent(u -> {
            if (!u.isEmailVerified()) {
                userRepository.delete(u);
            }
        });
        userRepository.findByUsername(pending.getUsername()).ifPresent(u -> {
            if (!u.isEmailVerified()) {
                userRepository.delete(u);
            }
        });

        // Save permanent ACTIVE / VERIFIED user ONLY AFTER successful OTP verification
        User user = User.builder()
                .email(pending.getEmail())
                .username(pending.getUsername())
                .passwordHash(pending.getPasswordHash())
                .fullName(pending.getFullName())
                .role(pending.getRole() != null ? pending.getRole() : UserRole.DEVELOPER)
                .isEmailVerified(true)
                .build();

        userRepository.save(user);

        // Remove temporary pending verification record
        emailVerificationRepository.delete(pending);

        return ApiResponseDto.success("Email verified successfully! Your account has been created.");
    }

    @Override
    @Transactional
    public ApiResponseDto<String> resendOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmailAndIsEmailVerifiedTrue(cleanEmail)) {
            throw new AuthException("Account is already verified. Please log in.");
        }

        EmailVerification pending = emailVerificationRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new AuthException("No pending registration found for email: " + cleanEmail));

        String newOtp = generate6DigitOtp();

        try {
            emailService.sendOtpEmail(cleanEmail, pending.getUsername(), newOtp, "EMAIL_VERIFICATION");
        } catch (Exception ex) {
            log.error("Failed to resend OTP via EmailJS for recipient [{}]: {}", cleanEmail, ex.getMessage());
        }

        pending.setOtpCode(newOtp);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        emailVerificationRepository.save(pending);

        return ApiResponseDto.success("A new 6-digit OTP has been sent to your email.");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String identifier = request.getEmailOrUsername().trim();

        // Find user by email or username
        Optional<User> userOpt = userRepository.findByEmail(identifier.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(identifier);
        }

        if (userOpt.isEmpty()) {
            Optional<EmailVerification> pendingEmail = emailVerificationRepository.findByEmail(identifier.toLowerCase());
            Optional<EmailVerification> pendingUsername = emailVerificationRepository.findByUsername(identifier);
            if (pendingEmail.isPresent() || pendingUsername.isPresent()) {
                throw new EmailNotVerifiedException("Please verify your email before logging in.");
            }
            throw new AuthException("Invalid username/email or password");
        }

        User user = userOpt.get();

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid username/email or password");
        }

        // Establish secure Spring Security authentication in context & session
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (httpRequest != null && httpResponse != null) {
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);
        }

        UserDto userDto = mapToUserDto(user);

        return AuthResponseDto.builder()
                .expiresInSeconds(86400L)
                .user(userDto)
                .message("Login successful")
                .build();
    }

    @Override
    public ApiResponseDto<String> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        if (httpRequest != null) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        return ApiResponseDto.success("Logout successful.");
    }

    @Override
    public UserDto getCurrentAuthenticatedUser() {
        User user = authenticatedUserService.getCurrentUser();
        return mapToUserDto(user);
    }

    @Override
    @Transactional
    public ApiResponseDto<String> forgotPassword(ForgotPasswordRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailAndIsEmailVerifiedTrue(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<EmailOtp> activeOtps = emailOtpRepository.findByEmailAndPurposeAndIsUsedFalse(user.getEmail(), "PASSWORD_RESET");
            for (EmailOtp active : activeOtps) {
                if (active.getCreatedAt() != null && active.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                    throw new AuthException("Please wait 60 seconds before requesting another password reset OTP.");
                }
            }
            String otpCode = generate6DigitOtp();
            try {
                saveAndSendOtp(user.getEmail(), user.getUsername(), otpCode, "PASSWORD_RESET");
            } catch (Exception ex) {
                log.error("Failed to send password reset OTP via EmailJS for recipient: {}", email);
            }
        }

        // Generic response to avoid user enumeration
        return ApiResponseDto.success("If an active account exists for that email, a password reset code has been sent.");
    }

    @Override
    @Transactional
    public ApiResponseDto<String> verifyResetOtp(VerifyResetOtpRequestDto request, HttpServletRequest httpRequest) {
        String email = request.getEmail().trim().toLowerCase();
        String otpCode = request.getOtpCode() != null ? request.getOtpCode().trim() : "";

        if (otpCode.length() != 6) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        EmailOtp otp = emailOtpRepository
                .findTopByEmailAndOtpCodeAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
                        email, otpCode, "PASSWORD_RESET")
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP.");
        }

        User user = userRepository.findByEmailAndIsEmailVerifiedTrue(email)
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP."));

        // Consume OTP (single use)
        otp.setUsed(true);
        emailOtpRepository.save(otp);

        // Store short-lived password reset authorization in HttpSession
        if (httpRequest != null) {
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("passwordResetVerified", true);
            session.setAttribute("passwordResetUserId", user.getId().toString());
            session.setAttribute("passwordResetExpiresAt", LocalDateTime.now().plusMinutes(10));
        }

        return ApiResponseDto.success("OTP verified successfully. You can now set a new password.");
    }

    @Override
    @Transactional
    public ApiResponseDto<String> resetPassword(ResetPasswordRequestDto request, HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            throw new AuthException("Password reset session expired. Please request a new OTP.");
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new AuthException("Password reset session expired. Please request a new OTP.");
        }

        Boolean verified = (Boolean) session.getAttribute("passwordResetVerified");
        String userIdStr = (String) session.getAttribute("passwordResetUserId");
        LocalDateTime expiresAt = (LocalDateTime) session.getAttribute("passwordResetExpiresAt");

        if (verified == null || !verified || userIdStr == null || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            session.removeAttribute("passwordResetVerified");
            session.removeAttribute("passwordResetUserId");
            session.removeAttribute("passwordResetExpiresAt");
            throw new AuthException("Password reset session expired. Please request a new OTP.");
        }

        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (newPassword == null || newPassword.length() < 6) {
            throw new AuthException("Password must be at least 6 characters.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new AuthException("Passwords do not match.");
        }

        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new AuthException("User account not found."));

        // Password must be BCrypt hashed. Never store plaintext password.
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove server-side reset authorization after successful password reset
        session.removeAttribute("passwordResetVerified");
        session.removeAttribute("passwordResetUserId");
        session.removeAttribute("passwordResetExpiresAt");

        return ApiResponseDto.success("Password reset successfully. You can now log in with your new password.");
    }

    private String generate6DigitOtp() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void saveAndSendOtp(String email, String username, String otpCode, String purpose) {
        List<EmailOtp> existingOtps = emailOtpRepository.findByEmailAndPurposeAndIsUsedFalse(email, purpose);
        for (EmailOtp oldOtp : existingOtps) {
            oldOtp.setUsed(true);
        }
        if (!existingOtps.isEmpty()) {
            emailOtpRepository.saveAll(existingOtps);
        }

        emailService.sendOtpEmail(email, username, otpCode, purpose);

        EmailOtp emailOtp = EmailOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        emailOtpRepository.save(emailOtp);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isEmailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
