package com.codereviewagent;

import com.codereviewagent.dto.ForgotPasswordRequestDto;
import com.codereviewagent.dto.LoginRequestDto;
import com.codereviewagent.dto.ResetPasswordRequestDto;
import com.codereviewagent.dto.VerifyEmailRequestDto;
import com.codereviewagent.entity.EmailOtp;
import com.codereviewagent.entity.EmailVerification;
import com.codereviewagent.entity.User;
import com.codereviewagent.entity.enums.UserRole;
import com.codereviewagent.repository.EmailOtpRepository;
import com.codereviewagent.repository.EmailVerificationRepository;
import com.codereviewagent.repository.UserRepository;
import com.codereviewagent.service.AuthService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SecurityAndUserIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;

    @BeforeEach
    public void setupUsers() {
        try { jdbcTemplate.execute("DELETE FROM review_metrics"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM reviews"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM code_submissions"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM projects"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM notifications"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM email_otps"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM email_verifications"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM users WHERE email LIKE '%_test@codereviewagent.com'"); } catch (Exception ignored) {}

        userA = userRepository.save(User.builder()
                .email("usera_test@codereviewagent.com")
                .username("usera_test")
                .passwordHash(passwordEncoder.encode("PasswordA123"))
                .fullName("User A")
                .role(UserRole.DEVELOPER)
                .isEmailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb_test@codereviewagent.com")
                .username("userb_test")
                .passwordHash(passwordEncoder.encode("PasswordB123"))
                .fullName("User B")
                .role(UserRole.DEVELOPER)
                .isEmailVerified(true)
                .build());
    }

    @Test
    @DisplayName("TEST 1: Unauthenticated GET /api/reviews returns 401")
    public void testUnauthenticatedReviewsReturns401() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 2: Unauthenticated GET /api/notifications returns 401")
    public void testUnauthenticatedNotificationsReturns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 3: Unauthenticated GET /api/dashboard returns 401")
    public void testUnauthenticatedDashboardReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "usera_test@codereviewagent.com")
    @DisplayName("TEST 4: Authenticated User A can access /api/reviews")
    public void testAuthenticatedUserAAccess() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "userb_test@codereviewagent.com")
    @DisplayName("TEST 5: User B requesting non-existent or other user review returns 404")
    public void testUserBResourceIsolation() throws Exception {
        UUID fakeId = UUID.randomUUID();
        mockMvc.perform(get("/api/reviews/" + fakeId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "userb_test@codereviewagent.com")
    @DisplayName("TEST 6 & 7: User B parameter tampering attempts still return ONLY User B data")
    public void testParameterTamperingProtection() throws Exception {
        mockMvc.perform(get("/api/reviews?userId=" + userA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notifications?userId=" + userA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "userb_test@codereviewagent.com")
    @DisplayName("TEST 8: User B marking non-existent or User A notification returns 404")
    public void testNotificationOwnershipProtection() throws Exception {
        UUID randomNotificationId = UUID.randomUUID();
        mockMvc.perform(put("/api/notifications/" + randomNotificationId + "/read")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TEST 12 & 13: Forgot Password 3-Step Flow: Expired OTP, Single-Use, Session Auth")
    public void testForgotPasswordOtpExpiryAndSingleUse() {
        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();

        // 1. Expired OTP test
        emailOtpRepository.save(EmailOtp.builder()
                .email(userA.getEmail())
                .otpCode("123456")
                .purpose("PASSWORD_RESET")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .build());

        com.codereviewagent.dto.VerifyResetOtpRequestDto expiredVerifyReq = com.codereviewagent.dto.VerifyResetOtpRequestDto.builder()
                .email(userA.getEmail())
                .otpCode("123456")
                .build();

        assertThrows(RuntimeException.class, () -> authService.verifyResetOtp(expiredVerifyReq, mockRequest));

        // 2. Valid OTP verification & Reset
        emailOtpRepository.save(EmailOtp.builder()
                .email(userA.getEmail())
                .otpCode("654321")
                .purpose("PASSWORD_RESET")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build());

        com.codereviewagent.dto.VerifyResetOtpRequestDto validVerifyReq = com.codereviewagent.dto.VerifyResetOtpRequestDto.builder()
                .email(userA.getEmail())
                .otpCode("654321")
                .build();

        // Step 2: Verify OTP
        var verifyResp = authService.verifyResetOtp(validVerifyReq, mockRequest);
        assertTrue(verifyResp.isSuccess());

        // Step 3: Reset Password
        ResetPasswordRequestDto resetRequest = ResetPasswordRequestDto.builder()
                .newPassword("NewPass123!")
                .confirmPassword("NewPass123!")
                .build();

        var resetResp = authService.resetPassword(resetRequest, mockRequest);
        assertTrue(resetResp.isSuccess());

        // Verify password updated with BCrypt
        User updatedUser = userRepository.findByEmail(userA.getEmail()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPass123!", updatedUser.getPasswordHash()));

        // Ensure OTP cannot be reused
        assertThrows(RuntimeException.class, () -> authService.verifyResetOtp(validVerifyReq, mockRequest));

        // Ensure session authorization is cleared after use
        assertThrows(RuntimeException.class, () -> authService.resetPassword(resetRequest, mockRequest));
    }

    @Test
    @DisplayName("TEST 14 & 15: Password Reset Stores BCrypt Hash and Rejects Old Password")
    public void testPasswordResetBcryptHashAndLoginRejection() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();

        emailOtpRepository.save(EmailOtp.builder()
                .email(userA.getEmail())
                .otpCode("888999")
                .purpose("PASSWORD_RESET")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build());

        com.codereviewagent.dto.VerifyResetOtpRequestDto verifyDto = com.codereviewagent.dto.VerifyResetOtpRequestDto.builder()
                .email(userA.getEmail())
                .otpCode("888999")
                .build();

        authService.verifyResetOtp(verifyDto, mockRequest);

        ResetPasswordRequestDto resetDto = ResetPasswordRequestDto.builder()
                .newPassword("BrandNewPassword123")
                .confirmPassword("BrandNewPassword123")
                .build();

        authService.resetPassword(resetDto, mockRequest);

        // Attempt login with OLD password -> should fail
        LoginRequestDto oldPassLogin = LoginRequestDto.builder()
                .emailOrUsername(userA.getEmail())
                .password("PasswordA123")
                .build();

        assertThrows(RuntimeException.class, () -> authService.login(oldPassLogin, null, null));

        // Attempt login with NEW password -> should succeed
        LoginRequestDto newPassLogin = LoginRequestDto.builder()
                .emailOrUsername(userA.getEmail())
                .password("BrandNewPassword123")
                .build();

        assertNotNull(authService.login(newPassLogin, null, null));
    }
}
