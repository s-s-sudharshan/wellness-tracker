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
}
