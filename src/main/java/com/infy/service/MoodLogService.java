package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface MoodLogService {

    // Any authenticated user can log their own mood.
    // Caller identity derived from JWT inside the implementation.
    @PreAuthorize("isAuthenticated()")
    public Integer saveMoodLog(MoodLogRequestDTO requestDTO) throws WellnessTrackerException;

    // Returns the JWT caller's own mood trend.
    @PreAuthorize("isAuthenticated()")
    public List<MoodLogResponseDTO> getMoodTrend(LocalDate fromDate, LocalDate toDate)
            throws WellnessTrackerException;

    // Returns the JWT caller's own mood vs activity correlation.
    @PreAuthorize("isAuthenticated()")
    public List<MoodCorrelationDTO> getMoodCorrelation(LocalDate fromDate, LocalDate toDate)
            throws WellnessTrackerException;
}
