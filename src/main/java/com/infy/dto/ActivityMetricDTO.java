package com.infy.dto;

import com.infy.enums.ActivityType;

import lombok.Data;

@Data
public class ActivityMetricDTO {
	private ActivityType activityType;
	private double totalValue;
	private String unit;
}
