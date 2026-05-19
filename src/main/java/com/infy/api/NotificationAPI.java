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

    // US 10 - Get the JWT caller's notifications (flat list, newest first).
    // Path changed from /notifications/users/{userId} to /notifications/mine.
    // Frontend groups by notificationType: CHALLENGE / BADGE / REMINDER.
    @GetMapping(value = "/notifications/mine")
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications()
            throws WellnessTrackerException {
        List<NotificationResponseDTO> notifications =
                notificationService.getNotifications();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    // US 10 - Unread count for the JWT caller's header bell icon.
    // Path changed from /notifications/users/{userId}/unread-count
    // to /notifications/mine/unread-count.
    @GetMapping(value = "/notifications/mine/unread-count")
    public ResponseEntity<Integer> getUnreadCount()
            throws WellnessTrackerException {
        Integer count = notificationService.getUnreadCount();
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    // US 10 - Mark all notifications as read for the JWT caller.
    // Path changed from /notifications/users/{userId}/mark-all-read
    // to /notifications/mine/mark-all-read.
    @PutMapping(value = "/notifications/mine/mark-all-read")
    public ResponseEntity<String> markAllAsRead()
            throws WellnessTrackerException {
        notificationService.markAllAsRead();
        String successMessage = env.getProperty("API.MARK_ALL_READ_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 10 - Mark a single notification as read (ownership enforced via JWT caller).
    // userId query param removed — ownership derived from JWT inside service.
    @PutMapping(value = "/notifications/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Integer notificationId)
            throws WellnessTrackerException {
        notificationService.markAsRead(notificationId);
        String successMessage = env.getProperty("API.MARK_READ_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }
}