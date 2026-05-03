package com.infy.dto;

import com.infy.enums.CriteriaType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BadgeRequestDTO {

    // The manager/HR user creating or editing this badge
    @NotNull(message = "{badge.createdby.absent}")
    private Integer requestingUserId;

    @NotBlank(message = "{badge.title.absent}")
    @Size(max = 100, message = "{badge.title.toolong}")
    private String title;

    @NotBlank(message = "{badge.description.absent}")
    @Size(max = 500, message = "{badge.description.toolong}")
    private String description;

    @NotNull(message = "{badge.criteriatype.absent}")
    private CriteriaType criteriaType;

    @NotNull(message = "{badge.criteriavalue.absent}")
    @Positive(message = "{badge.criteriavalue.invalid}")
    private Double criteriaValue;

    // Bootstrap icon class — e.g. "bi-trophy"
    @NotBlank(message = "{badge.icon.absent}")
    private String badgeIcon;

    // Hex color — e.g. "#ffc107"
    @NotBlank(message = "{badge.color.absent}")
    private String badgeColor;
}
