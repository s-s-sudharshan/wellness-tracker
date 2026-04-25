package com.infy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.VisibilityType;

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
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
public class Challenge {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer challengeId;

    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Enumerated(EnumType.STRING)
    private ActivityType metricType;

    private Double goalValue;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private VisibilityType visibilityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // reward_badge_id left as nullable FK — badges (US 05) not yet implemented
    private Integer rewardBadgeId;

    private Boolean isFeatured;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isFeatured == null) this.isFeatured = false;
        if (this.status == null) this.status = ChallengeStatus.UPCOMING;
    }
}
