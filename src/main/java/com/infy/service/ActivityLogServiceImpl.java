package com.infy.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Override
    public Integer createActivityLog(ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        // Identity derived from JWT — never from request body
        User caller = authenticatedUserResolver.resolveCurrentUser();

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(caller);
        activityLog.setActivityType(requestDTO.getActivityType());
        activityLog.setActivityDate(requestDTO.getActivityDate());
        activityLog.setActivityValue(requestDTO.getActivityValue());
        activityLog.setUnit(requestDTO.getUnit());
        activityLog.setNotes(requestDTO.getNotes());
        return activityLogRepository.save(activityLog).getActivityLogId();
    }

    // Ownership enforced at DB level — findByActivityLogIdAndUser_UserId returns
    // empty if the log does not belong to the JWT caller.
    @Override
    public ActivityLogResponseDTO updateActivityLog(Integer activityLogId,
            ActivityLogRequestDTO requestDTO) throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        Optional<ActivityLog> optional = activityLogRepository
                .findByActivityLogIdAndUser_UserId(activityLogId, callerId);
        ActivityLog activityLog = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.ACTIVITY_LOG_NOT_FOUND"));

        activityLog.setActivityType(requestDTO.getActivityType());
        activityLog.setActivityDate(requestDTO.getActivityDate());
        activityLog.setActivityValue(requestDTO.getActivityValue());
        activityLog.setUnit(requestDTO.getUnit());
        activityLog.setNotes(requestDTO.getNotes());

        return mapToDTO(activityLogRepository.save(activityLog));
    }

    // Ownership enforced at DB level — same pattern as update.
    @Override
    public void deleteActivityLog(Integer activityLogId) throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        Optional<ActivityLog> optional = activityLogRepository
                .findByActivityLogIdAndUser_UserId(activityLogId, callerId);
        optional.orElseThrow(
                () -> new WellnessTrackerException("Service.ACTIVITY_LOG_NOT_FOUND"));

        activityLogRepository.deleteById(activityLogId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogResponseDTO> getActivityHistory() throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        List<ActivityLog> logs = activityLogRepository
                .findByUser_UserIdOrderByActivityDateDescCreatedAtDesc(callerId);

        if (logs.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_ACTIVITY_FOUND");
        }

        List<ActivityLogResponseDTO> responseList = new ArrayList<>();
        for (ActivityLog log : logs) {
            responseList.add(mapToDTO(log));
        }
        return responseList;
    }

    // Manager/HR viewing a target user's summary — userId is the TARGET, not caller.
    // @PreAuthorize("hasRole('MANAGER') or hasRole('HR')") enforces role gate.
    @Override
    @Transactional(readOnly = true)
    public ActivitySummaryDTO getActivitySummary(Integer userId, LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException {
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

    // Manager/HR viewing a target user's trend — userId is the TARGET, not caller.
    @Override
    @Transactional(readOnly = true)
    public List<ActivityTrendPointDTO> getActivityTrend(Integer userId, LocalDate fromDate,
            LocalDate toDate, ActivityType metricType) throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        List<Object[]> raw = activityLogRepository
                .findTrendByUserAndDateRange(userId, fromDate, toDate);

        List<ActivityTrendPointDTO> result = new ArrayList<>();
        for (Object[] row : raw) {
            ActivityType type = (ActivityType) row[1];
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

    // JWT caller's own filtered history — userId derived internally.
    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogResponseDTO> getFilteredActivityHistory(
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException {

        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new WellnessTrackerException("Service.INVALID_VALUE_RANGE");
        }

        List<ActivityLog> logs = "amount".equalsIgnoreCase(sortBy)
                ? activityLogRepository.findFilteredSortByAmount(
                        callerId, fromDate, toDate, activityType, minValue, maxValue)
                : activityLogRepository.findFilteredSortByDate(
                        callerId, fromDate, toDate, activityType, minValue, maxValue);

        List<ActivityLogResponseDTO> responseList = new ArrayList<>();
        for (ActivityLog log : logs) {
            responseList.add(mapToDTO(log));
        }
        return responseList;
    }

    // JWT caller's own export — userId derived internally.
    @Override
    @Transactional(readOnly = true)
    public byte[] exportActivityHistoryCsv(
            LocalDate fromDate,
            LocalDate toDate,
            ActivityType activityType,
            Double minValue,
            Double maxValue,
            String sortBy) throws WellnessTrackerException {

        List<ActivityLogResponseDTO> data = getFilteredActivityHistory(
                fromDate, toDate, activityType, minValue, maxValue, sortBy);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.println("Activity Log ID,User ID,User Name,Activity Type," +
                           "Activity Date,Activity Value,Unit,Notes,Created At");

            for (ActivityLogResponseDTO dto : data) {
                writer.printf(Locale.US, "%d,%d,\"%s\",%s,%s,%.2f,\"%s\",\"%s\",%s%n",
                        dto.getActivityLogId(),
                        dto.getUserId(),
                        escapeCsv(dto.getUserName()),
                        dto.getActivityType(),
                        dto.getActivityDate(),
                        dto.getActivityValue(),
                        escapeCsv(dto.getUnit()),
                        dto.getNotes() != null ? escapeCsv(dto.getNotes()) : "",
                        dto.getCreatedAt());
            }
        }

        return out.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private ActivityLogResponseDTO mapToDTO(ActivityLog log) {
        ActivityLogResponseDTO dto = new ActivityLogResponseDTO();
        dto.setActivityLogId(log.getActivityLogId());
        dto.setUserId(log.getUser().getUserId());
        dto.setUserName(log.getUser().getFirstName() + " " + log.getUser().getLastName());
        dto.setActivityType(log.getActivityType());
        dto.setActivityDate(log.getActivityDate());
        dto.setActivityValue(log.getActivityValue());
        dto.setUnit(log.getUnit());
        dto.setNotes(log.getNotes());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
