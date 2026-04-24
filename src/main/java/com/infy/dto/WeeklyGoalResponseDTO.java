package com.infy.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class WeeklyGoalResponseDTO {

	private Integer weeklyGoalId;
	private Integer userId;
	private LocalDate weekStartDate;
	private LocalDate weekEndDate;

	// Goals
	private Double stepsGoal;
	private Double workoutGoal;
	private Double waterGoal;
	private Double meditationGoal;
	private Double sleepGoalHours;

	// Actuals — calculated live from activity_logs
	private Double stepsActual;
	private Double workoutActual;
	private Double waterActual;
	private Double meditationActual;
	private Double sleepActual;

	// Progress percentages for progress bars (capped at 100)
	private Integer stepsProgressPct;
	private Integer workoutProgressPct;
	private Integer waterProgressPct;
	private Integer meditationProgressPct;
	private Integer sleepProgressPct;
}
