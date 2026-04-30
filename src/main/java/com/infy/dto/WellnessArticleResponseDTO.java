package com.infy.dto;

import java.time.LocalDateTime;

import com.infy.enums.ActivityType;
import com.infy.enums.WellnessArticleStatus;

import lombok.Data;

@Data
public class WellnessArticleResponseDTO {

    private Integer articleId;
    private Integer createdBy;
    private String createdByName;
    private String title;
    private String description;
    private String articleUrl;
    private ActivityType relatedMetric;
    private WellnessArticleStatus status;
    private LocalDateTime createdAt;
}
