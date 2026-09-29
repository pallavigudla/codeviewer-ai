package com.codereviewagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationDto {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean isUsed;
    private LocalDateTime createdAt;
}
