package com.codereviewagent.service;

import com.codereviewagent.dto.NotificationDto;
import com.codereviewagent.dto.NotificationReadRequestDto;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDto> getCurrentUserNotifications();
    void markCurrentUserNotificationsAsRead(NotificationReadRequestDto request);
    void markSingleNotificationAsReadForCurrentUser(UUID notificationId);
}
