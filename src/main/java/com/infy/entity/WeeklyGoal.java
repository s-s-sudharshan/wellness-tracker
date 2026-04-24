package com.infy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
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
@Table(name = "weekly_goals")
@Getter
@Setter
@NoArgsConstructor
public class WeeklyGoal {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer weeklyGoalId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	private LocalDate weekStartDate;
	
	private Double stepsGoal;

	private Double workoutGoal;

	private Double waterGoal;

	private Double meditationGoal;

	private Double sleepGoalHours;

	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
