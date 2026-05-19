package com.infy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinChallengeRequestDTO {

    // userId intentionally removed — caller identity is derived from JWT.
    // The service calls authenticatedUserResolver.resolveCurrentUser() instead.

    @NotNull(message = "{participant.challengeid.absent}")
    private Integer challengeId;
}
