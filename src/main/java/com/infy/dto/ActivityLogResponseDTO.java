package com.infy.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.infy.enums.ActivityType;

import lombok.Data;

@Data
public class ActivityLogResponseDTO {
	
	private Integer activityLogId;
	private Integer userId;
	private String userName;
	private ActivityType activityType;
	private LocalDate activityDate;
	private double activityValue;
	private String unit;
	private String notes;
	private LocalDateTime createdAt;
}
