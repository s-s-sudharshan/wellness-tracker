package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.DepartmentAnalyticsEntryDTO;
import com.infy.dto.DepartmentAnalyticsResponseDTO;
import com.infy.dto.DepartmentMetricDTO;
import com.infy.entity.Department;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.Role;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.DepartmentRepository;
import com.infy.repository.UserRepository;

@Service
public class DepartmentAnalyticsServiceImpl implements DepartmentAnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public DepartmentAnalyticsResponseDTO getDepartmentAnalytics(
            Integer requestingUserId,
            LocalDate fromDate,
            LocalDate toDate,
            String metricType) throws WellnessTrackerException {

        // 1. Verify JWT principal matches the supplied userId and user is HR
        resolveAuthenticatedHrUser(requestingUserId);

        // 2. Validate date range
        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        // 3. Parse metricType String → ActivityType
        // Accepted as String in the controller to prevent Spring's enum conversion
        // from producing a 500 via the catch-all ExceptionControllerAdvice handler.
        ActivityType activityTypeFilter = null;
        if (metricType != null && !metricType.isBlank()) {
            try {
                activityTypeFilter = ActivityType.valueOf(metricType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new WellnessTrackerException("Service.INVALID_METRIC_TYPE");
            }
        }

        // 4. Query 1 — per-department per-type aggregates (sum + distinct user count)
        // Returns [Integer deptId, ActivityType type, Double sum, Long distinctUsers]
        List<Object[]> aggregateRows = activityLogRepository
                .findDepartmentMetricAggregates(fromDate, toDate, activityTypeFilter);

        // 5. Query 2 — distinct active participants per department (no metric filter)
        // totalParticipants always reflects overall department engagement, regardless
        // of metricType filter — intentional design decision documented in the DTO.
        // Returns [Integer deptId, Long count]
        List<Object[]> participantRows = activityLogRepository
                .findDepartmentParticipantCounts(fromDate, toDate);

        // 6. Build lookup maps from query results
        // deptId → type → [sum, distinctUsers]
        Map<Integer, Map<ActivityType, double[]>> aggregateMap = new HashMap<>();
        for (Object[] row : aggregateRows) {
            Integer deptId        = ((Number) row[0]).intValue();
            ActivityType type     = (ActivityType) row[1];
            double sum            = ((Number) row[2]).doubleValue();
            double distinctUsers  = ((Number) row[3]).doubleValue();

            aggregateMap
                    .computeIfAbsent(deptId, k -> new HashMap<>())
                    .put(type, new double[]{sum, distinctUsers});
        }

        // deptId → totalParticipants
        Map<Integer, Integer> participantMap = new HashMap<>();
        for (Object[] row : participantRows) {
            Integer deptId = ((Number) row[0]).intValue();
            Integer count  = ((Number) row[1]).intValue();
            participantMap.put(deptId, count);
        }

        // 7. Load all departments — ensures empty departments appear in the result
        List<Department> allDepartments = new ArrayList<>();
        departmentRepository.findAll().forEach(allDepartments::add);

        // 8. Build per-department entries
        List<DepartmentAnalyticsEntryDTO> entries = new ArrayList<>();
        for (Department dept : allDepartments) {
            Integer deptId = dept.getDepartmentId();

            DepartmentAnalyticsEntryDTO entry = new DepartmentAnalyticsEntryDTO();
            entry.setDepartmentId(deptId);
            entry.setDepartmentName(dept.getDepartmentName());
            entry.setTotalParticipants(participantMap.getOrDefault(deptId, 0));

            List<DepartmentMetricDTO> metrics = new ArrayList<>();
            Map<ActivityType, double[]> typeMap = aggregateMap.getOrDefault(deptId, new HashMap<>());

            for (Map.Entry<ActivityType, double[]> e : typeMap.entrySet()) {
                double sum           = e.getValue()[0];
                double distinctUsers = e.getValue()[1];
                double avgValue      = distinctUsers > 0 ? sum / distinctUsers : 0.0;

                DepartmentMetricDTO metricDTO = new DepartmentMetricDTO();
                metricDTO.setActivityType(e.getKey());
                metricDTO.setTotalValue(sum);
                metricDTO.setAvgValue(avgValue);
                metricDTO.setIsBest(false); // set in step 9
                metrics.add(metricDTO);
            }

            entry.setMetrics(metrics);
            entries.add(entry);
        }

        // 9. Determine isBest per metric type across all departments
        // For each ActivityType, find the department entry with the highest avgValue
        // and flag it. Ties: all tied entries are flagged isBest = true.
        Map<ActivityType, Double> bestAvgByType = new HashMap<>();
        for (DepartmentAnalyticsEntryDTO entry : entries) {
            for (DepartmentMetricDTO m : entry.getMetrics()) {
                bestAvgByType.merge(m.getActivityType(), m.getAvgValue(), Math::max);
            }
        }
        for (DepartmentAnalyticsEntryDTO entry : entries) {
            for (DepartmentMetricDTO m : entry.getMetrics()) {
                Double best = bestAvgByType.get(m.getActivityType());
                if (best != null && best > 0 && Double.compare(m.getAvgValue(), best) == 0) {
                    m.setIsBest(true);
                }
            }
        }

        // 10. Build and return response
        DepartmentAnalyticsResponseDTO response = new DepartmentAnalyticsResponseDTO();
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setMetricType(activityTypeFilter);
        response.setDepartments(entries);
        return response;
    }

    // Verifies the supplied userId matches the JWT principal AND the user is HR.
    // Same pattern as BulkUploadServiceImpl.resolveAuthenticatedHrUser().
    // Throws Service.UNAUTHORIZED if the userId does not match the JWT subject.
    // Throws Service.NOT_HR if the user exists but is not HR.
    private User resolveAuthenticatedHrUser(Integer requestingUserId)
            throws WellnessTrackerException {

        String authenticatedEmail = getAuthenticatedEmail();

        Optional<User> optional = userRepository.findById(requestingUserId);
        User user = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }

        if (!Role.HR.equals(user.getRole())) {
            throw new WellnessTrackerException("Service.NOT_HR");
        }

        return user;
    }

    // Reads the authenticated email from the JWT subject via SecurityContextHolder.
    // Throws Service.UNAUTHORIZED if no authentication is present.
    private String getAuthenticatedEmail() throws WellnessTrackerException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }
        return authentication.getName();
    }
}
