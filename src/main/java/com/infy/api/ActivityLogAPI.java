package com.infy.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wellness")
public class ActivityLogAPI {

    @Autowired
    ActivityLogService activityService;

    @Autowired
    Environment env;

    // US 01 - Log a new wellness activity.
    // userId is no longer in the request body — derived from JWT inside the service.
    @PostMapping(value = "/activity-logs")
    public ResponseEntity<String> createActivityLog(
            @Valid @RequestBody ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer activityLogId = activityService.createActivityLog(requestDTO);
        String successMessage = env.getProperty("API.CREATE_ACTIVITY_SUCCESS") + activityLogId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 01 - Update an existing activity log.
    // Ownership enforced in service: log must belong to JWT caller.
    // userId removed from request body — derived from JWT.
    @PutMapping(value = "/activity-logs/{activityLogId}")
    public ResponseEntity<ActivityLogResponseDTO> updateActivityLog(
            @PathVariable Integer activityLogId,
            @Valid @RequestBody ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        ActivityLogResponseDTO updated = activityService.updateActivityLog(
                activityLogId, requestDTO);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    // US 01 - Delete an activity log.
    // userId query param removed — caller identity derived from JWT inside service.
    @DeleteMapping(value = "/activity-logs/{activityLogId}")
    public ResponseEntity<String> deleteActivityLog(
            @PathVariable Integer activityLogId)
            throws WellnessTrackerException {
        activityService.deleteActivityLog(activityLogId);
        String successMessage = env.getProperty("API.DELETE_ACTIVITY_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 01 - Get the JWT caller's own activity history.
    // Path changed from /users/{userId} to /mine.
    @GetMapping(value = "/activity-logs/mine")
    public ResponseEntity<List<ActivityLogResponseDTO>> getActivityHistory()
            throws WellnessTrackerException {
        List<ActivityLogResponseDTO> activityLogs = activityService.getActivityHistory();
        return new ResponseEntity<>(activityLogs, HttpStatus.OK);
    }

    // US 02 - Get activity summary for a target user (manager/HR only).
    // userId is the TARGET user being viewed — not the caller.
    // Role gate: @PreAuthorize("hasRole('MANAGER') or hasRole('HR')") on service.
    @GetMapping(value = "/activity-logs/users/{userId}/summary")
    public ResponseEntity<ActivitySummaryDTO> getActivitySummary(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
            throws WellnessTrackerException {
        ActivitySummaryDTO summary = activityService.getActivitySummary(userId, fromDate, toDate);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    // US 02 - Get day-wise activity trend for a target user (manager/HR only).
    // userId is the TARGET user being viewed — not the caller.
    // metricType is optional — omit to get all types, provide to filter to one type.
    @GetMapping(value = "/activity-logs/users/{userId}/trend")
    public ResponseEntity<List<ActivityTrendPointDTO>> getActivityTrend(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ActivityType metricType)
            throws WellnessTrackerException {
        List<ActivityTrendPointDTO> trend = activityService.getActivityTrend(
                userId, fromDate, toDate, metricType);
        return new ResponseEntity<>(trend, HttpStatus.OK);
    }

    // US 09 - Filtered and sorted activity history for the JWT caller.
    // Path changed from /users/{userId}/search to /mine/search.
    // All query params are optional except the path (JWT identity used).
    // sortBy: "date" (default) or "amount".
    @GetMapping(value = "/activity-logs/mine/search")
    public ResponseEntity<List<ActivityLogResponseDTO>> getFilteredActivityHistory(
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) Double minValue,
            @RequestParam(required = false) Double maxValue,
            @RequestParam(required = false, defaultValue = "date") String sortBy)
            throws WellnessTrackerException {
        List<ActivityLogResponseDTO> results = activityService.getFilteredActivityHistory(
                fromDate, toDate, activityType, minValue, maxValue, sortBy);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // US 09 - Export filtered activity history as CSV for the JWT caller.
    // Path changed from /users/{userId}/export to /mine/export.
    @GetMapping(value = "/activity-logs/mine/export")
    public ResponseEntity<byte[]> exportActivityHistory(
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) Double minValue,
            @RequestParam(required = false) Double maxValue,
            @RequestParam(required = false, defaultValue = "date") String sortBy)
            throws WellnessTrackerException {

        byte[] csv = activityService.exportActivityHistoryCsv(
                fromDate, toDate, activityType, minValue, maxValue, sortBy);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("activity_history.csv")
                        .build());

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}