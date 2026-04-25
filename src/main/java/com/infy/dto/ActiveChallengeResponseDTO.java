package com.infy.dto;

import java.time.LocalDate;

import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;

import lombok.Data;

@Data
public class ActiveChallengeResponseDTO {

    private Integer challengeId;
    private String title;
    private String description;
    private String createdByName;
    private ActivityType metricType;
    private String unit;             // e.g. "steps", "minutes", "liters", "hours" — for display
    private Double goalValue;
    private Difficulty difficulty;
    private LocalDate startDate;
    private LocalDate endDate;
    private String departmentName;   // null for COMPANY_WIDE, "Engineering" for DEPARTMENT scoped
    private Boolean isFeatured;
    private ChallengeStatus status;
    private Boolean alreadyJoined;
}
