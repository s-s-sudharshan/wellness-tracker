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

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Log LOGGER = LogFactory.getLog(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // Internal method — called by BadgeServiceImpl, ChallengeServiceImpl,
    // ChallengeParticipantServiceImpl, and ChallengeStatusSyncService.
    // referenceId is nullable — only set for challenge activation notifications.
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

    // US 10 - Flat list, newest first. Returns [] when no notifications exist.
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(Integer userId)
            throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        List<Notification> notifications =
                notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);

        List<NotificationResponseDTO> responseList = new ArrayList<>();
        for (Notification n : notifications) {
            responseList.add(mapToDTO(n));
        }
        return responseList;
    }

    // US 10 - Unread count for bell icon
    @Override
    @Transactional(readOnly = true)
    public Integer getUnreadCount(Integer userId) throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }
        Integer count = notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
        return count != null ? count : 0;
    }

    // US 10 - Mark all notifications as read
    @Override
    public void markAllAsRead(Integer userId) throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }
        notificationRepository.markAllAsRead(userId);
    }

    // US 10 - Mark a single notification as read (ownership guarded)
    @Override
    public void markAsRead(Integer notificationId, Integer userId)
            throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, userId)
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
