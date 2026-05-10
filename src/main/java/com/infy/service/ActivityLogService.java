package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;

public interface ActivityLogService {

    public Integer createActivityLog(ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException;

    public List<ActivityLogResponseDTO> getActivityHistory(Integer userId)
            throws WellnessTrackerException;

    public ActivitySummaryDTO getActivitySummary(Integer userId, LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException;

    public List<ActivityTrendPointDTO> getActivityTrend(Integer userId, LocalDate fromDate,
            LocalDate toDate, ActivityType metricType) throws WellnessTrackerException;

    // US 01 - Update an existing activity log (ownership guarded)
    public ActivityLogResponseDTO updateActivityLog(Integer activityLogId,
            ActivityLogRequestDTO requestDTO) throws WellnessTrackerException;

    // US 01 - Delete an activity log (ownership guarded)
    public void deleteActivityLog(Integer activityLogId, Integer userId)
            throws WellnessTrackerException;
    
    // US 09 - Filtered and sorted activity history.
    // All filter params are optional (nullable). sortBy accepts "date" or "amount";
    // any other value defaults silently to date. Returns [] on empty — not an error.
    public List<ActivityLogResponseDTO> getFilteredActivityHistory(
            Integer userId,
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException;
 
    // US 09 - Export filtered activity history as a CSV byte array.
    // Accepts the same filter and sort params as getFilteredActivityHistory.
    // Returns raw CSV bytes — caller writes Content-Disposition header.
    public byte[] exportActivityHistoryCsv(
            Integer userId,
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException;
}
