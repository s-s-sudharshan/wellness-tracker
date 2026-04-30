package com.infy.dto;

import com.infy.enums.ActivityType;
import com.infy.enums.WellnessArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WellnessArticleRequestDTO {

    @NotNull(message = "{article.createdby.absent}")
    private Integer createdBy;

    @NotBlank(message = "{article.title.absent}")
    @Size(max = 150, message = "{article.title.toolong}")
    private String title;

    @NotBlank(message = "{article.description.absent}")
    @Size(max = 500, message = "{article.description.toolong}")
    private String description;

    @NotBlank(message = "{article.url.absent}")
    @Size(max = 500, message = "{article.url.toolong}")
    private String articleUrl;

    // Optional — links this article to a specific metric for targeted recommendations.
    // Null means the article is a general wellness article used for padding.
    private ActivityType relatedMetric;

    // Optional — defaults to DRAFT on create. Set to PUBLISHED to make it live.
    private WellnessArticleStatus status;
}
