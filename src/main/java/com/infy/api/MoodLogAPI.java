package com.infy.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

@RestController
@RequestMapping("/wellness")
public class MoodLogAPI {

	@Autowired
	MoodLogService moodLogService;

	@Autowired
	Environment env;

	// US 12 - Log or update today's mood
	@PostMapping(value = "/mood-logs")
	public ResponseEntity<String> saveMoodLog(@Valid @RequestBody MoodLogRequestDTO requestDTO)
			throws WellnessTrackerException {
		Integer moodLogId = moodLogService.saveMoodLog(requestDTO);
		String successMessage = env.getProperty("API.MOOD_LOG_SUCCESS") + moodLogId;
		return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
	}

	// US 12 - Get mood trend for a date range (for trend chart)
	@GetMapping(value = "/mood-logs/users/{userId}")
	public ResponseEntity<List<MoodLogResponseDTO>> getMoodTrend(
			@PathVariable Integer userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
			throws WellnessTrackerException {
		List<MoodLogResponseDTO> trend = moodLogService.getMoodTrend(userId, fromDate, toDate);
		return new ResponseEntity<>(trend, HttpStatus.OK);
	}

	// US 12 - Get mood vs activity correlation for analytics page
	@GetMapping(value = "/mood-logs/users/{userId}/correlation")
	public ResponseEntity<List<MoodCorrelationDTO>> getMoodCorrelation(
			@PathVariable Integer userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate)
			throws WellnessTrackerException {
		List<MoodCorrelationDTO> correlation = moodLogService.getMoodCorrelation(userId, fromDate, toDate);
		return new ResponseEntity<>(correlation, HttpStatus.OK);
	}
}
