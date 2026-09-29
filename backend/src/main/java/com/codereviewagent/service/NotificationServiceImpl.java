package com.codereviewagent.service;

import com.codereviewagent.dto.NotificationDto;
import com.codereviewagent.dto.NotificationReadRequestDto;
import com.codereviewagent.entity.Notification;
import com.codereviewagent.entity.User;
import com.codereviewagent.exception.ResourceNotFoundException;
import com.codereviewagent.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getCurrentUserNotifications() {
        User user = authenticatedUserService.getCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public void markCurrentUserNotificationsAsRead(NotificationReadRequestDto request) {
        User user = authenticatedUserService.getCurrentUser();
        if (request != null && request.getNotificationIds() != null && !request.getNotificationIds().isEmpty()) {
            notificationRepository.markAsReadByIdsAndUserId(request.getNotificationIds(), user.getId());
        } else {
            notificationRepository.markAllAsReadByUserId(user.getId());
        }
    }

    @Override
    @Transactional
    public void markSingleNotificationAsReadForCurrentUser(UUID notificationId) {
        User user = authenticatedUserService.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (notification.getUser() == null || !notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
