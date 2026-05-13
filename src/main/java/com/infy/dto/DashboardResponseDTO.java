package com.infy.dto;

import java.util.List;

import lombok.Data;

@Data
public class DashboardResponseDTO {

    // Last 5 activity entries logged by the user, newest first
    private List<ActivityLogResponseDTO> recentActivities;

    // Joined challenges with ACTIVE status only (live date-based status, not DB column)
    private List<MyChallengeResponseDTO> activeChallenges;

    // This week's activity totals — Monday to today
    private ActivitySummaryDTO weekSummary;

    // Nullable — present only if a weekly goal exists for this week's Monday
    private WeeklyGoalResponseDTO weeklyGoal;

    // Nullable — present only if the user has logged a mood entry today
    private MoodLogResponseDTO todayMood;

    // Unread notification count for header bell icon
    private Integer unreadNotificationCount;

    // US 07 — challenges flagged isFeatured=true, visible to user's department, non-expired
    private List<ActiveChallengeResponseDTO> featuredChallenges;

    // US 07 — unjoined challenges ranked by the user's strongest activity habits (last 30 days)
    // Empty list when user has no activity in the last 30 days
    private List<ActiveChallengeResponseDTO> recommendedChallenges;

    // US 08 — personalised wellness suggestions (challenge + article mix, rule-engine driven)
    private List<RecommendationResponseDTO> recommendations;
}
