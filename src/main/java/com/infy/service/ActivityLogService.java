package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.exception.WellnessTrackerException;

public interface ActivityLogService {
	public Integer createActivityLog(ActivityLogRequestDTO requestDTO) throws WellnessTrackerException;
	
	public List<ActivityLogResponseDTO> getActivityHistory(Integer userId) throws WellnessTrackerException;
	
	public ActivitySummaryDTO getActivitySummary(Integer userId, LocalDate fromDate, LocalDate toDate) throws WellnessTrackerException;
}
