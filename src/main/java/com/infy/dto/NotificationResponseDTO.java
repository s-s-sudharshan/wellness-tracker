package com.infy.dto;

import java.time.LocalDateTime;

import com.infy.enums.NotificationType;

import lombok.Data;

@Data
public class NotificationResponseDTO {

    private Integer notificationId;
    private Integer userId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
