package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface MoodLogService {

	public Integer saveMoodLog(MoodLogRequestDTO requestDTO) throws WellnessTrackerException;

	public List<MoodLogResponseDTO> getMoodTrend(Integer userId, LocalDate fromDate, LocalDate toDate)
			throws WellnessTrackerException;

	public List<MoodCorrelationDTO> getMoodCorrelation(Integer userId, LocalDate fromDate, LocalDate toDate)
			throws WellnessTrackerException;
}
