package com.infy.dto;

import java.util.List;

import lombok.Data;

@Data
public class DepartmentAnalyticsEntryDTO {

    private Integer departmentId;

    private String departmentName;

    // Distinct users with any activity in the date range, regardless of metricType filter.
    // Intentional: participation reflects overall department engagement, not just
    // activity in the filtered metric. Zero for departments with no activity.
    private Integer totalParticipants;

    // One entry per activity type present in the date range for this department.
    // Empty list for departments with no activity in the date range.
    private List<DepartmentMetricDTO> metrics;
}
