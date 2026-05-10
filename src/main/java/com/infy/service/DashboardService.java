package com.infy.service;

import com.infy.dto.DashboardResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface DashboardService {

    // US 06 - Aggregated dashboard data for a user.
    // Returns recent activities, active joined challenges, this week's summary,
    // weekly goal (if set), today's mood (if logged), and unread notification count.
    public DashboardResponseDTO getDashboard(Integer userId) throws WellnessTrackerException;
}
