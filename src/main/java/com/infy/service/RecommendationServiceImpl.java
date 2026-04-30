package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.RecommendationResponseDTO;
import com.infy.entity.Challenge;
import com.infy.entity.Recommendation;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.RecommendationStatus;
import com.infy.enums.RecommendationType;
import com.infy.enums.Role;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.RecommendationRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    // -------------------------------------------------------------------------
    // Thresholds — what counts as "low" activity over the past 7 days
    // -------------------------------------------------------------------------
    private static final double LOW_WATER_LITERS    = 10.0;  // ~1.4 L/day
    private static final double LOW_STEPS           = 35000; // ~5k steps/day
    private static final double LOW_WORKOUT_MINUTES = 60.0;  // 60 min/week
    private static final double LOW_MEDITATION_MIN  = 30.0;  // 30 min/week
    private static final double LOW_SLEEP_HOURS     = 42.0;  // ~6 hrs/night

    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int MIN_RECOMMENDATIONS = 3;

    // -------------------------------------------------------------------------
    // Simple article value holder. URLs are guaranteed unique by the developer.
    // -------------------------------------------------------------------------
    private record Article(String title, String description, String url) {}

    // Fallback articles — shown to highly active users or when no matching
    // challenge exists for multiple metrics. URLs are developer-guaranteed unique.
    private static final List<Article> FALLBACK_ARTICLES = List.of(
        new Article(
            "Your Weekly Wellness Review",
            "Consistent logging is the first step toward lasting wellness habits. "
            + "Check out our guide to building a balanced weekly routine.",
            "https://www.who.int/news-room/fact-sheets/detail/physical-activity"
        ),
        new Article(
            "The Science of Building Healthy Habits",
            "Small daily actions compound into big results. Learn how habit stacking "
            + "can help you embed wellness into your everyday routine.",
            "https://www.healthline.com/health/how-to-build-healthy-habits"
        ),
        new Article(
            "Why Tracking Your Activity Makes a Difference",
            "Research shows that people who log their activity are significantly more "
            + "likely to hit their wellness goals. Here is how to make it work for you.",
            "https://www.healthline.com/nutrition/self-monitoring-health"
        ),
        new Article(
            "Balancing Work and Wellbeing",
            "High performers protect their recovery time as much as their work time. "
            + "Explore practical strategies for maintaining balance at a demanding job.",
            "https://www.mindful.org/how-to-make-mindfulness-a-habit/"
        ),
        new Article(
            "Nutrition Basics for an Active Lifestyle",
            "What you eat directly fuels your wellness activities. "
            + "A simple primer on eating well without overhauling your entire diet.",
            "https://www.healthline.com/nutrition/healthy-eating-tips"
        )
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ChallengeStatusSyncService statusSyncService;

    // =========================================================================
    // Main entry point
    // =========================================================================

    @Override
    public List<RecommendationResponseDTO> getRecommendations(Integer userId)
            throws WellnessTrackerException {

        // --- Validate user and determine role capabilities ---
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // HR cannot join challenges — challenge branches are skipped for them.
        // Article suggestions still apply (session feedback Section 3c).
        // Role.HR.equals(...) is null-safe — avoids NPE if role is null in the DB.
        boolean canJoinChallenges = !Role.HR.equals(user.getRole());

        if (canJoinChallenges) {
            statusSyncService.syncStatuses();
        }

        // --- Step 1: Gather 7-day activity totals ---
        LocalDate today   = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        double totalWater = 0, totalSteps = 0, totalWorkout = 0,
               totalMeditation = 0, totalSleep = 0;

        for (Object[] row : activityLogRepository
                .findActualsByUserAndDateRange(userId, weekAgo, today)) {
            ActivityType type  = (ActivityType) row[0];
            double       value = ((Number) row[1]).doubleValue();
            switch (type) {
                case WATER      -> totalWater      = value;
                case STEPS      -> totalSteps      = value;
                case WORKOUT    -> totalWorkout    = value;
                case MEDITATION -> totalMeditation = value;
                case SLEEP      -> totalSleep      = value;
                default         -> { }
            }
        }

        // --- Step 2: Load visible challenges and already-joined IDs ---
        Integer userDeptId = user.getDepartment() != null
                ? user.getDepartment().getDepartmentId() : null;

        List<Challenge> visibleChallenges = (canJoinChallenges && userDeptId != null)
                ? challengeRepository.findVisibleChallengesForDepartment(today, userDeptId)
                : new ArrayList<>();

        List<Integer> visibleIds = visibleChallenges.stream()
                .map(Challenge::getChallengeId)
                .toList();

        Set<Integer> joinedChallengeIds = visibleIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(participantRepository
                        .findJoinedChallengeIdsByUser(userId, visibleIds));

        // --- Step 3: Dedup trackers ---
        Set<Integer> seenChallengeIds = new HashSet<>();
        Set<String>  seenArticleUrls  = new HashSet<>();

        List<Recommendation> candidates = new ArrayList<>();

        // --- Step 4: Rule engine ---
        // One if block per metric, priority ordered. Each rule tries a matching
        // challenge first and falls back to a paired article if none is available.

        // Rule 1 — Low hydration
        if (totalWater < LOW_WATER_LITERS) {
            Challenge c = findBestChallenge(
                    visibleChallenges, joinedChallengeIds, seenChallengeIds,
                    ActivityType.WATER);
            if (c != null) {
                tryAddChallenge(candidates, seenChallengeIds, user, c,
                        "Stay Hydrated — Join a Water Challenge",
                        "Your water intake this week is below the recommended level. "
                        + "Joining this challenge will help you build a daily hydration habit.");
            } else {
                tryAddArticle(candidates, seenArticleUrls, user,
                        "Boost Your Daily Water Intake",
                        "You have logged less than 1.5 litres of water per day on average "
                        + "this week. Read our tips on easy ways to stay hydrated throughout "
                        + "the workday.",
                        "https://www.healthline.com/nutrition/how-much-water-should-you-drink-per-day");
            }
        }

        // Rule 2 — Low steps
        if (totalSteps < LOW_STEPS && candidates.size() < MAX_RECOMMENDATIONS) {
            Challenge c = findBestChallenge(
                    visibleChallenges, joinedChallengeIds, seenChallengeIds,
                    ActivityType.STEPS);
            if (c != null) {
                tryAddChallenge(candidates, seenChallengeIds, user, c,
                        "Get Moving — Steps Challenge",
                        "You are averaging fewer than 5,000 steps a day this week. "
                        + "This challenge will help you hit a healthy daily step target.");
            } else {
                tryAddArticle(candidates, seenArticleUrls, user,
                        "Simple Ways to Walk More Every Day",
                        "Small changes like taking the stairs or a lunchtime walk can "
                        + "significantly increase your daily step count.",
                        "https://www.healthline.com/health/how-to-increase-steps-per-day");
            }
        }

        // Rule 3 — Low workout
        if (totalWorkout < LOW_WORKOUT_MINUTES && candidates.size() < MAX_RECOMMENDATIONS) {
            Challenge c = findBestChallenge(
                    visibleChallenges, joinedChallengeIds, seenChallengeIds,
                    ActivityType.WORKOUT);
            if (c != null) {
                tryAddChallenge(candidates, seenChallengeIds, user, c,
                        "Level Up — Join a Workout Challenge",
                        "You have logged less than 60 minutes of workout this week. "
                        + "Even short sessions add up — this challenge is a great motivator.");
            } else {
                tryAddArticle(candidates, seenArticleUrls, user,
                        "Fitting Exercise Into a Busy Workday",
                        "Short desk workouts and micro-breaks can deliver real fitness benefits "
                        + "even when your schedule is packed.",
                        "https://www.healthline.com/health/desk-exercises");
            }
        }

        // Rule 4 — Low sleep (article fires first, then challenge — sleep challenges are rare)
        if (totalSleep < LOW_SLEEP_HOURS && candidates.size() < MAX_RECOMMENDATIONS) {
            tryAddArticle(candidates, seenArticleUrls, user,
                    "Better Sleep for Better Performance",
                    "You have averaged less than 6 hours of sleep per night this week. "
                    + "Quality sleep is the foundation of physical and mental recovery.",
                    "https://www.sleepfoundation.org/sleep-hygiene");

            if (candidates.size() < MAX_RECOMMENDATIONS) {
                Challenge c = findBestChallenge(
                        visibleChallenges, joinedChallengeIds, seenChallengeIds,
                        ActivityType.SLEEP);
                if (c != null) {
                    tryAddChallenge(candidates, seenChallengeIds, user, c,
                            "Sleep Reset Challenge",
                            "Log your sleep this week and aim for the recommended target. "
                            + "Consistent rest tracking is the first step to better recovery.");
                }
            }
        }

        // Rule 5 — Low meditation
        if (totalMeditation < LOW_MEDITATION_MIN && candidates.size() < MAX_RECOMMENDATIONS) {
            Challenge c = findBestChallenge(
                    visibleChallenges, joinedChallengeIds, seenChallengeIds,
                    ActivityType.MEDITATION);
            if (c != null) {
                tryAddChallenge(candidates, seenChallengeIds, user, c,
                        "Find Your Focus — Meditation Challenge",
                        "Even 5 minutes of daily mindfulness can reduce stress and sharpen focus. "
                        + "Join this challenge to build the habit.");
            } else {
                tryAddArticle(candidates, seenArticleUrls, user,
                        "Getting Started with Workplace Mindfulness",
                        "You have not logged much meditation this week. "
                        + "This guide covers simple techniques that fit into any lunch break.",
                        "https://www.mindful.org/meditation/mindfulness-getting-started/");
            }
        }

        // --- Step 5: Padding ---
        // Pass A — unjoinable visible challenges not yet surfaced by any rule,
        //          sorted by relevance (featured → active → lowest metric ratio).
        if (candidates.size() < MIN_RECOMMENDATIONS && canJoinChallenges) {
            List<Challenge> paddingPool = buildPaddingPool(
                    visibleChallenges, joinedChallengeIds, seenChallengeIds,
                    totalWater, totalSteps, totalWorkout, totalMeditation, totalSleep);

            for (Challenge c : paddingPool) {
                if (candidates.size() >= MAX_RECOMMENDATIONS) break;
                tryAddChallenge(candidates, seenChallengeIds, user, c,
                        "Challenge Available: " + c.getTitle(),
                        c.getDescription().length() > 200
                                ? c.getDescription().substring(0, 200) + "..."
                                : c.getDescription());
            }
        }

        // Pass B — fallback articles until minimum is reached.
        // URLs are developer-guaranteed unique, so tryAddArticle will never
        // silently skip a fallback entry. Minimum is always reached.
        int fallbackIndex = 0;
        while (candidates.size() < MIN_RECOMMENDATIONS
                && fallbackIndex < FALLBACK_ARTICLES.size()) {
            Article a = FALLBACK_ARTICLES.get(fallbackIndex++);
            tryAddArticle(candidates, seenArticleUrls, user,
                    a.title(), a.description(), a.url());
        }

        // --- Step 6: Persist and return (single exit point) ---
        return persistAndReturn(userId, candidates);
    }

    // =========================================================================
    // Add helpers — enforce MAX cap and dedup before appending
    // =========================================================================

    private void tryAddChallenge(
            List<Recommendation> candidates,
            Set<Integer> seenChallengeIds,
            User user,
            Challenge challenge,
            String title,
            String description) {

        if (candidates.size() >= MAX_RECOMMENDATIONS) return;
        if (seenChallengeIds.contains(challenge.getChallengeId())) return;

        candidates.add(buildChallengeRec(user, challenge, title, description));
        seenChallengeIds.add(challenge.getChallengeId());
    }

    private void tryAddArticle(
            List<Recommendation> candidates,
            Set<String> seenArticleUrls,
            User user,
            String title,
            String description,
            String url) {

        if (candidates.size() >= MAX_RECOMMENDATIONS) return;
        if (seenArticleUrls.contains(url)) return;

        candidates.add(buildArticleRec(user, title, description, url));
        seenArticleUrls.add(url);
    }

    // =========================================================================
    // Challenge selection
    // =========================================================================

    // Finds the best unjoinable, unseen challenge for a given metric type.
    // Prefers ACTIVE over UPCOMING; within the same status, picks ending soonest.
    private Challenge findBestChallenge(
            List<Challenge> visible,
            Set<Integer> joinedIds,
            Set<Integer> seenIds,
            ActivityType type) {

        Challenge bestActive   = null;
        Challenge bestUpcoming = null;

        for (Challenge c : visible) {
            if (!c.getMetricType().equals(type)) continue;
            if (joinedIds.contains(c.getChallengeId())) continue;
            if (seenIds.contains(c.getChallengeId())) continue;

            if (c.getStatus() == ChallengeStatus.ACTIVE) {
                if (bestActive == null
                        || c.getEndDate().isBefore(bestActive.getEndDate())) {
                    bestActive = c;
                }
            } else if (c.getStatus() == ChallengeStatus.UPCOMING) {
                if (bestUpcoming == null
                        || c.getStartDate().isBefore(bestUpcoming.getStartDate())) {
                    bestUpcoming = c;
                }
            }
        }

        return bestActive != null ? bestActive : bestUpcoming;
    }

    // Builds the sorted padding pool (Pass A).
    // Excludes joined and already-seen challenges.
    // Sort: featured first → ACTIVE before UPCOMING → lowest metric ratio first.
    private List<Challenge> buildPaddingPool(
            List<Challenge> visibleChallenges,
            Set<Integer> joinedIds,
            Set<Integer> seenIds,
            double water, double steps, double workout,
            double meditation, double sleep) {

        List<Challenge> pool = new ArrayList<>();
        for (Challenge c : visibleChallenges) {
            if (joinedIds.contains(c.getChallengeId())) continue;
            if (seenIds.contains(c.getChallengeId())) continue;
            pool.add(c);
        }

        pool.sort((a, b) -> {
            boolean aFeat = Boolean.TRUE.equals(a.getIsFeatured());
            boolean bFeat = Boolean.TRUE.equals(b.getIsFeatured());
            if (aFeat && !bFeat) return -1;
            if (!aFeat && bFeat) return 1;

            boolean aActive = a.getStatus() == ChallengeStatus.ACTIVE;
            boolean bActive = b.getStatus() == ChallengeStatus.ACTIVE;
            if (aActive && !bActive) return -1;
            if (!aActive && bActive) return 1;

            double aRatio = getMetricRatio(
                    a.getMetricType(), water, steps, workout, meditation, sleep);
            double bRatio = getMetricRatio(
                    b.getMetricType(), water, steps, workout, meditation, sleep);
            return Double.compare(aRatio, bRatio);
        });

        return pool;
    }

    // Returns the user's actual as a ratio of the threshold for a given metric.
    // 0.0 = no activity (highest padding priority). 1.0+ = threshold met (lowest).
    // OTHER is treated as neutral so it sorts to the end of the padding pool.
    private double getMetricRatio(
            ActivityType type,
            double water, double steps, double workout,
            double meditation, double sleep) {
        return switch (type) {
            case WATER      -> water      / LOW_WATER_LITERS;
            case STEPS      -> steps      / LOW_STEPS;
            case WORKOUT    -> workout    / LOW_WORKOUT_MINUTES;
            case MEDITATION -> meditation / LOW_MEDITATION_MIN;
            case SLEEP      -> sleep      / LOW_SLEEP_HOURS;
            case OTHER      -> 1.0;
        };
    }

    // =========================================================================
    // Entity builders — baseRec handles the four shared fields
    // =========================================================================

    private Recommendation baseRec(User user, RecommendationType type,
            String title, String description) {
        Recommendation rec = new Recommendation();
        rec.setUser(user);
        rec.setRecommendationType(type);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setStatus(RecommendationStatus.ACTIVE);
        return rec;
    }

    private Recommendation buildChallengeRec(User user, Challenge challenge,
            String title, String description) {
        Recommendation rec = baseRec(user, RecommendationType.CHALLENGE, title, description);
        rec.setChallengeId(challenge.getChallengeId());
        return rec;
    }

    private Recommendation buildArticleRec(User user, String title,
            String description, String url) {
        Recommendation rec = baseRec(user, RecommendationType.ARTICLE, title, description);
        rec.setArticleUrl(url);
        return rec;
    }

    // =========================================================================
    // Persist and map — single exit point for every code path.
    //
    // Deletes existing ACTIVE recommendations before saving the new batch.
    // DISMISSED recommendations are intentionally preserved (status filter in query).
    // Delete-then-insert keeps logic simple — the rule engine always produces a
    // fresh variable-length list, so diffing existing rows adds no value.
    // =========================================================================

    private List<RecommendationResponseDTO> persistAndReturn(
            Integer userId, List<Recommendation> candidates) {

        recommendationRepository.deleteActiveByUserId(userId);

        List<RecommendationResponseDTO> response = new ArrayList<>();
        for (Recommendation rec : candidates) {
            Recommendation saved = recommendationRepository.save(rec);
            response.add(mapToDTO(saved));
        }
        return response;
    }

    private RecommendationResponseDTO mapToDTO(Recommendation rec) {
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
}