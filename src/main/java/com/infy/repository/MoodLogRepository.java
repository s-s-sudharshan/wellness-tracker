package com.infy.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.MoodLog;

public interface MoodLogRepository extends CrudRepository<MoodLog, Integer> {

	Optional<MoodLog> findByUser_UserIdAndLogDate(Integer userId, LocalDate logDate);

	List<MoodLog> findByUser_UserIdAndLogDateBetweenOrderByLogDateAsc(
			Integer userId, LocalDate fromDate, LocalDate toDate);
}
