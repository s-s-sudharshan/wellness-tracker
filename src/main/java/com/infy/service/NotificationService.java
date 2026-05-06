package com.infy.service;

import java.util.List;

import com.infy.dto.NotificationResponseDTO;
import com.infy.enums.NotificationType;
import com.infy.exception.WellnessTrackerException;

public interface NotificationService {

    // Internal — called by BadgeServiceImpl, ChallengeServiceImpl,
    // ChallengeParticipantServiceImpl, and ChallengeStatusSyncService.
    //
    // referenceId: nullable. Pass the challengeId when the notification is
    // challenge-scoped (activation trigger) so the dedup check in
    // ChallengeStatusSyncService can key on userId + type + referenceId
    // instead of the unstable title. Pass null for badge and join notifications
    // (those are naturally deduped by DB constraints or join guards).
    public void createNotification(Integer userId, NotificationType type,
            String title, String message, Integer referenceId);

    // US 10 - Get all notifications for a user (flat list, newest first)
    public List<NotificationResponseDTO> getNotifications(Integer userId)
            throws WellnessTrackerException;

    // US 10 - Unread count for bell icon
    public Integer getUnreadCount(Integer userId) throws WellnessTrackerException;

    // US 10 - Mark all notifications as read
    public void markAllAsRead(Integer userId) throws WellnessTrackerException;

    // US 10 - Mark a single notification as read (ownership guarded)
    public void markAsRead(Integer notificationId, Integer userId)
            throws WellnessTrackerException;
}
