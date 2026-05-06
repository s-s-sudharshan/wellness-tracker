package com.infy.dto;

import java.time.LocalDate;

import com.infy.enums.ActivityType;
import com.infy.enums.Difficulty;
import com.infy.enums.VisibilityType;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChallengeRequestDTO {

    @NotNull(message = "{challenge.createdby.absent}")
    private Integer createdBy;

    @NotBlank(message = "{challenge.title.absent}")
    @Size(max = 150, message = "{challenge.title.toolong}")
    private String title;

    @NotBlank(message = "{challenge.description.absent}")
    private String description;

    @NotNull(message = "{challenge.metrictype.absent}")
    private ActivityType metricType;

    @NotNull(message = "{challenge.goalvalue.absent}")
    @Positive(message = "{challenge.goalvalue.invalid}")
    private Double goalValue;

    @NotNull(message = "{challenge.difficulty.absent}")
    private Difficulty difficulty;

    @NotNull(message = "{challenge.startdate.absent}")
    @Future(message = "{challenge.startdate.invalid}")
    private LocalDate startDate;

    @NotNull(message = "{challenge.enddate.absent}")
    private LocalDate endDate;

    @NotNull(message = "{challenge.visibilitytype.absent}")
    private VisibilityType visibilityType;

    // Required only when visibilityType is DEPARTMENT
    private Integer departmentId;

    private Boolean isFeatured;
    
    private Integer rewardBadgeId;
}
