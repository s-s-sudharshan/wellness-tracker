package com.infy.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.WeeklyGoal;

public interface WeeklyGoalRepository extends CrudRepository<WeeklyGoal, Integer> {

	Optional<WeeklyGoal> findByUser_UserIdAndWeekStartDate(Integer userId, LocalDate weekStartDate);
}
