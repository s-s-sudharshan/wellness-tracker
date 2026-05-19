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
import com.infy.repository.WeeklyGoalRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class WeeklyGoalServiceImpl implements WeeklyGoalService {

    @Autowired
    private WeeklyGoalRepository weeklyGoalRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // Creates a new weekly goal or updates it if one already exists for that user and week.
    // Caller identity derived from JWT — never from request body.
    @Override
    public Integer saveWeeklyGoal(WeeklyGoalRequestDTO requestDTO) throws WellnessTrackerException {
        User caller = authenticatedUserResolver.resolveCurrentUser();

        LocalDate thisWeekMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (!requestDTO.getWeekStartDate().getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            throw new WellnessTrackerException("Service.INVALID_WEEK_START_DATE");
        }
        if (requestDTO.getWeekStartDate().isBefore(thisWeekMonday)) {
            throw new WellnessTrackerException("Service.INVALID_WEEK_START_DATE");
        }

        Optional<WeeklyGoal> existing = weeklyGoalRepository
                .findByUser_UserIdAndWeekStartDate(caller.getUserId(), requestDTO.getWeekStartDate());

        WeeklyGoal weeklyGoal = existing.orElse(new WeeklyGoal());
        weeklyGoal.setUser(caller);
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
    public WeeklyGoalResponseDTO getWeeklyGoal(LocalDate weekStartDate)
            throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        Optional<WeeklyGoal> optional = weeklyGoalRepository
                .findByUser_UserIdAndWeekStartDate(callerId, weekStartDate);
        WeeklyGoal goal = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.WEEKLY_GOAL_NOT_FOUND"));

        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<Object[]> actuals = activityLogRepository
                .findActualsByUserAndDateRange(callerId, weekStartDate, weekEndDate);

        double stepsActual = 0, workoutActual = 0, waterActual = 0,
               meditationActual = 0, sleepActual = 0;

        for (Object[] row : actuals) {
            ActivityType type = (ActivityType) row[0];
            double value = ((Number) row[1]).doubleValue();
            switch (type) {
                case STEPS      -> stepsActual      = value;
                case WORKOUT    -> workoutActual    = value;
                case WATER      -> waterActual      = value;
                case MEDITATION -> meditationActual = value;
                case SLEEP      -> sleepActual      = value;
                default         -> { }
            }
        }

        WeeklyGoalResponseDTO dto = new WeeklyGoalResponseDTO();
        dto.setWeeklyGoalId(goal.getWeeklyGoalId());
        dto.setUserId(callerId);
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

    private Integer calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }
}
