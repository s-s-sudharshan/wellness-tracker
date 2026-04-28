package com.infy.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wellness")
public class ActivityLogAPI {
	
	@Autowired
	ActivityLogService activityService;
	
	@Autowired
	Environment env;
	
	// US 01 - Log a new wellness activity
	@PostMapping(value = "/activity-logs")
	public ResponseEntity<String> createActivityLog(@Valid @RequestBody ActivityLogRequestDTO requestDTO)
			throws WellnessTrackerException {
		Integer activityLogId = activityService.createActivityLog(requestDTO);
		String successMessage = env.getProperty("API.CREATE_ACTIVITY_SUCCESS")+activityLogId;
		return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
	}
	
	// US 01 - Get activity history for a user
	@GetMapping(value = "/activity-logs/users/{userId}")
	public ResponseEntity<List<ActivityLogResponseDTO>> getActivityHistory(@PathVariable Integer userId) 
			throws WellnessTrackerException {
		List<ActivityLogResponseDTO> activityLogs = activityService.getActivityHistory(userId);
		return new ResponseEntity<>(activityLogs, HttpStatus.OK);
	}
	
	// US 02 - Get activity summary for a date range
	@GetMapping(value = "/activity-logs/users/{userId}/summary")
	public ResponseEntity<ActivitySummaryDTO> getActivitySummary (
			@PathVariable Integer userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) 
			throws WellnessTrackerException {
		ActivitySummaryDTO summary = activityService.getActivitySummary(userId, fromDate, toDate);
		return new ResponseEntity<>(summary, HttpStatus.OK);
	}
	
	// US 02 - Get day-wise activity trend for charts
	// metricType is optional — omit to get all types, provide to filter to one type
	@GetMapping(value = "/activity-logs/users/{userId}/trend")
	public ResponseEntity<List<ActivityTrendPointDTO>> getActivityTrend(
	        @PathVariable Integer userId,
	        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
	        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
	        @RequestParam(required = false) ActivityType metricType)
	        throws WellnessTrackerException {
	    List<ActivityTrendPointDTO> trend = activityService.getActivityTrend(
	            userId, fromDate, toDate, metricType);
	    return new ResponseEntity<>(trend, HttpStatus.OK);
	}
}
