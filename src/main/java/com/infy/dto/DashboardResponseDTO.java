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
}
