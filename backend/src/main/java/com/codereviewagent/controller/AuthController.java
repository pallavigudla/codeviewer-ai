package com.codereviewagent.controller;

import com.codereviewagent.dto.*;
import com.codereviewagent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping({"/register", "/api/auth/register"})
    public ResponseEntity<ApiResponseDto<String>> register(@Valid @RequestBody RegisterRequestDto request) {
        ApiResponseDto<String> response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping({"/verify-email", "/api/auth/verify-email"})
    public ResponseEntity<ApiResponseDto<String>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto request) {
        ApiResponseDto<String> response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/resend-otp", "/api/auth/resend-otp"})
    public ResponseEntity<ApiResponseDto<String>> resendOtp(@RequestParam String email) {
        ApiResponseDto<String> response = authService.resendOtp(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/login", "/api/auth/login"})
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponseDto response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/logout", "/api/auth/logout"})
    public ResponseEntity<ApiResponseDto<String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        ApiResponseDto<String> response = authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/me", "/api/auth/me"})
    public ResponseEntity<ApiResponseDto<UserDto>> me() {
        UserDto currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(ApiResponseDto.success("Authenticated user retrieved", currentUser));
    }

    @PostMapping({"/forgot-password", "/api/auth/forgot-password"})
    public ResponseEntity<ApiResponseDto<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        ApiResponseDto<String> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/verify-reset-otp", "/api/auth/verify-reset-otp"})
    public ResponseEntity<ApiResponseDto<String>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequestDto request,
            HttpServletRequest httpRequest) {
        ApiResponseDto<String> response = authService.verifyResetOtp(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/reset-password", "/api/auth/reset-password"})
    public ResponseEntity<ApiResponseDto<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request,
            HttpServletRequest httpRequest) {
        ApiResponseDto<String> response = authService.resetPassword(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}
