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
		
		if(logs.isEmpty()) {
			throw new WellnessTrackerException("Service.NO_ACTIVITY_FOUND");
		}
		
		List<ActivityLogResponseDTO> responseList = new ArrayList<>();
		for (ActivityLog log: logs) {
			ActivityLogResponseDTO dto = new ActivityLogResponseDTO();
			dto.setUserId(log.getUser().getUserId());
			dto.setUserName(log.getUser().getFirstName()+" "+log.getUser().getLastName());
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
		
        // Fix 4: Validate date range — same guard as mood log APIs
        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }
		
		Integer totalActivities = activityLogRepository
				.countByUser_UserIdAndActivityDateBetween(userId, fromDate, toDate);
		
		List<Object[]> rawSummary = activityLogRepository
				.findSummaryByUserAndDateRange(userId, fromDate, toDate);
		
		List<ActivityMetricDTO> metrics = new ArrayList<>();
		for (Object[] row: rawSummary) {
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
}
