package com.infy.api;

import java.time.LocalDate;

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

import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.WeeklyGoalService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class WeeklyGoalAPI {

    @Autowired
    WeeklyGoalService weeklyGoalService;

    @Autowired
    Environment env;

    // US 11 - Set or update weekly goals for the JWT caller.
    // userId removed from request body — derived from JWT inside service.
    @PostMapping(value = "/weekly-goals")
    public ResponseEntity<String> saveWeeklyGoal(
            @Valid @RequestBody WeeklyGoalRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer weeklyGoalId = weeklyGoalService.saveWeeklyGoal(requestDTO);
        String successMessage = env.getProperty("API.WEEKLY_GOAL_SUCCESS") + weeklyGoalId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 11 - Get the JWT caller's weekly goals with actuals and progress.
    // Path changed from /weekly-goals/users/{userId} to /weekly-goals/mine.
    @GetMapping(value = "/weekly-goals/mine")
    public ResponseEntity<WeeklyGoalResponseDTO> getWeeklyGoal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate)
            throws WellnessTrackerException {
        WeeklyGoalResponseDTO response = weeklyGoalService.getWeeklyGoal(weekStartDate);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
