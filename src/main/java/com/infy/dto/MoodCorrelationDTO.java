package com.infy.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MoodCorrelationDTO {

	private LocalDate date;
	private Integer moodScore;
	private String moodLabel;
	private boolean hadActivity;
	private Integer activityCount;
}
