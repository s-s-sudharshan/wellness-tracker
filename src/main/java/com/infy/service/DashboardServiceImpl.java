package com.infy.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivityMetricDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.DashboardResponseDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.dto.RecommendationResponseDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.entity.ActivityLog;
import com.infy.entity.Challenge;
import com.infy.entity.MoodLog;
import com.infy.entity.Recommendation;
import com.infy.entity.User;
import com.infy.entity.WeeklyGoal;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.RecommendationStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.MoodLogRepository;
import com.infy.repository.NotificationRepository;
import com.infy.repository.RecommendationRepository;
import com.infy.repository.WeeklyGoalRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private static final int RECOMMENDED_CHALLENGES_MAX = 5;


    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private MoodLogRepository moodLogRepository;

    @Autowired
    private WeeklyGoalRepository weeklyGoalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private ChallengeParticipantService participantService;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // userId derived from JWT — never from a caller-supplied parameter.
    @Override
    public DashboardResponseDTO getDashboard() throws WellnessTrackerException {
        User caller = authenticatedUserResolver.resolveCurrentUser();
        Integer userId = caller.getUserId();

        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        DashboardResponseDTO response = new DashboardResponseDTO();

        // 1. Recent activities — top 5, newest first
        List<ActivityLog> recentLogs = activityLogRepository
                .findTop5ByUser_UserIdOrderByActivityDateDescCreatedAtDesc(userId);
        List<ActivityLogResponseDTO> recentActivities = new ArrayList<>();
        for (ActivityLog log : recentLogs) {
            recentActivities.add(mapActivityToDTO(log));
        }
        response.setRecentActivities(recentActivities);

        // 2. Active joined challenges — getMyChallenges() now derives userId from JWT
        List<MyChallengeResponseDTO> activeChallenges = new ArrayList<>();
        List<MyChallengeResponseDTO> allJoined = participantService.getMyChallenges();
        for (MyChallengeResponseDTO dto : allJoined) {
            if (ChallengeStatus.ACTIVE.equals(dto.getChallengeStatus())) {
                activeChallenges.add(dto);
            }
        }
        response.setActiveChallenges(activeChallenges);

        // 3. This week's activity summary — Monday to today
        Integer totalActivities = activityLogRepository
                .countByUser_UserIdAndActivityDateBetween(userId, thisWeekMonday, today);

        List<Object[]> rawSummary = activityLogRepository
                .findSummaryByUserAndDateRange(userId, thisWeekMonday, today);

        List<ActivityMetricDTO> metrics = new ArrayList<>();
        for (Object[] row : rawSummary) {
            ActivityMetricDTO metric = new ActivityMetricDTO();
            metric.setActivityType((ActivityType) row[0]);
            metric.setTotalValue(((Number) row[1]).doubleValue());
            metric.setUnit((String) row[2]);
            metrics.add(metric);
        }

        ActivitySummaryDTO weekSummary = new ActivitySummaryDTO();
        weekSummary.setUserId(userId);
        weekSummary.setFromDate(thisWeekMonday);
        weekSummary.setToDate(today);
        weekSummary.setTotalActivities(totalActivities != null ? totalActivities : 0);
        weekSummary.setMetrics(metrics);
        response.setWeekSummary(weekSummary);

        // 4. Weekly goal for this week — null if not set (never throws)
        Optional<WeeklyGoal> goalOptional = weeklyGoalRepository
                .findByUser_UserIdAndWeekStartDate(userId, thisWeekMonday);
        if (goalOptional.isPresent()) {
            WeeklyGoal goal = goalOptional.get();
            LocalDate weekEndDate = thisWeekMonday.plusDays(6);

            List<Object[]> actuals = activityLogRepository
                    .findActualsByUserAndDateRange(userId, thisWeekMonday, today);

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

            WeeklyGoalResponseDTO goalDTO = new WeeklyGoalResponseDTO();
            goalDTO.setWeeklyGoalId(goal.getWeeklyGoalId());
            goalDTO.setUserId(userId);
            goalDTO.setWeekStartDate(thisWeekMonday);
            goalDTO.setWeekEndDate(weekEndDate);
            goalDTO.setStepsGoal(goal.getStepsGoal());
            goalDTO.setWorkoutGoal(goal.getWorkoutGoal());
            goalDTO.setWaterGoal(goal.getWaterGoal());
            goalDTO.setMeditationGoal(goal.getMeditationGoal());
            goalDTO.setSleepGoalHours(goal.getSleepGoalHours());
            goalDTO.setStepsActual(stepsActual);
            goalDTO.setWorkoutActual(workoutActual);
            goalDTO.setWaterActual(waterActual);
            goalDTO.setMeditationActual(meditationActual);
            goalDTO.setSleepActual(sleepActual);
            goalDTO.setStepsProgressPct(calcPct(stepsActual, goal.getStepsGoal()));
            goalDTO.setWorkoutProgressPct(calcPct(workoutActual, goal.getWorkoutGoal()));
            goalDTO.setWaterProgressPct(calcPct(waterActual, goal.getWaterGoal()));
            goalDTO.setMeditationProgressPct(calcPct(meditationActual, goal.getMeditationGoal()));
            goalDTO.setSleepProgressPct(calcPct(sleepActual, goal.getSleepGoalHours()));

            response.setWeeklyGoal(goalDTO);
        }

        // 5. Today's mood log — null if not logged today (never throws)
        Optional<MoodLog> moodOptional = moodLogRepository
                .findByUser_UserIdAndLogDate(userId, today);
        if (moodOptional.isPresent()) {
            MoodLog mood = moodOptional.get();
            MoodLogResponseDTO moodDTO = new MoodLogResponseDTO();
            moodDTO.setMoodLogId(mood.getMoodLogId());
            moodDTO.setUserId(mood.getUser().getUserId());
            moodDTO.setLogDate(mood.getLogDate());
            moodDTO.setMoodScore(mood.getMoodScore());
            moodDTO.setMoodLabel(getMoodLabel(mood.getMoodScore()));
            moodDTO.setNote(mood.getNote());
            moodDTO.setCreatedAt(mood.getCreatedAt());
            response.setTodayMood(moodDTO);
        }

        // 6. Unread notification count
        Integer unreadCount = notificationRepository
                .countByUser_UserIdAndIsReadFalse(userId);
        response.setUnreadNotificationCount(unreadCount != null ? unreadCount : 0);

        // 7. US 07 — Featured challenges visible to caller's department.
        Integer userDeptId = caller.getDepartment() != null
                ? caller.getDepartment().getDepartmentId() : null;

        if (userDeptId != null) {
            List<Challenge> featuredList = challengeRepository
                    .findFeaturedChallengesForDepartment(today, userDeptId);

            List<Integer> featuredIds = new ArrayList<>();
            for (Challenge c : featuredList) {
                featuredIds.add(c.getChallengeId());
            }

            Set<Integer> joinedFeaturedIds = featuredIds.isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(participantRepository
                            .findJoinedChallengeIdsByUser(userId, featuredIds));

            List<ActiveChallengeResponseDTO> featuredDTOs = new ArrayList<>();
            for (Challenge c : featuredList) {
                ActiveChallengeResponseDTO dto = mapToChallengeDTO(c, today);
                dto.setAlreadyJoined(joinedFeaturedIds.contains(c.getChallengeId()));
                featuredDTOs.add(dto);
            }
            response.setFeaturedChallenges(featuredDTOs);
        } else {
            response.setFeaturedChallenges(new ArrayList<>());
        }

        // 8. US 07 — Recommended challenges ranked by last-30-days activity frequency.
        response.setRecommendedChallenges(
                buildRecommendedChallenges(userId, userDeptId, today));

        // 9. US 08 — Read persisted ACTIVE recommendations directly.
        // Dashboard GET must not regenerate recommendation rows on every refresh.
        List<Recommendation> persistedRecs = recommendationRepository
                .findByUser_UserIdAndStatusOrderByCreatedAtDesc(
                        userId, RecommendationStatus.ACTIVE);
        List<RecommendationResponseDTO> recommendations = new ArrayList<>();
        for (Recommendation rec : persistedRecs) {
            recommendations.add(mapRecommendationToDTO(rec));
        }
        response.setRecommendations(recommendations);

        return response;
    }

    private List<ActiveChallengeResponseDTO> buildRecommendedChallenges(
            Integer userId, Integer userDeptId, LocalDate today) {

        if (userDeptId == null) {
            return new ArrayList<>();
        }

        LocalDate thirtyDaysAgo = today.minusDays(29);

        List<Object[]> frequencyRows = activityLogRepository
                .countActivityFrequencyByUserAndDateRange(userId, thirtyDaysAgo, today);

        if (frequencyRows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<ActivityType, Long> frequencyMap = new LinkedHashMap<>();
        for (Object[] row : frequencyRows) {
            ActivityType type  = (ActivityType) row[0];
            long count         = ((Number) row[1]).longValue();
            frequencyMap.put(type, count);
        }

        List<Challenge> visibleChallenges = challengeRepository
                .findVisibleChallengesForDepartment(today, userDeptId);

        if (visibleChallenges.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> visibleIds = new ArrayList<>();
        for (Challenge c : visibleChallenges) {
            visibleIds.add(c.getChallengeId());
        }
        Set<Integer> joinedIds = new HashSet<>(
                participantRepository.findJoinedChallengeIdsByUser(userId, visibleIds));

        List<ActiveChallengeResponseDTO> result = new ArrayList<>();
        for (ActivityType metric : frequencyMap.keySet()) {
            if (result.size() >= RECOMMENDED_CHALLENGES_MAX) {
                break;
            }
            for (Challenge c : visibleChallenges) {
                if (result.size() >= RECOMMENDED_CHALLENGES_MAX) {
                    break;
                }
                if (metric.equals(c.getMetricType()) && !joinedIds.contains(c.getChallengeId())) {
                    ActiveChallengeResponseDTO dto = mapToChallengeDTO(c, today);
                    dto.setAlreadyJoined(false);
                    result.add(dto);
                }
            }
        }

        return result;
    }

    private ActiveChallengeResponseDTO mapToChallengeDTO(Challenge c, LocalDate today) {
        long rawDays = ChronoUnit.DAYS.between(today, c.getEndDate());
        int daysRemaining = (int) Math.max(0, rawDays);

        ActiveChallengeResponseDTO dto = new ActiveChallengeResponseDTO();
        dto.setChallengeId(c.getChallengeId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCreatedByName(
                c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName());
        dto.setMetricType(c.getMetricType());
        dto.setUnit(resolveUnit(c.getMetricType()));
        dto.setGoalValue(c.getGoalValue());
        dto.setDifficulty(c.getDifficulty());
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setDaysRemaining(daysRemaining);
        dto.setIsFeatured(c.getIsFeatured());
        dto.setStatus(c.getStatus());
        if (c.getDepartment() != null) {
            dto.setDepartmentName(c.getDepartment().getDepartmentName());
        }
        if (c.getRewardBadgeId() != null) {
            dto.setRewardBadgeId(c.getRewardBadgeId());
        }
        return dto;
    }

    private ActivityLogResponseDTO mapActivityToDTO(ActivityLog log) {
        ActivityLogResponseDTO dto = new ActivityLogResponseDTO();
        dto.setActivityLogId(log.getActivityLogId());
        dto.setUserId(log.getUser().getUserId());
        dto.setUserName(log.getUser().getFirstName() + " " + log.getUser().getLastName());
        dto.setActivityType(log.getActivityType());
        dto.setActivityDate(log.getActivityDate());
        dto.setActivityValue(log.getActivityValue());
        dto.setUnit(log.getUnit());
        dto.setNotes(log.getNotes());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    private String getMoodLabel(Integer score) {
        return switch (score) {
            case 1 -> "Very Low";
            case 2 -> "Low";
            case 3 -> "OK";
            case 4 -> "Good";
            case 5 -> "Great";
            default -> "Unknown";
        };
    }

    private Integer calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }

    private RecommendationResponseDTO mapRecommendationToDTO(Recommendation rec) {
        RecommendationResponseDTO dto = new RecommendationResponseDTO();
        dto.setRecommendationId(rec.getRecommendationId());
        dto.setUserId(rec.getUser().getUserId());
        dto.setRecommendationType(rec.getRecommendationType());
        dto.setTitle(rec.getTitle());
        dto.setDescription(rec.getDescription());
        dto.setChallengeId(rec.getChallengeId());
        dto.setArticleUrl(rec.getArticleUrl());
        dto.setStatus(rec.getStatus());
        dto.setCreatedAt(rec.getCreatedAt());
        return dto;
    }

    private String resolveUnit(ActivityType metricType) {
        return switch (metricType) {
            case STEPS      -> "steps";
            case WORKOUT    -> "minutes";
            case MEDITATION -> "minutes";
            case WATER      -> "liters";
            case SLEEP      -> "hours";
            case OTHER      -> "units";
        };
    }
}
