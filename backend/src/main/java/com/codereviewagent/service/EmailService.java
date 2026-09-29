package com.codereviewagent.service;

public interface EmailService {
    void sendOtpEmail(String recipientEmail, String otpCode, String purpose);
    void sendOtpEmail(String recipientEmail, String userName, String otpCode, String purpose);
}
