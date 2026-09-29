package com.codereviewagent.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailJsServiceImpl implements EmailService {

    @Value("${emailjs.service-id:}")
    private String serviceId;

    @Value("${emailjs.template-id:}")
    private String templateId;

    @Value("${emailjs.public-key:}")
    private String publicKey;

    @Value("${emailjs.private-key:}")
    private String privateKey;

    @Value("${app.origin:http://localhost:8080}")
    private String appOrigin;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void checkConfiguration() {
        // Enable restricted headers in Java HttpURLConnection so Origin header can be sent
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        boolean configured = serviceId != null && !serviceId.isBlank()
                && templateId != null && !templateId.isBlank()
                && publicKey != null && !publicKey.isBlank();
        if (configured) {
            log.info("EmailJS configuration diagnostic: PRESENT (service_id=[{}], template_id=[{}], public_key length=[{}], private_key length=[{}])",
                    serviceId, templateId, publicKey.length(), privateKey != null ? privateKey.length() : 0);
        } else {
            log.warn("EmailJS configuration diagnostic: MISSING or incomplete (service_id set: {}, template_id set: {}, public_key set: {})",
                    serviceId != null && !serviceId.isBlank(),
                    templateId != null && !templateId.isBlank(),
                    publicKey != null && !publicKey.isBlank());
        }
    }

    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode, String purpose) {
        sendOtpEmail(recipientEmail, null, otpCode, purpose);
    }

    @Override
    public void sendOtpEmail(String recipientEmail, String userName, String otpCode, String purpose) {
        log.info("Sending OTP email through EmailJS for recipient [{}] with purpose [{}]", recipientEmail, purpose);

        if (serviceId == null || serviceId.isBlank() || templateId == null || templateId.isBlank() || publicKey == null || publicKey.isBlank()) {
            log.error("EmailJS credentials not configured (EMAILJS_SERVICE_ID/TEMPLATE_ID/PUBLIC_KEY missing).");
            throw new RuntimeException("EmailJS credentials not configured. Cannot send OTP email.");
        }

        try {
            String url = "https://api.emailjs.com/api/v1.0/email/send";
            String name = (userName != null && !userName.isBlank()) ? userName : recipientEmail;

            Map<String, Object> templateParams = new HashMap<>();
            // Exact EmailJS template variables required: email, user_name, otp
            templateParams.put("email", recipientEmail);
            templateParams.put("user_name", name);
            templateParams.put("otp", otpCode);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("service_id", serviceId.trim());
            requestBody.put("template_id", templateId.trim());
            requestBody.put("user_id", publicKey.trim());
            if (privateKey != null && !privateKey.isBlank()) {
                requestBody.put("accessToken", privateKey.trim());
            }
            requestBody.put("template_params", templateParams);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Origin", appOrigin);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("EmailJS response status: {}", response.getStatusCode().value());
                log.info("EmailJS OTP successfully sent to recipient [{}]", recipientEmail);
            } else {
                log.error("EmailJS failed with status: {}", response.getStatusCode().value());
                throw new RuntimeException("EmailJS API failed with HTTP status: " + response.getStatusCode().value() + " - " + response.getBody());
            }
        } catch (HttpStatusCodeException ex) {
            log.error("EmailJS failed with status: {} response body: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new RuntimeException("EmailJS API failed with HTTP status: " + ex.getStatusCode().value() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            log.error("EmailJS dispatch failed for recipient [{}]: {}", recipientEmail, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("EmailJS dispatch failed for recipient [{}]: {}", recipientEmail, ex.getMessage());
            throw new RuntimeException("Failed to send OTP email via EmailJS: " + ex.getMessage(), ex);
        }
    }
}
