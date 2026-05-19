package com.infy.dto;

import java.time.LocalDate;

import com.infy.enums.Difficulty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChallengeUpdateRequestDTO {

    // requestingUserId intentionally removed — caller identity is derived from JWT.
    // The service calls authenticatedUserResolver.resolveCurrentUser() instead.
    // Ownership check (creator == caller) is performed in the service using the JWT identity.

    @NotBlank(message = "{challenge.title.absent}")
    @Size(max = 150, message = "{challenge.title.toolong}")
    private String title;

    @NotBlank(message = "{challenge.description.absent}")
    private String description;

    @NotNull(message = "{challenge.goalvalue.absent}")
    @Positive(message = "{challenge.goalvalue.invalid}")
    private Double goalValue;

    @NotNull(message = "{challenge.difficulty.absent}")
    private Difficulty difficulty;

    @NotNull(message = "{challenge.enddate.absent}")
    private LocalDate endDate;

    private Boolean isFeatured;
}
