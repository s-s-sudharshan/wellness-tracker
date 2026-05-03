package com.infy.dto;

import java.time.LocalDateTime;

import com.infy.enums.BadgeStatus;
import com.infy.enums.CriteriaType;

import lombok.Data;

@Data
public class BadgeResponseDTO {

    private Integer badgeId;
    private String title;
    private String description;
    private CriteriaType criteriaType;
    private Double criteriaValue;
    private String badgeIcon;
    private String badgeColor;

    // Progress fields — always populated so frontend can show progress bars
    private double progress;
    private int progressPct;        // capped at 100

    // Status — EARNED / IN_PROGRESS / LOCKED
    private BadgeStatus badgeStatus;

    // Earned info
    private boolean earned;
    private LocalDateTime earnedAt; // null if not yet earned

    // True only on the call that first awards this badge — used for toast
    private boolean newlyUnlocked;
}
