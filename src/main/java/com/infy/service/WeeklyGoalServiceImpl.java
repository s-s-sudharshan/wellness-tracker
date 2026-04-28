package com.infy.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.entity.User;
import com.infy.entity.WeeklyGoal;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.UserRepository;
import com.infy.repository.WeeklyGoalRepository;

@Service
@Transactional
public class WeeklyGoalServiceImpl implements WeeklyGoalService {

	@Autowired
	private WeeklyGoalRepository weeklyGoalRepository;

	@Autowired
	private ActivityLogRepository activityLogRepository;

	@Autowired
	private UserRepository userRepository;

	// Creates a new weekly goal or updates it if one already exists for that user and week.
	@Override
	public Integer saveWeeklyGoal(WeeklyGoalRequestDTO requestDTO) throws WellnessTrackerException {
		Optional<User> optional = userRepository.findById(requestDTO.getUserId());
		User user = optional.orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

		// P1 Fix: Enforce that weekStartDate is a Monday AND falls on next week or later.
		//
		// AC says "set goals for the upcoming week" — this means the current week is already
		// in progress and goals should be set ahead of time for future weeks.
		// "Next week Monday" = today's Monday + 7 days, regardless of what day today is.
		//
		// Two separate checks so the error message can be targeted per violation:
		//   - Non-Monday date → same error key (date is invalid in both cases)
		//   - Date before next Monday → same error key (date is in the past or current week)
		LocalDate nextWeekMonday = LocalDate.now()
				.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

		if (!requestDTO.getWeekStartDate().getDayOfWeek().equals(DayOfWeek.MONDAY)) {
			throw new WellnessTrackerException("Service.INVALID_WEEK_START_DATE");
		}
		if (requestDTO.getWeekStartDate().isBefore(nextWeekMonday)) {
			throw new WellnessTrackerException("Service.INVALID_WEEK_START_DATE");
		}

		Optional<WeeklyGoal> existing = weeklyGoalRepository
				.findByUser_UserIdAndWeekStartDate(requestDTO.getUserId(), requestDTO.getWeekStartDate());

		WeeklyGoal weeklyGoal = existing.orElse(new WeeklyGoal());
		weeklyGoal.setUser(user);
		weeklyGoal.setWeekStartDate(requestDTO.getWeekStartDate());
		weeklyGoal.setStepsGoal(requestDTO.getStepsGoal());
		weeklyGoal.setWorkoutGoal(requestDTO.getWorkoutGoal());
		weeklyGoal.setWaterGoal(requestDTO.getWaterGoal());
		weeklyGoal.setMeditationGoal(requestDTO.getMeditationGoal());
		weeklyGoal.setSleepGoalHours(requestDTO.getSleepGoalHours());

		return weeklyGoalRepository.save(weeklyGoal).getWeeklyGoalId();
	}

	@Override
	@Transactional(readOnly = true)
	public WeeklyGoalResponseDTO getWeeklyGoal(Integer userId, LocalDate weekStartDate)
			throws WellnessTrackerException {
		if (!userRepository.existsById(userId)) {
			throw new WellnessTrackerException("Service.USER_NOT_FOUND");
		}

		Optional<WeeklyGoal> optional = weeklyGoalRepository
				.findByUser_UserIdAndWeekStartDate(userId, weekStartDate);
		WeeklyGoal goal = optional.orElseThrow(() -> new WellnessTrackerException("Service.WEEKLY_GOAL_NOT_FOUND"));

		// Week runs Monday to Sunday (weekStartDate to weekStartDate + 6 days)
		LocalDate weekEndDate = weekStartDate.plusDays(6);

		// Fetch actuals from activity_logs for this week
		List<Object[]> actuals = activityLogRepository
				.findActualsByUserAndDateRange(userId, weekStartDate, weekEndDate);

		double stepsActual = 0, workoutActual = 0, waterActual = 0, meditationActual = 0, sleepActual = 0;

		for (Object[] row : actuals) {
			ActivityType type = (ActivityType) row[0];
			double value = ((Number) row[1]).doubleValue();
			switch (type) {
				case STEPS -> stepsActual = value;
				case WORKOUT -> workoutActual = value;
				case WATER -> waterActual = value;
				case MEDITATION -> meditationActual = value;
				case SLEEP -> sleepActual = value;
				default -> { }
			}
		}

		WeeklyGoalResponseDTO dto = new WeeklyGoalResponseDTO();
		dto.setWeeklyGoalId(goal.getWeeklyGoalId());
		dto.setUserId(userId);
		dto.setWeekStartDate(weekStartDate);
		dto.setWeekEndDate(weekEndDate);
		dto.setStepsGoal(goal.getStepsGoal());
		dto.setWorkoutGoal(goal.getWorkoutGoal());
		dto.setWaterGoal(goal.getWaterGoal());
		dto.setMeditationGoal(goal.getMeditationGoal());
		dto.setSleepGoalHours(goal.getSleepGoalHours());
		dto.setStepsActual(stepsActual);
		dto.setWorkoutActual(workoutActual);
		dto.setWaterActual(waterActual);
		dto.setMeditationActual(meditationActual);
		dto.setSleepActual(sleepActual);
		dto.setStepsProgressPct(calcPct(stepsActual, goal.getStepsGoal()));
		dto.setWorkoutProgressPct(calcPct(workoutActual, goal.getWorkoutGoal()));
		dto.setWaterProgressPct(calcPct(waterActual, goal.getWaterGoal()));
		dto.setMeditationProgressPct(calcPct(meditationActual, goal.getMeditationGoal()));
		dto.setSleepProgressPct(calcPct(sleepActual, goal.getSleepGoalHours()));

		return dto;
	}

	// Calculates progress % capped at 100. Returns 0 if goal is null or zero.
	private Integer calcPct(double actual, Double goal) {
		if (goal == null || goal == 0) return 0;
		int pct = (int) ((actual / goal) * 100);
		return Math.min(pct, 100);
	}
}
