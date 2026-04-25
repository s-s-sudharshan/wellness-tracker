package com.infy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JoinChallengeRequestDTO {

    @NotNull(message = "{participant.userid.absent}")
    private Integer userId;

    @NotNull(message = "{participant.challengeid.absent}")
    private Integer challengeId;
}
