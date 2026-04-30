package com.infy.entity;

import java.time.LocalDateTime;

import com.infy.enums.ActivityType;
import com.infy.enums.WellnessArticleStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wellness_articles")
@Getter
@Setter
@NoArgsConstructor
public class WellnessArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer articleId;

    // HR user who published this article
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private String title;

    private String description;

    private String articleUrl;

    // Which activity type this article is relevant to.
    // Null means it is a general wellness article (used for padding / fallback).
    @Enumerated(EnumType.STRING)
    private ActivityType relatedMetric;

    // PUBLISHED articles are shown in recommendations. DRAFT articles are not.
    @Enumerated(EnumType.STRING)
    private WellnessArticleStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = WellnessArticleStatus.DRAFT;
    }
}
