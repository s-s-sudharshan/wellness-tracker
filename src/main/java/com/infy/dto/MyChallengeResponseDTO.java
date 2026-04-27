package com.infy.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.ParticipantStatus;

import lombok.Data;

@Data
public class MyChallengeResponseDTO {

    // Participant info
    private Integer participantId;
    private LocalDateTime joinedAt;
    private ParticipantStatus participantStatus;

    // Challenge info
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
    private Integer daysRemaining;   // 0 = ends today, positive = days left
    private ChallengeStatus challengeStatus;

    // Progress — calculated live from activity_logs
    private Double actualValue;
    private Integer progressPct;
}
