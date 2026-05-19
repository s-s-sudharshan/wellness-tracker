package com.infy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.NotificationResponseDTO;
import com.infy.entity.Notification;
import com.infy.entity.User;
import com.infy.enums.NotificationType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.NotificationRepository;
import com.infy.repository.UserRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Log LOGGER = LogFactory.getLog(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // Internal method — called by other services, not from API layer.
    // userId is the TARGET recipient — not derived from JWT (scheduler sends to many users).
    // Silently logs and returns on any failure so the caller is never interrupted.
    @Override
    public void createNotification(Integer userId, NotificationType type,
            String title, String message, Integer referenceId) {
        try {
            Optional<User> optional = userRepository.findById(userId);
            if (optional.isEmpty()) {
                LOGGER.warn("createNotification skipped — user not found: userId=" + userId);
                return;
            }
            Notification notification = new Notification();
            notification.setUser(optional.get());
            notification.setNotificationType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setReferenceId(referenceId);
            notificationRepository.save(notification);
        } catch (Exception e) {
            LOGGER.error("Failed to create notification for userId=" + userId, e);
        }
    }

    // US 10 - Get the JWT caller's notifications, newest first.
    // Returns [] when no notifications exist.
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications()
            throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        List<Notification> notifications =
                notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(callerId);

        List<NotificationResponseDTO> responseList = new ArrayList<>();
        for (Notification n : notifications) {
            responseList.add(mapToDTO(n));
        }
        return responseList;
    }

    // US 10 - Unread count for the JWT caller.
    @Override
    @Transactional(readOnly = true)
    public Integer getUnreadCount() throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();
        Integer count = notificationRepository.countByUser_UserIdAndIsReadFalse(callerId);
        return count != null ? count : 0;
    }

    // US 10 - Mark all notifications as read for the JWT caller.
    @Override
    public void markAllAsRead() throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();
        notificationRepository.markAllAsRead(callerId);
    }

    // US 10 - Mark a single notification as read (ownership guarded).
    // Ownership: the notification must belong to the JWT caller.
    @Override
    public void markAsRead(Integer notificationId) throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, callerId)
                .orElseThrow(
                        () -> new WellnessTrackerException("Service.NOTIFICATION_NOT_FOUND"));

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponseDTO mapToDTO(Notification n) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setNotificationId(n.getNotificationId());
        dto.setUserId(n.getUser().getUserId());
        dto.setNotificationType(n.getNotificationType());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setIsRead(n.getIsRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}