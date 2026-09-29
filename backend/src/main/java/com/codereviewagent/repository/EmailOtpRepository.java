package com.codereviewagent.repository;

import com.codereviewagent.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {
    Optional<EmailOtp> findTopByEmailAndOtpCodeAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            String email, String otpCode, String purpose
    );

    List<EmailOtp> findByEmailAndPurposeAndIsUsedFalse(String email, String purpose);
}
