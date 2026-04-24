package com.infy.service;

import java.time.LocalDate;

import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface WeeklyGoalService {

	public Integer saveWeeklyGoal(WeeklyGoalRequestDTO requestDTO) throws WellnessTrackerException;

	public WeeklyGoalResponseDTO getWeeklyGoal(Integer userId, LocalDate weekStartDate)
			throws WellnessTrackerException;
}
