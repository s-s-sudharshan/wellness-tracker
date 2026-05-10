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
import org.springframework.web.bind.annotation.CrossOrigin;
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

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class ActivityLogAPI {

    @Autowired
    ActivityLogService activityService;

    @Autowired
    Environment env;

    // US 01 - Log a new wellness activity
    @PostMapping(value = "/activity-logs")
    public ResponseEntity<String> createActivityLog(
            @Valid @RequestBody ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer activityLogId = activityService.createActivityLog(requestDTO);
        String successMessage = env.getProperty("API.CREATE_ACTIVITY_SUCCESS") + activityLogId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 01 - Update an existing activity log (ownership guarded via userId in body)
    @PutMapping(value = "/activity-logs/{activityLogId}")
    public ResponseEntity<ActivityLogResponseDTO> updateActivityLog(
            @PathVariable Integer activityLogId,
            @Valid @RequestBody ActivityLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        ActivityLogResponseDTO updated = activityService.updateActivityLog(
                activityLogId, requestDTO);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    // US 01 - Delete an activity log (ownership enforced via userId query param)
    // userId passed as query param — avoids a wrapper request body for a DELETE
    @DeleteMapping(value = "/activity-logs/{activityLogId}")
    public ResponseEntity<String> deleteActivityLog(
            @PathVariable Integer activityLogId,
            @RequestParam Integer userId)
            throws WellnessTrackerException {
        activityService.deleteActivityLog(activityLogId, userId);
        String successMessage = env.getProperty("API.DELETE_ACTIVITY_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 01 - Get activity history for a user
    @GetMapping(value = "/activity-logs/users/{userId}")
    public ResponseEntity<List<ActivityLogResponseDTO>> getActivityHistory(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<ActivityLogResponseDTO> activityLogs = activityService.getActivityHistory(userId);
        return new ResponseEntity<>(activityLogs, HttpStatus.OK);
    }

    // US 02 - Get activity summary for a date range
    @GetMapping(value = "/activity-logs/users/{userId}/summary")
    public ResponseEntity<ActivitySummaryDTO> getActivitySummary(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
            throws WellnessTrackerException {
        ActivitySummaryDTO summary = activityService.getActivitySummary(userId, fromDate, toDate);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    // US 02 - Get day-wise activity trend for charts
    // metricType is optional — omit to get all types, provide to filter to one type
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
    
    // US 09 - Filtered and sorted activity history.
    // All query params are optional except userId (path variable).
    // sortBy: "date" (default) sorts by activityDate DESC;
    //         "amount" sorts by activityValue DESC (activityValue in entity/DTO).
    //         Any other value silently defaults to date — intentional.
    // Returns [] when no results match the filters — not an error.
    // Differs from GET /activity-logs/users/{userId} which throws when history is empty.
    @GetMapping(value = "/activity-logs/users/{userId}/search")
    public ResponseEntity<List<ActivityLogResponseDTO>> getFilteredActivityHistory(
            @PathVariable Integer userId,
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
                userId, fromDate, toDate, activityType, minValue, maxValue, sortBy);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }
 
    // US 09 - Export filtered activity history as a CSV file download.
    // Accepts the same filter and sort params as /search.
    // Returns the CSV as an attachment with filename activity_history_{userId}.csv.
    @GetMapping(value = "/activity-logs/users/{userId}/export")
    public ResponseEntity<byte[]> exportActivityHistory(
            @PathVariable Integer userId,
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
                userId, fromDate, toDate, activityType, minValue, maxValue, sortBy);
 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("activity_history_" + userId + ".csv")
                        .build());
 
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}
