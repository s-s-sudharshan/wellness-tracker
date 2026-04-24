package com.infy.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MoodLogRequestDTO {

	@NotNull(message = "{moodlog.userid.absent}")
	private Integer userId;
 
	@NotNull(message = "{moodlog.logdate.absent}")
	@PastOrPresent(message = "{moodlog.logdate.invalid}")
	private LocalDate logDate;
 
	@NotNull(message = "{moodlog.moodscore.absent}")
	@Min(value = 1, message = "{moodlog.moodscore.invalid}")
	@Max(value = 5, message = "{moodlog.moodscore.invalid}")
	private Integer moodScore;
 
	@Size(max = 500, message = "{moodlog.note.toolong}")
	private String note;
}
