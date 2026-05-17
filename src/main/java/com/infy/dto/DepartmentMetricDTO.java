package com.infy.dto;

import com.infy.enums.ActivityType;

import lombok.Data;

@Data
public class DepartmentMetricDTO {

    private ActivityType activityType;

    // Raw sum of all activity values for this department and type in the date range
    private Double totalValue;

    // Average per active user: totalValue / distinctUsers
    private Double avgValue;

    // True when this department has the highest avgValue for this metric
    // across all departments — set in service layer after all entries are built
    private Boolean isBest;
}
