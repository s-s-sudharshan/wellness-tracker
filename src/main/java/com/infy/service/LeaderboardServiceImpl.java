package com.infy.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.LeaderboardEntryDTO;
import com.infy.dto.LeaderboardResponseDTO;
import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.UserRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class LeaderboardServiceImpl implements LeaderboardService {

    @Autowired
    public ChallengeRepository challengeRepository;

    @Autowired
    public ChallengeParticipantRepository participantRepository;

    @Autowired
    public ActivityLogRepository activityLogRepository;

    @Autowired
    public UserRepository userRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // syncStatuses() removed — handled by scheduled job at midnight IST.
    // requestingUserId derived from JWT — not supplied by client.
    @Override
    public LeaderboardResponseDTO getLeaderboard(Integer challengeId)
            throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();
        Integer requestingUserId = caller.getUserId();

        Optional<Challenge> challengeOptional = challengeRepository.findById(challengeId);
        Challenge challenge = challengeOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        // DEPARTMENT challenge: same-dept OR creator OR existing participant
        if (challenge.getVisibilityType().equals(VisibilityType.DEPARTMENT)) {
            Integer challengeDeptId = challenge.getDepartment() != null
                    ? challenge.getDepartment().getDepartmentId() : null;
            Integer userDeptId = caller.getDepartment() != null
                    ? caller.getDepartment().getDepartmentId() : null;

            boolean inSameDepartment = challengeDeptId != null
                    && challengeDeptId.equals(userDeptId);
            boolean isCreator = challenge.getCreatedBy().getUserId().equals(requestingUserId);
            boolean isParticipant = participantRepository
                    .findByChallenge_ChallengeIdAndUser_UserId(challengeId, requestingUserId)
                    .isPresent();

            if (!inSameDepartment && !isParticipant && !isCreator) {
                throw new WellnessTrackerException("Service.LEADERBOARD_ACCESS_DENIED");
            }
        }

        List<ChallengeParticipant> participants = participantRepository
                .findByChallenge_ChallengeIdOrderByJoinedAtAsc(challengeId);

        if (participants.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_PARTICIPANTS_FOUND");
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = challenge.getStartDate();
        LocalDate toDate = challenge.getEndDate().isAfter(today)
                ? today : challenge.getEndDate();

        if (toDate.isBefore(fromDate)) {
            toDate = fromDate;
        }

        List<Integer> participantUserIds = new ArrayList<>();
        for (ChallengeParticipant p : participants) {
            participantUserIds.add(p.getUser().getUserId());
        }

        List<Object[]> allActuals = activityLogRepository
                .findActualsByUsersAndDateRangeAndType(
                        participantUserIds,
                        challenge.getMetricType(),
                        fromDate,
                        toDate);

        Map<Integer, Double> actualByUser = new HashMap<>();
        for (Object[] row : allActuals) {
            Integer userId = ((Number) row[0]).intValue();
            Double value   = ((Number) row[1]).doubleValue();
            actualByUser.put(userId, value);
        }

        String unit = resolveUnit(challenge.getMetricType());

        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        for (ChallengeParticipant p : participants) {
            Integer userId     = p.getUser().getUserId();
            double actualValue = actualByUser.getOrDefault(userId, 0.0);
            int progressPct    = calcPct(actualValue, challenge.getGoalValue());

            LeaderboardEntryDTO entry = new LeaderboardEntryDTO();
            entry.setUserId(userId);
            entry.setParticipantName(
                    p.getUser().getFirstName() + " " + p.getUser().getLastName());
            entry.setActualValue(actualValue);
            entry.setUnit(unit);
            entry.setProgressPct(progressPct);
            entry.setIsCurrentUser(userId.equals(requestingUserId));
            entries.add(entry);
        }

        // Sort by actualValue descending, name ascending for deterministic tie order
        entries.sort((a, b) -> {
            int cmp = Double.compare(b.getActualValue(), a.getActualValue());
            if (cmp != 0) return cmp;
            return a.getParticipantName().compareTo(b.getParticipantName());
        });

        // Standard competition ranking
        int rank = 1;
        for (int i = 0; i < entries.size(); i++) {
            if (i == 0) {
                entries.get(i).setRank(rank);
            } else {
                double prev = entries.get(i - 1).getActualValue();
                double curr = entries.get(i).getActualValue();
                if (Double.compare(prev, curr) != 0) {
                    rank = i + 1;
                }
                entries.get(i).setRank(rank);
            }
        }

        Integer currentUserRank        = null;
        Double  currentUserValue       = 0.0;
        Integer currentUserProgressPct = 0;

        for (LeaderboardEntryDTO entry : entries) {
            if (entry.getIsCurrentUser()) {
                currentUserRank        = entry.getRank();
                currentUserValue       = entry.getActualValue();
                currentUserProgressPct = entry.getProgressPct();
                break;
            }
        }

        long rawDaysRemaining = ChronoUnit.DAYS.between(today, challenge.getEndDate());
        int daysRemaining = (int) Math.max(0, rawDaysRemaining);

        LeaderboardResponseDTO response = new LeaderboardResponseDTO();
        response.setChallengeId(challenge.getChallengeId());
        response.setChallengeTitle(challenge.getTitle());
        response.setMetricType(challenge.getMetricType());
        response.setUnit(unit);
        response.setGoalValue(challenge.getGoalValue());
        response.setDifficulty(challenge.getDifficulty());
        response.setStartDate(challenge.getStartDate());
        response.setEndDate(challenge.getEndDate());
        response.setDaysRemaining(daysRemaining);
        response.setChallengeStatus(challenge.getStatus());
        response.setTotalParticipants(participants.size());
        response.setCurrentUserRank(currentUserRank);
        response.setCurrentUserValue(currentUserValue);
        response.setCurrentUserProgressPct(currentUserProgressPct);
        response.setEntries(entries);

        return response;
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

    private Integer calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }
}