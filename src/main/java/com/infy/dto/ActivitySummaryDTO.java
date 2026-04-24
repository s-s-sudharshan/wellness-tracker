package com.infy.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ActivitySummaryDTO {
	private Integer userId;
	private LocalDate fromDate;
	private LocalDate toDate;
	private Integer totalActivities;
	private List<ActivityMetricDTO> metrics;
}
