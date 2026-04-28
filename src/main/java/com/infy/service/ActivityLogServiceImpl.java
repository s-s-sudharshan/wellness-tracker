package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivityMetricDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.entity.ActivityLog;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

	@Autowired
	private ActivityLogRepository activityLogRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Integer createActivityLog(ActivityLogRequestDTO requestDTO) throws WellnessTrackerException {
		Optional<User> optional = userRepository.findById(requestDTO.getUserId());
		User user = optional.orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

		ActivityLog activityLog = new ActivityLog();
		activityLog.setUser(user);
		activityLog.setActivityType(requestDTO.getActivityType());
		activityLog.setActivityDate(requestDTO.getActivityDate());
		activityLog.setActivityValue(requestDTO.getActivityValue());
		activityLog.setUnit(requestDTO.getUnit());
		activityLog.setNotes(requestDTO.getNotes());
		return activityLogRepository.save(activityLog).getActivityLogId();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ActivityLogResponseDTO> getActivityHistory(Integer userId) throws WellnessTrackerException {
		if (!userRepository.existsById(userId)) {
			throw new WellnessTrackerException("Service.USER_NOT_FOUND");
		}

		List<ActivityLog> logs = activityLogRepository
				.findByUser_UserIdOrderByActivityDateDescCreatedAtDesc(userId);

		if (logs.isEmpty()) {
			throw new WellnessTrackerException("Service.NO_ACTIVITY_FOUND");
		}

		List<ActivityLogResponseDTO> responseList = new ArrayList<>();
		for (ActivityLog log : logs) {
			ActivityLogResponseDTO dto = new ActivityLogResponseDTO();
			dto.setUserId(log.getUser().getUserId());
			dto.setUserName(log.getUser().getFirstName() + " " + log.getUser().getLastName());
			dto.setActivityLogId(log.getActivityLogId());
			dto.setActivityType(log.getActivityType());
			dto.setActivityDate(log.getActivityDate());
			dto.setActivityValue(log.getActivityValue());
			dto.setUnit(log.getUnit());
			dto.setNotes(log.getNotes());
			dto.setCreatedAt(log.getCreatedAt());
			responseList.add(dto);
		}

		return responseList;
	}

	@Override
	@Transactional(readOnly = true)
	public ActivitySummaryDTO getActivitySummary(Integer userId, LocalDate fromDate, LocalDate toDate)
			throws WellnessTrackerException {
		if (!userRepository.existsById(userId)) {
			throw new WellnessTrackerException("Service.USER_NOT_FOUND");
		}

		if (fromDate.isAfter(toDate)) {
			throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
		}

		Integer totalActivities = activityLogRepository
				.countByUser_UserIdAndActivityDateBetween(userId, fromDate, toDate);

		List<Object[]> rawSummary = activityLogRepository
				.findSummaryByUserAndDateRange(userId, fromDate, toDate);

		List<ActivityMetricDTO> metrics = new ArrayList<>();
		for (Object[] row : rawSummary) {
			ActivityMetricDTO metric = new ActivityMetricDTO();
			metric.setActivityType((ActivityType) row[0]);
			metric.setTotalValue(((Number) row[1]).doubleValue());
			metric.setUnit((String) row[2]);
			metrics.add(metric);
		}

		ActivitySummaryDTO summaryDTO = new ActivitySummaryDTO();
		summaryDTO.setUserId(userId);
		summaryDTO.setFromDate(fromDate);
		summaryDTO.setToDate(toDate);
		summaryDTO.setTotalActivities(totalActivities);
		summaryDTO.setMetrics(metrics);
		return summaryDTO;
	}

	// US 02 — Day-wise activity trend for charting.
	//
	// P2 Fix: Returns an empty list [] when no activity rows exist for the date range,
	// instead of throwing NO_ACTIVITY_FOUND. Rationale:
	//   - The date range and user are both valid — there is simply no data yet.
	//   - Throwing a 400 error forces the frontend to add special-case error handling
	//     for a routine "no data" state, which is not an error from the client's perspective.
	//   - An empty list is a well-formed response the charting library can render as a
	//     blank/zero chart without any special handling.
	//   - This is consistent with how most time-series APIs behave (e.g. analytics APIs).
	//
	// If metricType is null, all activity types for the date range are returned.
	// If metricType is provided, only rows matching that type are returned.
	@Override
	@Transactional(readOnly = true)
	public List<ActivityTrendPointDTO> getActivityTrend(
			Integer userId, LocalDate fromDate, LocalDate toDate, ActivityType metricType)
			throws WellnessTrackerException {
		if (!userRepository.existsById(userId)) {
			throw new WellnessTrackerException("Service.USER_NOT_FOUND");
		}

		if (fromDate.isAfter(toDate)) {
			throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
		}

		List<Object[]> raw = activityLogRepository.findTrendByUserAndDateRange(
				userId, fromDate, toDate);

		// Return empty list — not an error — when no activity exists in the range.
		// The frontend chart renders an empty state; no special error handling needed.
		List<ActivityTrendPointDTO> result = new ArrayList<>();
		for (Object[] row : raw) {
			ActivityType type = (ActivityType) row[1];
			// Filter by metric type in Java when a specific type is requested.
			// Avoids a separate parameterised query and keeps the repository query reusable.
			if (metricType != null && !type.equals(metricType)) {
				continue;
			}
			ActivityTrendPointDTO point = new ActivityTrendPointDTO();
			point.setActivityDate((LocalDate) row[0]);
			point.setActivityType(type);
			point.setTotalValue(((Number) row[2]).doubleValue());
			point.setUnit((String) row[3]);
			result.add(point);
		}

		return result;
	}
}
