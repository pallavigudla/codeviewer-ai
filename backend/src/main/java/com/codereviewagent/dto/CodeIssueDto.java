package com.codereviewagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeIssueDto {
    private String severity;       // "HIGH", "MEDIUM", "LOW"
    private Integer line;          // line number starting from 1 (or 0 if unidentifiable)
    private String code;           // exact problematic code expression or line
    private String problem;        // short title/description of what is wrong
    private String why;            // detailed explanation of why it is wrong
    private String fix;            // fix guidance
    private String correctedCode;  // exact corrected code snippet
}
