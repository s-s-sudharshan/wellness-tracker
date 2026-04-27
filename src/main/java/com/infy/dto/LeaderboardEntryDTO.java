package com.infy.dto;

import lombok.Data;

@Data
public class LeaderboardEntryDTO {

    private Integer rank;
    private String participantName;
    private Double actualValue;
    private String unit;
    private Integer progressPct;
    private Boolean isCurrentUser;   // true when this entry belongs to the requesting user
}
