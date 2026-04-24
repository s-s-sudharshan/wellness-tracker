package com.infy.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MoodLogResponseDTO {

	private Integer moodLogId;
	private Integer userId;
	private LocalDate logDate;
	private Integer moodScore;
	private String moodLabel;
	private String note;
	private LocalDateTime createdAt;
}
