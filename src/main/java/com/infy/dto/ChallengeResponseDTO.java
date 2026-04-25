package com.infy.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.VisibilityType;

import lombok.Data;

@Data
public class ChallengeResponseDTO {

    private Integer challengeId;
    private String title;
    private String description;
    private Integer createdBy;
    private String createdByName;
    private ActivityType metricType;
    private Double goalValue;
    private Difficulty difficulty;
    private LocalDate startDate;
    private LocalDate endDate;
    private VisibilityType visibilityType;
    private Integer departmentId;
    private String departmentName;
    private Boolean isFeatured;
    private ChallengeStatus status;
    private LocalDateTime createdAt;
}
