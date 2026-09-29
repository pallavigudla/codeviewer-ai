package com.codereviewagent.controller;

import com.codereviewagent.dto.ApiResponseDto;
import com.codereviewagent.dto.NotificationDto;
import com.codereviewagent.dto.NotificationReadRequestDto;
import com.codereviewagent.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping({"/notifications", "/api/notifications"})
    public ResponseEntity<ApiResponseDto<List<NotificationDto>>> getNotifications() {
        List<NotificationDto> notifications = notificationService.getCurrentUserNotifications();
        return ResponseEntity.ok(ApiResponseDto.success("Notifications retrieved successfully", notifications));
    }

    @PutMapping({"/notifications/read", "/api/notifications/read"})
    public ResponseEntity<ApiResponseDto<String>> markNotificationsAsRead(
            @RequestBody(required = false) NotificationReadRequestDto request) {
        notificationService.markCurrentUserNotificationsAsRead(request);
        return ResponseEntity.ok(ApiResponseDto.success("Notifications marked as read successfully"));
    }

    @PutMapping({"/notifications/{id}/read", "/api/notifications/{id}/read"})
    public ResponseEntity<ApiResponseDto<String>> markSingleNotificationAsRead(@PathVariable UUID id) {
        notificationService.markSingleNotificationAsReadForCurrentUser(id);
        return ResponseEntity.ok(ApiResponseDto.success("Notification marked as read successfully"));
    }
}
