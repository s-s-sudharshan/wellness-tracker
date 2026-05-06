package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.Notification;
import com.infy.enums.NotificationType;

public interface NotificationRepository extends CrudRepository<Notification, Integer> {

    // All notifications for a user, newest first
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    // Unread count for bell icon
    Integer countByUser_UserIdAndIsReadFalse(Integer userId);

    // Mark all as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
           "WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Integer userId);

    // Ownership-guarded fetch for single mark-as-read
    @Query("SELECT n FROM Notification n " +
           "WHERE n.notificationId = :notificationId AND n.user.userId = :userId")
    Optional<Notification> findByNotificationIdAndUser_UserId(
            @Param("notificationId") Integer notificationId,
            @Param("userId") Integer userId);

    // US 10 - Stable dedup guard for challenge activation notifications.
    // Keyed on userId + notificationType + referenceId (challengeId).
    // This is stable regardless of challenge title — two challenges with the
    // same title will never suppress each other's notifications because their
    // challengeIds differ. Replaces the previous title-based exists check.
    boolean existsByUser_UserIdAndNotificationTypeAndReferenceId(
            Integer userId, NotificationType notificationType, Integer referenceId);
}
