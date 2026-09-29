package com.codereviewagent.entity.enums;

public enum NotificationType {
    REVIEW_COMPLETED,
    MEMORY_SAVED,
    WEEKLY_SUMMARY,
    SYSTEM,
    
    // Legacy constants for backwards compatibility with pre-existing database records
    MEMBER_JOINED,
    TEAM_CREATED,
    TEAM_JOINED,
    MEMBER_LEFT,
    CODING_STANDARD_UPDATED,
    ROLE_CHANGED,
    REVIEW_REQUESTED,
    REVIEW_SUBMITTED
}


