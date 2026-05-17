package com.infy.dto;

import java.time.LocalDate;
import java.util.List;

import com.infy.enums.ActivityType;

import lombok.Data;

@Data
public class DepartmentAnalyticsResponseDTO {

    private LocalDate fromDate;

    private LocalDate toDate;

    // Null when no metricType filter was applied — all metrics are returned
    private ActivityType metricType;

    // One entry per department. Departments with no activity in the date range
    // are included with totalParticipants = 0 and an empty metrics list.
    private List<DepartmentAnalyticsEntryDTO> departments;
}
