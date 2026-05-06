package com.infy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.BadgeRequestDTO;
import com.infy.dto.BadgeResponseDTO;
import com.infy.entity.Badge;
import com.infy.entity.User;
import com.infy.entity.UserBadge;
import com.infy.enums.ActivityType;
import com.infy.enums.BadgeStatus;
import com.infy.enums.CriteriaType;
import com.infy.enums.NotificationType;
import com.infy.enums.ParticipantStatus;
import com.infy.enums.Role;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.BadgeRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.UserBadgeRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class BadgeServiceImpl implements BadgeService {

    private static final Log LOGGER = LogFactory.getLog(BadgeServiceImpl.class);

    private static final int STREAK_DATE_LIMIT = 400;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public Integer createBadge(BadgeRequestDTO requestDTO) throws WellnessTrackerException {
        resolveManagerOrHr(requestDTO.getRequestingUserId());

        Badge badge = new Badge();
        badge.setTitle(requestDTO.getTitle());
        badge.setDescription(requestDTO.getDescription());
        badge.setCriteriaType(requestDTO.getCriteriaType());
        badge.setCriteriaValue(requestDTO.getCriteriaValue());
        badge.setBadgeIcon(requestDTO.getBadgeIcon());
        badge.setBadgeColor(requestDTO.getBadgeColor());

        return badgeRepository.save(badge).getBadgeId();
    }

    // Manager/HR — edit badge
    @Override
    public BadgeResponseDTO updateBadge(Integer badgeId, BadgeRequestDTO requestDTO)
            throws WellnessTrackerException {
        resolveManagerOrHr(requestDTO.getRequestingUserId());

        Optional<Badge> optional = badgeRepository.findById(badgeId);
        Badge badge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.BADGE_NOT_FOUND"));

        badge.setTitle(requestDTO.getTitle());
        badge.setDescription(requestDTO.getDescription());
        badge.setCriteriaType(requestDTO.getCriteriaType());
        badge.setCriteriaValue(requestDTO.getCriteriaValue());
        badge.setBadgeIcon(requestDTO.getBadgeIcon());
        badge.setBadgeColor(requestDTO.getBadgeColor());

        return mapToBadgeDTO(badgeRepository.save(badge));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponseDTO> getAllBadges() throws WellnessTrackerException {
        List<BadgeResponseDTO> result = new ArrayList<>();
        badgeRepository.findAll().forEach(badge -> result.add(mapToBadgeDTO(badge)));
        return result;
    }

    // -------------------------------------------------------------------------
    // Employee view — all badges with live progress + award on threshold cross
    //
    // Award path is idempotent:
    //   1. earnedMap (loaded once at start) is the fast path for already-earned badges
    //   2. save() is wrapped in try/catch DataIntegrityViolationException — if two
    //      concurrent requests both attempt to award the same badge, the DB unique
    //      constraint rejects one cleanly; the catch path fetches the existing row
    //   3. earnedAt may be null in the extreme edge case where the race-path fetch
    //      finds no row (timing anomaly) — the frontend handles null earnedAt gracefully
    //
    // Response sorted: EARNED (earnedAt desc) → IN_PROGRESS (progressPct desc) → LOCKED
    // newlyUnlocked=true on any badge awarded in this call — frontend uses this for toast
    // -------------------------------------------------------------------------

    @Override
    public List<BadgeResponseDTO> getUserBadges(Integer userId)
            throws WellnessTrackerException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // 1. Compute all metrics in one block — DB calls are batched, not per-badge
        UserMetrics metrics = buildMetrics(userId);

        // 2. Load all badges — direct iteration, no intermediate list or unsafe cast
        // 3. Load already-earned badges into a map for O(1) lookup
        Map<Integer, UserBadge> earnedMap = new HashMap<>();
        for (UserBadge ub : userBadgeRepository.findByUser_UserIdOrderByEarnedAtDesc(userId)) {
            earnedMap.put(ub.getBadge().getBadgeId(), ub);
        }

        // 4. Evaluate each badge
        List<BadgeResponseDTO> earned     = new ArrayList<>();
        List<BadgeResponseDTO> inProgress = new ArrayList<>();
        List<BadgeResponseDTO> locked     = new ArrayList<>();

        for (Badge badge : badgeRepository.findAll()) {
            double target = badge.getCriteriaValue();

            // Skip malformed badge rows — criteriaValue > 0 enforced by API validation.
            // This guard is a safety net for direct DB inserts or legacy data only.
            if (target <= 0) {
                LOGGER.warn("Badge id=" + badge.getBadgeId()
                        + " has invalid criteriaValue=" + target + " — skipped.");
                continue;
            }

            double actual         = resolveActual(badge.getCriteriaType(), metrics);
            int progressPct       = (int) Math.min((actual / target) * 100, 100);
            boolean alreadyEarned = earnedMap.containsKey(badge.getBadgeId());
            boolean newlyUnlocked = false;
            LocalDateTime earnedAt = null;

            if (alreadyEarned) {
                // Fast path — already in earnedMap, no DB call needed
                earnedAt = earnedMap.get(badge.getBadgeId()).getEarnedAt();
            } else if (actual >= target) {
                // Threshold crossed — attempt award
                AwardResult result = awardIfEligible(user, badge, userId);
                earnedAt      = result.earnedAt;
                newlyUnlocked = result.newlyUnlocked;
                alreadyEarned = true;
            }

            BadgeResponseDTO dto = buildDto(badge, actual, progressPct,
                    alreadyEarned, earnedAt, newlyUnlocked);

            if (alreadyEarned) {
                earned.add(dto);
            } else if (actual > 0) {
                inProgress.add(dto);
            } else {
                locked.add(dto);
            }
        }

        // 5. Sort earned by earnedAt descending — most recently earned first
        earned.sort(Comparator.comparing(BadgeResponseDTO::getEarnedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 6. Sort in-progress by progressPct descending — most progress first
        inProgress.sort((a, b) -> Integer.compare(b.getProgressPct(), a.getProgressPct()));

        // 7. Return: EARNED → IN_PROGRESS → LOCKED
        List<BadgeResponseDTO> result = new ArrayList<>();
        result.addAll(earned);
        result.addAll(inProgress);
        result.addAll(locked);
        return result;
    }

    // Award helper — sends notification only on first award (newlyUnlocked).
    // referenceId is null — badge notifications are naturally deduped by the
    // DB unique constraint on user_badges (user_id, badge_id).
    private AwardResult awardIfEligible(User user, Badge badge, Integer userId) {
        UserBadge ub = new UserBadge();
        ub.setUser(user);
        ub.setBadge(badge);

        try {
            UserBadge saved = userBadgeRepository.save(ub);

            // US 10 — notify user on first badge award.
            // null referenceId: badge notifications don't need referenceId-based dedup.
            notificationService.createNotification(
                    userId,
                    NotificationType.BADGE,
                    "Badge Unlocked: " + badge.getTitle(),
                    "You earned the \"" + badge.getTitle() + "\" badge. "
                            + badge.getDescription(),
                    null);

            return new AwardResult(saved.getEarnedAt(), true);
        } catch (DataIntegrityViolationException e) {
            Optional<UserBadge> existing = userBadgeRepository
                    .findByUser_UserIdAndBadge_BadgeId(userId, badge.getBadgeId());
            if (existing.isPresent()) {
                return new AwardResult(existing.get().getEarnedAt(), false);
            }
            throw e;
        }
    }

    // Metrics — all values the badge engine needs, computed once per call
    private UserMetrics buildMetrics(Integer userId) {
        UserMetrics m = new UserMetrics();

        // All-time totals — one grouped query for all 5 activity types
        for (Object[] row : activityLogRepository.getAllTimeTotalsByUser(userId)) {
            ActivityType type  = (ActivityType) row[0];
            double       value = ((Number) row[1]).doubleValue();
            switch (type) {
                case STEPS      -> m.steps      = value;
                case WATER      -> m.water      = value;
                case WORKOUT    -> m.workout    = value;
                case MEDITATION -> m.meditation = value;
                case SLEEP      -> m.sleep      = value;
                default         -> { }
            }
        }

        // Daily personal best (native query — String type name)
        m.dailySteps   = safeDouble(activityLogRepository.getDailyBestByType(userId, ActivityType.STEPS.name()));
        m.dailyWorkout = safeDouble(activityLogRepository.getDailyBestByType(userId, ActivityType.WORKOUT.name()));

        // Weekly personal best (native query — String type name)
        m.weeklySteps   = safeDouble(activityLogRepository.getWeeklyBestByType(userId, ActivityType.STEPS.name()));
        m.weeklyWorkout = safeDouble(activityLogRepository.getWeeklyBestByType(userId, ActivityType.WORKOUT.name()));

        // Streak — pageable limits rows to STREAK_DATE_LIMIT, avoids loading full history
        m.streak = calculateStreak(userId);

        // Social
        m.challengesJoined    = safeInt(participantRepository.countByUser_UserId(userId));
        m.challengesCompleted = safeInt(
                participantRepository.countByUser_UserIdAndStatus(userId, ParticipantStatus.COMPLETED));

        // Consistency
        m.totalLogs       = safeInt(activityLogRepository.countByUser_UserId(userId));
        m.activityVariety = safeInt(activityLogRepository.countDistinctActivityTypes(userId));

        return m;
    }

    private int calculateStreak(Integer userId) {
        List<LocalDate> dates = activityLogRepository.getActiveDates(
                userId, PageRequest.of(0, STREAK_DATE_LIMIT));
        Set<LocalDate> activeDates = new HashSet<>(dates);

        int streak = 0;
        LocalDate date = LocalDate.now();

        if (!activeDates.contains(date)) {
            date = date.minusDays(1);
        }

        while (activeDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    // Maps a CriteriaType to the correct metric value from UserMetrics
    private double resolveActual(CriteriaType type, UserMetrics m) {
        return switch (type) {
            case STEPS                -> m.steps;
            case WATER                -> m.water;
            case WORKOUT              -> m.workout;
            case MEDITATION           -> m.meditation;
            case SLEEP                -> m.sleep;
            case DAILY_STEPS          -> m.dailySteps;
            case DAILY_WORKOUT        -> m.dailyWorkout;
            case WEEKLY_STEPS         -> m.weeklySteps;
            case WEEKLY_WORKOUT       -> m.weeklyWorkout;
            case STREAK               -> m.streak;
            case CHALLENGES_JOINED    -> m.challengesJoined;
            case CHALLENGES_COMPLETED -> m.challengesCompleted;
            case TOTAL_LOGS           -> m.totalLogs;
            case ACTIVITY_VARIETY     -> m.activityVariety;
        };
    }

    // Private helpers
    private void resolveManagerOrHr(Integer userId) throws WellnessTrackerException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));
        if (Role.EMPLOYEE.equals(user.getRole())) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER_OR_HR");
        }
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    // Builds a BadgeResponseDTO with full user progress context
    private BadgeResponseDTO buildDto(Badge badge, double actual, int progressPct,
            boolean earned, LocalDateTime earnedAt, boolean newlyUnlocked) {
        BadgeResponseDTO dto = new BadgeResponseDTO();
        dto.setBadgeId(badge.getBadgeId());
        dto.setTitle(badge.getTitle());
        dto.setDescription(badge.getDescription());
        dto.setCriteriaType(badge.getCriteriaType());
        dto.setCriteriaValue(badge.getCriteriaValue());
        dto.setBadgeIcon(badge.getBadgeIcon());
        dto.setBadgeColor(badge.getBadgeColor());
        dto.setProgress(actual);
        dto.setProgressPct(progressPct);
        dto.setEarned(earned);
        dto.setEarnedAt(earnedAt);
        dto.setNewlyUnlocked(newlyUnlocked);
        if (earned) {
            dto.setBadgeStatus(BadgeStatus.EARNED);
        } else if (actual > 0) {
            dto.setBadgeStatus(BadgeStatus.IN_PROGRESS);
        } else {
            dto.setBadgeStatus(BadgeStatus.LOCKED);
        }
        return dto;
    }

    // Builds a BadgeResponseDTO with no user context
    private BadgeResponseDTO mapToBadgeDTO(Badge badge) {
        BadgeResponseDTO dto = new BadgeResponseDTO();
        dto.setBadgeId(badge.getBadgeId());
        dto.setTitle(badge.getTitle());
        dto.setDescription(badge.getDescription());
        dto.setCriteriaType(badge.getCriteriaType());
        dto.setCriteriaValue(badge.getCriteriaValue());
        dto.setBadgeIcon(badge.getBadgeIcon());
        dto.setBadgeColor(badge.getBadgeColor());
        dto.setProgress(0);
        dto.setProgressPct(0);
        dto.setBadgeStatus(BadgeStatus.LOCKED);
        dto.setEarned(false);
        dto.setNewlyUnlocked(false);
        return dto;
    }

    private static class UserMetrics {
        double steps, water, workout, meditation, sleep;
        double dailySteps, dailyWorkout;
        double weeklySteps, weeklyWorkout;
        int streak;
        int challengesJoined, challengesCompleted;
        int totalLogs, activityVariety;
    }

    // Return type for awardIfEligible — carries earnedAt and newlyUnlocked together
    private static class AwardResult {
        final LocalDateTime earnedAt;
        final boolean newlyUnlocked;

        AwardResult(LocalDateTime earnedAt, boolean newlyUnlocked) {
            this.earnedAt      = earnedAt;
            this.newlyUnlocked = newlyUnlocked;
        }
    }
}
