package com.infy.service;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface WeeklyGoalService {

    // Any authenticated user can set their own weekly goal.
    // Caller identity derived from JWT inside the implementation.
    @PreAuthorize("isAuthenticated()")
    public Integer saveWeeklyGoal(WeeklyGoalRequestDTO requestDTO) throws WellnessTrackerException;

    // Returns the JWT caller's own weekly goal for the given week start date.
    @PreAuthorize("isAuthenticated()")
    public WeeklyGoalResponseDTO getWeeklyGoal(LocalDate weekStartDate)
            throws WellnessTrackerException;
}
