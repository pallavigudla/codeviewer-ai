package com.codereviewagent.service;

import com.codereviewagent.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    ApiResponseDto<String> register(RegisterRequestDto request);
    ApiResponseDto<String> verifyEmail(VerifyEmailRequestDto request);
    ApiResponseDto<String> resendOtp(String email);
    AuthResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    ApiResponseDto<String> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    UserDto getCurrentAuthenticatedUser();
    ApiResponseDto<String> forgotPassword(ForgotPasswordRequestDto request);
    ApiResponseDto<String> verifyResetOtp(VerifyResetOtpRequestDto request, HttpServletRequest httpRequest);
    ApiResponseDto<String> resetPassword(ResetPasswordRequestDto request, HttpServletRequest httpRequest);
}
