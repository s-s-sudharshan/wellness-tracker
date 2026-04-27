package com.infy.dto;

import java.time.LocalDate;
import java.util.List;

import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;

import lombok.Data;

@Data
public class LeaderboardResponseDTO {

    // Challenge context — frontend needs this to render the leaderboard header
    private Integer challengeId;
    private String challengeTitle;
    private ActivityType metricType;
    private String unit;
    private Double goalValue;
    private Difficulty difficulty;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysRemaining;
    private ChallengeStatus challengeStatus;
    private Integer totalParticipants;

    // Requesting user's own position — shown separately at top/bottom of leaderboard
    private Integer currentUserRank;
    private Double currentUserValue;
    private Integer currentUserProgressPct;

    // Full ranked list
    private List<LeaderboardEntryDTO> entries;
}
