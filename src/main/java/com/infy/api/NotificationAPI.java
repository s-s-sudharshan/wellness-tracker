package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.NotificationResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.NotificationService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class NotificationAPI {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Environment env;

    // US 10 - Get all notifications for a user (flat list, newest first)
    // Frontend groups by notificationType: CHALLENGE / BADGE / REMINDER
    @GetMapping(value = "/notifications/users/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<NotificationResponseDTO> notifications =
                notificationService.getNotifications(userId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    // US 10 - Unread count for header bell icon
    @GetMapping(value = "/notifications/users/{userId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        Integer count = notificationService.getUnreadCount(userId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    // US 10 - Mark all notifications as read
    @PutMapping(value = "/notifications/users/{userId}/mark-all-read")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        notificationService.markAllAsRead(userId);
        String successMessage = env.getProperty("API.MARK_ALL_READ_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 10 - Mark a single notification as read (ownership enforced via userId)
    @PutMapping(value = "/notifications/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Integer notificationId,
            @RequestParam Integer userId)
            throws WellnessTrackerException {
        notificationService.markAsRead(notificationId, userId);
        String successMessage = env.getProperty("API.MARK_READ_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }
}
