package com.codereviewagent.repository;

import com.codereviewagent.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findByUsername(String username);
    Optional<EmailVerification> findByEmailAndOtpCode(String email, String otpCode);
    void deleteByEmail(String email);
    void deleteByUsername(String username);
}
