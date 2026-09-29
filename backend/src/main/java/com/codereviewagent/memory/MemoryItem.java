package com.codereviewagent.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {
    private String id;
    private String userId;
    private String developerName;
    private String language;
    private String mistake;
    private String suggestion;
    private String improvement;
    private LocalDateTime timestamp;
}
