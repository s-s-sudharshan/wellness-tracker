package com.infy.dto;

import java.time.LocalDate;

import com.infy.enums.ActivityType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityLogRequestDTO {
	
	@NotNull(message = "{activity.userid.absent}")
	private Integer userId;
 
	@NotNull(message = "{activity.type.absent}")
	private ActivityType activityType;
 
	@NotNull(message = "{activity.date.absent}")
	@PastOrPresent(message = "{activity.date.invalid}")
	private LocalDate activityDate;
 
	@Positive(message = "{activity.value.invalid}")
	private double activityValue;
 
	@NotBlank(message = "{activity.unit.absent}")
	private String unit;
 
	@Size(max = 500, message = "{activity.notes.toolong}")
	private String notes;

}
