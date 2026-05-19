package com.infy.service;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.DepartmentAnalyticsResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface DepartmentAnalyticsService {

    // US 15 - Department comparison analytics (HR only).
    // requestingUserId removed — caller identity derived from JWT inside implementation.
    // metricType is a raw String — validated inside the service to avoid Spring's
    // enum conversion producing a 500 via the catch-all ExceptionControllerAdvice.
    // Null or blank metricType returns all metrics.
    @PreAuthorize("hasRole('HR')")
    public DepartmentAnalyticsResponseDTO getDepartmentAnalytics(
            LocalDate fromDate,
            LocalDate toDate,
            String metricType) throws WellnessTrackerException;
}
