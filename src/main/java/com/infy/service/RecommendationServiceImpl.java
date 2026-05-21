package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.RecommendationResponseDTO;
import com.infy.entity.Challenge;
import com.infy.entity.Recommendation;
import com.infy.entity.User;
import com.infy.entity.WellnessArticle;
import com.infy.enums.ActivityType;
import com.infy.enums.RecommendationStatus;
import com.infy.enums.RecommendationType;
import com.infy.enums.WellnessArticleStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.RecommendationRepository;
import com.infy.repository.WellnessArticleRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    private static final double LOW_WATER_LITERS    = 10.0;
    private static final double LOW_STEPS           = 35000.0;
    private static final double LOW_WORKOUT_MINUTES = 60.0;
    private static final double LOW_SLEEP_HOURS     = 42.0;
    private static final double LOW_MEDITATION_MIN  = 30.0;

    private static final int MIN_RECOMMENDATIONS = 3;
    private static final int MAX_RECOMMENDATIONS = 5;

    @Autowired private ActivityLogRepository          activityLogRepository;
    @Autowired private ChallengeRepository            challengeRepository;
    @Autowired private ChallengeParticipantRepository participantRepository;
    @Autowired private RecommendationRepository       recommendationRepository;
    @Autowired private WellnessArticleRepository      articleRepository;
    @Autowired private AuthenticatedUserResolver      authenticatedUserResolver;

    // syncStatuses() removed — handled by scheduled job at midnight IST.
    // userId derived from JWT — never from a caller-supplied parameter.
    @Override
    public List<RecommendationResponseDTO> getRecommendations()
            throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();
        Integer userId = caller.getUserId();

        LocalDate today   = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        double totalWater = 0, totalSteps = 0, totalWorkout = 0,
               totalSleep = 0, totalMeditation = 0;

        for (Object[] row : activityLogRepository
                .findActualsByUserAndDateRange(userId, weekAgo, today)) {
            ActivityType type  = (ActivityType) row[0];
            double       value = ((Number) row[1]).doubleValue();
            switch (type) {
                case WATER      -> totalWater      = value;
                case STEPS      -> totalSteps      = value;
                case WORKOUT    -> totalWorkout    = value;
                case SLEEP      -> totalSleep      = value;
                case MEDITATION -> totalMeditation = value;
                default         -> { }
            }
        }

        Integer userDeptId = caller.getDepartment() != null
                ? caller.getDepartment().getDepartmentId() : null;

        List<Challenge> visibleChallenges = userDeptId != null
                ? challengeRepository.findVisibleChallengesForDepartment(today, userDeptId)
                : new ArrayList<>();

        List<Integer> visibleIds = new ArrayList<>();
        for (Challenge c : visibleChallenges) {
            visibleIds.add(c.getChallengeId());
        }

        Set<Integer> joinedIds = visibleIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(participantRepository
                        .findJoinedChallengeIdsByUser(userId, visibleIds));

        Map<ActivityType, Double> ratioMap = new HashMap<>();
        if (totalWater      < LOW_WATER_LITERS)    ratioMap.put(ActivityType.WATER,      totalWater      / LOW_WATER_LITERS);
        if (totalSteps      < LOW_STEPS)            ratioMap.put(ActivityType.STEPS,      totalSteps      / LOW_STEPS);
        if (totalWorkout    < LOW_WORKOUT_MINUTES)  ratioMap.put(ActivityType.WORKOUT,    totalWorkout    / LOW_WORKOUT_MINUTES);
        if (totalSleep      < LOW_SLEEP_HOURS)      ratioMap.put(ActivityType.SLEEP,      totalSleep      / LOW_SLEEP_HOURS);
        if (totalMeditation < LOW_MEDITATION_MIN)   ratioMap.put(ActivityType.MEDITATION, totalMeditation / LOW_MEDITATION_MIN);

        List<ActivityType> lowMetrics = new ArrayList<>(ratioMap.keySet());
        lowMetrics.sort((a, b) -> Double.compare(ratioMap.get(a), ratioMap.get(b)));

        Set<Integer> seenChallengeIds = new HashSet<>();
        Set<String>  seenArticleUrls  = new HashSet<>();
        List<Recommendation> results  = new ArrayList<>();

        for (ActivityType metric : lowMetrics) {
            if (results.size() >= MAX_RECOMMENDATIONS) break;

            for (Challenge c : visibleChallenges) {
                if (c.getMetricType().equals(metric)
                        && !joinedIds.contains(c.getChallengeId())
                        && !seenChallengeIds.contains(c.getChallengeId())) {
                    results.add(challengeRec(caller, c, metric));
                    seenChallengeIds.add(c.getChallengeId());
                    break;
                }
            }

            if (results.size() < MAX_RECOMMENDATIONS) {
                addArticleForMetric(results, seenArticleUrls, caller, metric);
            }
        }

        if (results.size() < MIN_RECOMMENDATIONS) {
            List<WellnessArticle> generalArticles = articleRepository
                    .findGeneralPublishedArticles(WellnessArticleStatus.PUBLISHED);
            for (WellnessArticle a : generalArticles) {
                if (results.size() >= MIN_RECOMMENDATIONS) break;
                if (!seenArticleUrls.contains(a.getArticleUrl())) {
                    results.add(articleRec(caller, a.getTitle(), a.getDescription(),
                            a.getArticleUrl()));
                    seenArticleUrls.add(a.getArticleUrl());
                }
            }
        }

        recommendationRepository.deleteActiveByUserId(userId);
        List<RecommendationResponseDTO> response = new ArrayList<>();
        for (Recommendation rec : results) {
            response.add(mapToDTO(recommendationRepository.save(rec)));
        }
        return response;
    }

    private void addArticleForMetric(List<Recommendation> results, Set<String> seenUrls,
            User user, ActivityType metric) {

        if (results.size() >= MAX_RECOMMENDATIONS) return;

        List<WellnessArticle> articles = articleRepository
                .findByRelatedMetricAndStatusOrderByCreatedAtDesc(
                        metric, WellnessArticleStatus.PUBLISHED);

        for (WellnessArticle a : articles) {
            if (!seenUrls.contains(a.getArticleUrl())) {
                results.add(articleRec(user, a.getTitle(), a.getDescription(),
                        a.getArticleUrl()));
                seenUrls.add(a.getArticleUrl());
                return;
            }
        }
    }

    private Recommendation challengeRec(User user, Challenge challenge, ActivityType metric) {
        Recommendation rec = new Recommendation();
        rec.setUser(user);
        rec.setRecommendationType(RecommendationType.CHALLENGE);
        rec.setTitle(challenge.getTitle());
        rec.setDescription(challengeDescription(metric));
        rec.setChallengeId(challenge.getChallengeId());
        rec.setStatus(RecommendationStatus.ACTIVE);
        return rec;
    }

    private Recommendation articleRec(User user, String title,
            String description, String url) {
        Recommendation rec = new Recommendation();
        rec.setUser(user);
        rec.setRecommendationType(RecommendationType.ARTICLE);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setArticleUrl(url);
        rec.setStatus(RecommendationStatus.ACTIVE);
        return rec;
    }

    private String challengeDescription(ActivityType metric) {
        return switch (metric) {
            case WATER      -> "Your water intake this week is below the recommended level. "
                             + "Joining this challenge will help you build a daily hydration habit.";
            case STEPS      -> "You are averaging fewer than 5,000 steps a day this week. "
                             + "This challenge will help you hit a healthy daily step target.";
            case WORKOUT    -> "You have logged less than 60 minutes of workout this week. "
                             + "Even short sessions add up — this challenge is a great motivator.";
            case SLEEP      -> "Log your sleep this week and aim for the recommended target. "
                             + "Consistent rest tracking is the first step to better recovery.";
            case MEDITATION -> "Even 5 minutes of daily mindfulness can reduce stress. "
                             + "Join this challenge to build a daily habit.";
            default         -> "Join this challenge to improve your wellness this week.";
        };
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