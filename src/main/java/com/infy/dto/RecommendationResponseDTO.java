package com.infy.dto;

import java.time.LocalDateTime;

import com.infy.enums.RecommendationStatus;
import com.infy.enums.RecommendationType;

import lombok.Data;

@Data
public class RecommendationResponseDTO {

    private Integer recommendationId;
    private Integer userId;
    private RecommendationType recommendationType;
    private String title;
    private String description;
 
    // Present when recommendationType = CHALLENGE — frontend uses this for "Join Now" / "View" action
    private Integer challengeId;
 
    // Present when recommendationType = ARTICLE — frontend links to this URL
    private String articleUrl;
 
    private RecommendationStatus status;
    private LocalDateTime createdAt;
}
