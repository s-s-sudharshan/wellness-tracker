package com.infy.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.MoodLogService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class MoodLogAPI {

    @Autowired
    MoodLogService moodLogService;

    @Autowired
    Environment env;

    // US 12 - Log or update today's mood.
    // userId removed from request body — derived from JWT inside service.
    @PostMapping(value = "/mood-logs")
    public ResponseEntity<String> saveMoodLog(@Valid @RequestBody MoodLogRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer moodLogId = moodLogService.saveMoodLog(requestDTO);
        String successMessage = env.getProperty("API.MOOD_LOG_SUCCESS") + moodLogId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 12 - Get the JWT caller's mood trend for a date range.
    // Path changed from /mood-logs/users/{userId} to /mood-logs/mine.
    @GetMapping(value = "/mood-logs/mine")
    public ResponseEntity<List<MoodLogResponseDTO>> getMoodTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
            throws WellnessTrackerException {
        List<MoodLogResponseDTO> trend = moodLogService.getMoodTrend(fromDate, toDate);
        return new ResponseEntity<>(trend, HttpStatus.OK);
    }

    // US 12 - Get the JWT caller's mood vs activity correlation.
    // Path changed from /mood-logs/users/{userId}/correlation to /mood-logs/mine/correlation.
    @GetMapping(value = "/mood-logs/mine/correlation")
    public ResponseEntity<List<MoodCorrelationDTO>> getMoodCorrelation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
            throws WellnessTrackerException {
        List<MoodCorrelationDTO> correlation = moodLogService.getMoodCorrelation(fromDate, toDate);
        return new ResponseEntity<>(correlation, HttpStatus.OK);
    }
}
