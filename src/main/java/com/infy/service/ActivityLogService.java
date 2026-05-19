package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;

public interface ActivityLogService {

    // Any authenticated user can log their own activity.
    // Caller identity derived from JWT inside the implementation.
    @PreAuthorize("isAuthenticated()")
    public Integer createActivityLog(ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException;

    // Ownership enforced in service: log must belong to JWT caller.
    @PreAuthorize("isAuthenticated()")
    public ActivityLogResponseDTO updateActivityLog(Integer activityLogId,
            ActivityLogRequestDTO requestDTO) throws WellnessTrackerException;

    // Ownership enforced in service: log must belong to JWT caller.
    @PreAuthorize("isAuthenticated()")
    public void deleteActivityLog(Integer activityLogId) throws WellnessTrackerException;

    // Returns the JWT caller's full activity history.
    @PreAuthorize("isAuthenticated()")
    public List<ActivityLogResponseDTO> getActivityHistory() throws WellnessTrackerException;

    // Manager or HR can view any user's activity summary.
    // userId here is the TARGET user — not the caller.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ActivitySummaryDTO getActivitySummary(Integer userId, LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException;

    // Manager or HR can view any user's activity trend.
    // userId here is the TARGET user — not the caller.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public List<ActivityTrendPointDTO> getActivityTrend(Integer userId, LocalDate fromDate,
            LocalDate toDate, ActivityType metricType) throws WellnessTrackerException;

    // Returns the JWT caller's filtered activity history.
    @PreAuthorize("isAuthenticated()")
    public List<ActivityLogResponseDTO> getFilteredActivityHistory(
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException;

    // Returns the JWT caller's filtered activity history as CSV bytes.
    @PreAuthorize("isAuthenticated()")
    public byte[] exportActivityHistoryCsv(
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException;
}