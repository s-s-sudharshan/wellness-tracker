package com.infy.dto;

import java.time.LocalDate;

import com.infy.enums.ActivityType;

import lombok.Data;

@Data
public class ActivityTrendPointDTO {
	private LocalDate activityDate;
    private ActivityType activityType;
    private double totalValue;
    private String unit;
}
