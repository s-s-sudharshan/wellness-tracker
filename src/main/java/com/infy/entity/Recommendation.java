package com.infy.entity;

import java.time.LocalDateTime;

import com.infy.enums.RecommendationStatus;
import com.infy.enums.RecommendationType;

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
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recommendationId;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Enumerated(EnumType.STRING)
    private RecommendationType recommendationType;
 
    private String title;
 
    private String description;
 
    // Nullable — only set when recommendationType = CHALLENGE
    private Integer challengeId;
 
    // Nullable — only set when recommendationType = ARTICLE
    private String articleUrl;
 
    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;
 
    private LocalDateTime createdAt;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = RecommendationStatus.ACTIVE;
    }
}
