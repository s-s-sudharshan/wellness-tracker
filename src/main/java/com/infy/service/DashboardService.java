package com.infy.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.DashboardResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface DashboardService {

    // US 06 - Aggregated dashboard data for the JWT caller.
    // Returns recent activities, active joined challenges, this week's summary,
    // weekly goal (if set), today's mood (if logged), and unread notification count.
    // userId removed — derived from JWT inside implementation.
    @PreAuthorize("isAuthenticated()")
    public DashboardResponseDTO getDashboard() throws WellnessTrackerException;
}
