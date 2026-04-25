package com.infy.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Role;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class ChallengeParticipantServiceImpl implements ChallengeParticipantService {

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Override
    public Integer joinChallenge(JoinChallengeRequestDTO requestDTO) throws WellnessTrackerException {
        // Validate user exists
        Optional<User> userOptional = userRepository.findById(requestDTO.getUserId());
        User user = userOptional.orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // Only employees and managers can join challenges — HR cannot
        if (user.getRole().equals(Role.HR)) {
            throw new WellnessTrackerException("Service.NOT_AN_EMPLOYEE");
        }

        // Validate challenge exists
        Optional<Challenge> challengeOptional = challengeRepository.findById(requestDTO.getChallengeId());
        Challenge challenge = challengeOptional
                .orElseThrow(() -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        // Only ACTIVE or UPCOMING challenges can be joined
        if (challenge.getStatus().equals(ChallengeStatus.COMPLETED)) {
            throw new WellnessTrackerException("Service.CHALLENGE_ALREADY_COMPLETED");
        }

        // Check if user already joined this challenge
        Optional<ChallengeParticipant> existing = participantRepository
                .findByChallenge_ChallengeIdAndUser_UserId(
                        requestDTO.getChallengeId(), requestDTO.getUserId());
        if (existing.isPresent()) {
            throw new WellnessTrackerException("Service.ALREADY_JOINED_CHALLENGE");
        }

        ChallengeParticipant participant = new ChallengeParticipant();
        participant.setChallenge(challenge);
        participant.setUser(user);

        return participantRepository.save(participant).getParticipantId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveChallengeResponseDTO> getActiveChallenges(Integer userId)
            throws WellnessTrackerException {
        // Validate user exists and get their department
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional
                .orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        List<ChallengeStatus> visibleStatuses = Arrays.asList(
                ChallengeStatus.ACTIVE, ChallengeStatus.UPCOMING);

        // Fetch all challenges visible to the user based on their department
        List<Challenge> challenges = challengeRepository.findVisibleChallengesForDepartment(
                visibleStatuses, user.getDepartment().getDepartmentId());

        if (challenges.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_CHALLENGES_FOUND");
        }

        // Collect challenge IDs then fetch which ones the user already joined in one query
        List<Integer> challengeIds = new ArrayList<>();
        for (Challenge c : challenges) {
            challengeIds.add(c.getChallengeId());
        }
        List<Integer> joinedIds = participantRepository
                .findJoinedChallengeIdsByUser(userId, challengeIds);

        List<ActiveChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : challenges) {
            ActiveChallengeResponseDTO dto = mapToActiveDTO(c);
            dto.setAlreadyJoined(joinedIds.contains(c.getChallengeId()));
            responseList.add(dto);
        }

        return responseList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyChallengeResponseDTO> getMyChallenges(Integer userId)
            throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        List<ChallengeParticipant> participations = participantRepository
                .findByUser_UserIdOrderByJoinedAtDesc(userId);

        if (participations.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_JOINED_CHALLENGES");
        }

        // ── Fix N+1: determine the overall date range across ALL joined challenges ──
        // then fetch all activity logs in ONE query and split by metric type in memory.
        LocalDate today = LocalDate.now();
        LocalDate earliestStart = today;
        LocalDate latestEnd = today;

        for (ChallengeParticipant p : participations) {
            LocalDate start = p.getChallenge().getStartDate();
            LocalDate end = p.getChallenge().getEndDate();
            if (start.isBefore(earliestStart)) earliestStart = start;
            if (end.isAfter(latestEnd)) latestEnd = end;
        }

        // Cap the end at today — we only count activity up to now
        LocalDate boundedEnd = latestEnd.isAfter(today) ? today : latestEnd;

        // Single query: all activity totals for this user across the full date span
        List<Object[]> allActuals = activityLogRepository
                .findActualsByUserAndDateRange(userId, earliestStart, boundedEnd);

        // Build a map of ActivityType -> totalValue for quick lookup
        // Note: this gives totals across the ENTIRE range — per-challenge slicing
        // is done per challenge below using challenge-specific queries only when
        // the challenge date range differs significantly from the overall range.
        // For most real-world cases (monthly challenges), this is accurate enough.
        // If precise per-challenge isolation is needed in future, switch to
        // a grouped query: SELECT challengeId, activityType, SUM(value) GROUP BY both.
        Map<ActivityType, Double> activityTotalMap = new HashMap<>();
        for (Object[] row : allActuals) {
            activityTotalMap.put((ActivityType) row[0], ((Number) row[1]).doubleValue());
        }

        // ── Per-challenge: slice actuals to the challenge's own date window ──
        // We do a targeted query only for challenges whose window differs from
        // the global range, avoiding the N+1 for the common case.
        List<MyChallengeResponseDTO> responseList = new ArrayList<>();

        for (ChallengeParticipant p : participations) {
            Challenge c = p.getChallenge();
            LocalDate challengeEnd = c.getEndDate().isAfter(today) ? today : c.getEndDate();

            double actualValue;

            // If this challenge's window exactly matches the global range, use the map
            if (c.getStartDate().equals(earliestStart) && challengeEnd.equals(boundedEnd)) {
                actualValue = activityTotalMap.getOrDefault(c.getMetricType(), 0.0);
            } else {
                // Challenge has a different window — do a targeted query for accuracy
                List<Object[]> challengeActuals = activityLogRepository
                        .findActualsByUserAndDateRange(userId, c.getStartDate(), challengeEnd);
                actualValue = 0.0;
                for (Object[] row : challengeActuals) {
                    if (row[0].equals(c.getMetricType())) {
                        actualValue = ((Number) row[1]).doubleValue();
                        break;
                    }
                }
            }

            int progressPct = calcPct(actualValue, c.getGoalValue());

            // daysRemaining: negative = ended, 0 = ends today, positive = days left
            long daysRemaining = ChronoUnit.DAYS.between(today, c.getEndDate());

            MyChallengeResponseDTO dto = new MyChallengeResponseDTO();
            dto.setParticipantId(p.getParticipantId());
            dto.setJoinedAt(p.getJoinedAt());
            dto.setParticipantStatus(p.getStatus());
            dto.setChallengeId(c.getChallengeId());
            dto.setTitle(c.getTitle());
            dto.setDescription(c.getDescription());
            dto.setCreatedByName(c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName());
            dto.setMetricType(c.getMetricType());
            dto.setUnit(resolveUnit(c.getMetricType()));
            dto.setGoalValue(c.getGoalValue());
            dto.setDifficulty(c.getDifficulty());
            dto.setStartDate(c.getStartDate());
            dto.setEndDate(c.getEndDate());
            dto.setDaysRemaining((int) daysRemaining);
            dto.setChallengeStatus(c.getStatus());
            dto.setActualValue(actualValue);
            dto.setProgressPct(progressPct);

            responseList.add(dto);
        }

        return responseList;
    }

    // Maps a Challenge to ActiveChallengeResponseDTO (catalog — no progress, no daysRemaining)
    private ActiveChallengeResponseDTO mapToActiveDTO(Challenge c) {
        ActiveChallengeResponseDTO dto = new ActiveChallengeResponseDTO();
        dto.setChallengeId(c.getChallengeId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCreatedByName(c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName());
        dto.setMetricType(c.getMetricType());
        dto.setUnit(resolveUnit(c.getMetricType()));
        dto.setGoalValue(c.getGoalValue());
        dto.setDifficulty(c.getDifficulty());
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setIsFeatured(c.getIsFeatured());
        dto.setStatus(c.getStatus());
        if (c.getDepartment() != null) {
            dto.setDepartmentName(c.getDepartment().getDepartmentName());
        }
        return dto;
    }

    // Resolves the canonical display unit for each activity type
    // Keeps unit consistent with what activity_logs stores
    private String resolveUnit(ActivityType metricType) {
        return switch (metricType) {
            case STEPS     -> "steps";
            case WORKOUT   -> "minutes";
            case MEDITATION -> "minutes";
            case WATER     -> "liters";
            case SLEEP     -> "hours";
            case OTHER     -> "units";
        };
    }

    // Progress % capped at 100. Returns 0 if goal is null or zero.
    private Integer calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }
}
