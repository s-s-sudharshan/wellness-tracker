package com.infy.service;

import java.time.LocalDate;

import com.infy.dto.DepartmentAnalyticsResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface DepartmentAnalyticsService {

    // US 15 - Department comparison analytics for HR.
    // metricType is a raw String — validated inside the service to avoid Spring's
    // enum conversion producing a 500 via the catch-all ExceptionControllerAdvice.
    // Null or blank metricType returns all metrics.
    // requestingUserId is verified against the JWT principal before use.
    public DepartmentAnalyticsResponseDTO getDepartmentAnalytics(
            Integer requestingUserId,
            LocalDate fromDate,
            LocalDate toDate,
            String metricType) throws WellnessTrackerException;
}
