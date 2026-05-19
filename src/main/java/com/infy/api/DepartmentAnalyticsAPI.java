package com.infy.api;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.DepartmentAnalyticsResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.DepartmentAnalyticsService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class DepartmentAnalyticsAPI {

    @Autowired
    private DepartmentAnalyticsService departmentAnalyticsService;

    // US 15 - Department comparison analytics (HR only).
    // requestingUserId param removed — caller identity derived from JWT inside service.
    // Role gate: @PreAuthorize("hasRole('HR')") on service interface.
    // metricType accepted as String (not ActivityType enum) to prevent Spring's
    // enum conversion from producing a 500 via the catch-all ExceptionControllerAdvice.
    // Invalid metricType values return 400 with Service.INVALID_METRIC_TYPE message.
    @GetMapping(value = "/analytics/departments")
    public ResponseEntity<DepartmentAnalyticsResponseDTO> getDepartmentAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String metricType)
            throws WellnessTrackerException {
        DepartmentAnalyticsResponseDTO response = departmentAnalyticsService
                .getDepartmentAnalytics(fromDate, toDate, metricType);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
