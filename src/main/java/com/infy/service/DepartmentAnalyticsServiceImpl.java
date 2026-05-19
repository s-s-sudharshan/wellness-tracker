package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.DepartmentAnalyticsEntryDTO;
import com.infy.dto.DepartmentAnalyticsResponseDTO;
import com.infy.dto.DepartmentMetricDTO;
import com.infy.entity.Department;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.DepartmentRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
public class DepartmentAnalyticsServiceImpl implements DepartmentAnalyticsService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // requestingUserId removed from signature — caller identity derived from JWT.
    // @PreAuthorize("hasRole('HR')") on interface enforces role gate before entry.
    @Override
    @Transactional(readOnly = true)
    public DepartmentAnalyticsResponseDTO getDepartmentAnalytics(
            LocalDate fromDate,
            LocalDate toDate,
            String metricType) throws WellnessTrackerException {

        // Resolve caller from JWT — @PreAuthorize already confirmed HR role,
        // this call ensures the JWT maps to a valid user record.
        authenticatedUserResolver.resolveCurrentUser();

        // Validate date range
        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        // Parse metricType String → ActivityType.
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

        // Query 1 — per-department per-type aggregates (sum + distinct user count)
        // Returns [Integer deptId, ActivityType type, Double sum, Long distinctUsers]
        List<Object[]> aggregateRows = activityLogRepository
                .findDepartmentMetricAggregates(fromDate, toDate, activityTypeFilter);

        // Query 2 — distinct active participants per department (no metric filter)
        // totalParticipants always reflects overall department engagement regardless
        // of the metricType filter — intentional design decision.
        // Returns [Integer deptId, Long count]
        List<Object[]> participantRows = activityLogRepository
                .findDepartmentParticipantCounts(fromDate, toDate);

        // Build lookup maps from query results
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

        Map<Integer, Integer> participantMap = new HashMap<>();
        for (Object[] row : participantRows) {
            Integer deptId = ((Number) row[0]).intValue();
            Integer count  = ((Number) row[1]).intValue();
            participantMap.put(deptId, count);
        }

        // Load all departments — ensures empty departments appear in the result
        List<Department> allDepartments = new ArrayList<>();
        departmentRepository.findAll().forEach(allDepartments::add);

        // Build per-department entries
        List<DepartmentAnalyticsEntryDTO> entries = new ArrayList<>();
        for (Department dept : allDepartments) {
            Integer deptId = dept.getDepartmentId();

            DepartmentAnalyticsEntryDTO entry = new DepartmentAnalyticsEntryDTO();
            entry.setDepartmentId(deptId);
            entry.setDepartmentName(dept.getDepartmentName());
            entry.setTotalParticipants(participantMap.getOrDefault(deptId, 0));

            List<DepartmentMetricDTO> metrics = new ArrayList<>();
            Map<ActivityType, double[]> typeMap =
                    aggregateMap.getOrDefault(deptId, new HashMap<>());

            for (Map.Entry<ActivityType, double[]> e : typeMap.entrySet()) {
                double sum           = e.getValue()[0];
                double distinctUsers = e.getValue()[1];
                double avgValue      = distinctUsers > 0 ? sum / distinctUsers : 0.0;

                DepartmentMetricDTO metricDTO = new DepartmentMetricDTO();
                metricDTO.setActivityType(e.getKey());
                metricDTO.setTotalValue(sum);
                metricDTO.setAvgValue(avgValue);
                metricDTO.setIsBest(false); // set below
                metrics.add(metricDTO);
            }

            entry.setMetrics(metrics);
            entries.add(entry);
        }

        // Determine isBest per metric type across all departments.
        // Ties: all tied entries are flagged isBest = true.
        // Guard: isBest is only set when best > 0 — zero avgValue is not meaningful.
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

        DepartmentAnalyticsResponseDTO response = new DepartmentAnalyticsResponseDTO();
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setMetricType(activityTypeFilter);
        response.setDepartments(entries);
        return response;
    }
}
