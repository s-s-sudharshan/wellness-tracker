package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.NotificationResponseDTO;
import com.infy.enums.NotificationType;
import com.infy.exception.WellnessTrackerException;

public interface NotificationService {

    // Internal — called by BadgeServiceImpl, ChallengeServiceImpl,
    // ChallengeParticipantServiceImpl, ChallengeStatusSyncService, and ReminderScheduler.
    // No @PreAuthorize — internal call, not a public API entry point.
    // referenceId: nullable. See notification namespace rules in SESSION_FEEDBACK_V10.md.
    public void createNotification(Integer userId, NotificationType type,
            String title, String message, Integer referenceId);

    // US 10 - Get all notifications for the JWT caller (flat list, newest first).
    @PreAuthorize("isAuthenticated()")
    public List<NotificationResponseDTO> getNotifications() throws WellnessTrackerException;

    // US 10 - Unread count for bell icon.
    @PreAuthorize("isAuthenticated()")
    public Integer getUnreadCount() throws WellnessTrackerException;

    // US 10 - Mark all notifications as read for the JWT caller.
    @PreAuthorize("isAuthenticated()")
    public void markAllAsRead() throws WellnessTrackerException;

    // US 10 - Mark a single notification as read (ownership guarded by JWT caller).
    @PreAuthorize("isAuthenticated()")
    public void markAsRead(Integer notificationId) throws WellnessTrackerException;
}
