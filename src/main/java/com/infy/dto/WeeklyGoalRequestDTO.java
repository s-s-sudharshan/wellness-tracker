package com.infy.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class WeeklyGoalRequestDTO {

	@NotNull(message = "{weeklygoal.userid.absent}")
	private Integer userId;
 
	@NotNull(message = "{weeklygoal.weekstartdate.absent}")
	private LocalDate weekStartDate;
 
	@Positive(message = "{weeklygoal.stepsgoal.invalid}")
	private Double stepsGoal;
 
	@Positive(message = "{weeklygoal.workoutgoal.invalid}")
	private Double workoutGoal;
 
	@Positive(message = "{weeklygoal.watergoal.invalid}")
	private Double waterGoal;
 
	@Positive(message = "{weeklygoal.meditationgoal.invalid}")
	private Double meditationGoal;
 
	@Positive(message = "{weeklygoal.sleepgoalhours.invalid}")
	private Double sleepGoalHours;
}
