package com.infy.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.infy.enums.VisibilityType;
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

    @Autowired
    private ChallengeStatusSyncService statusSyncService;

    // US 03 - Join challenge
    @Override
    public Integer joinChallenge(JoinChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {

        Optional<User> userOptional = userRepository.findById(requestDTO.getUserId());
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        Optional<Challenge> challengeOptional = challengeRepository.findById(
                requestDTO.getChallengeId());
        Challenge challenge = challengeOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        // Date-based guard catches stale status where endDate has passed
        // but DB status column was not yet updated to COMPLETED
        if (LocalDate.now().isAfter(challenge.getEndDate())
                || challenge.getStatus().equals(ChallengeStatus.COMPLETED)) {
            throw new WellnessTrackerException("Service.CHALLENGE_ALREADY_COMPLETED");
        }

        // Enforce department visibility on join — policy:
        //   same-department OR creator
        if (challenge.getVisibilityType().equals(VisibilityType.DEPARTMENT)) {
            Integer challengeDeptId = challenge.getDepartment() != null
                    ? challenge.getDepartment().getDepartmentId() : null;
            Integer userDeptId = user.getDepartment() != null
                    ? user.getDepartment().getDepartmentId() : null;

            boolean inSameDepartment = challengeDeptId != null
                    && challengeDeptId.equals(userDeptId);
            boolean isCreator = challenge.getCreatedBy().getUserId()
                    .equals(requestDTO.getUserId());

            if (!inSameDepartment && !isCreator) {
                throw new WellnessTrackerException("Service.CHALLENGE_ACCESS_DENIED");
            }
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

    // US 03 - Active/upcoming challenge catalog for a user.
    @Override
    @Transactional(readOnly = false)
    public List<ActiveChallengeResponseDTO> getActiveChallenges(Integer userId)
            throws WellnessTrackerException {
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        statusSyncService.syncStatuses();

        // Date-based query — not dependent on stale status column
        List<Challenge> challenges = challengeRepository.findVisibleChallengesForDepartment(
                LocalDate.now(), user.getDepartment().getDepartmentId());

        if (challenges.isEmpty()) {
        	return new ArrayList<>();
        }

        List<Integer> challengeIds = new ArrayList<>();
        for (Challenge c : challenges) {
            challengeIds.add(c.getChallengeId());
        }

        Set<Integer> joinedIdSet = new HashSet<>(
                participantRepository.findJoinedChallengeIdsByUser(userId, challengeIds));
 
        List<ActiveChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : challenges) {
            ActiveChallengeResponseDTO dto = mapToActiveDTO(c);
            dto.setAlreadyJoined(joinedIdSet.contains(c.getChallengeId()));
            responseList.add(dto);
        }

        return responseList;
    }

    // US 03 - All challenges the user has joined, with live progress
    @Override
    @Transactional(readOnly = false)
    public List<MyChallengeResponseDTO> getMyChallenges(Integer userId)
            throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        statusSyncService.syncStatuses();

        List<ChallengeParticipant> participations = participantRepository
                .findByUser_UserIdOrderByJoinedAtDesc(userId);

        if (participations.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_JOINED_CHALLENGES");
        }

        LocalDate today = LocalDate.now();
        LocalDate earliestStart = today;
        LocalDate latestEnd = today;

        for (ChallengeParticipant p : participations) {
            LocalDate start = p.getChallenge().getStartDate();
            LocalDate end = p.getChallenge().getEndDate();
            if (start.isBefore(earliestStart)) earliestStart = start;
            if (end.isAfter(latestEnd)) latestEnd = end;
        }

        LocalDate boundedEnd = latestEnd.isAfter(today) ? today : latestEnd;

        List<Object[]> allActuals = activityLogRepository
                .findActualsByUserAndDateRange(userId, earliestStart, boundedEnd);

        Map<ActivityType, Double> activityTotalMap = new HashMap<>();
        for (Object[] row : allActuals) {
            activityTotalMap.put((ActivityType) row[0], ((Number) row[1]).doubleValue());
        }

        List<MyChallengeResponseDTO> responseList = new ArrayList<>();

        for (ChallengeParticipant p : participations) {
            Challenge c = p.getChallenge();
            LocalDate challengeEnd = c.getEndDate().isAfter(today) ? today : c.getEndDate();

            double actualValue;
            if (c.getStartDate().equals(earliestStart) && challengeEnd.equals(boundedEnd)) {
                actualValue = activityTotalMap.getOrDefault(c.getMetricType(), 0.0);
            } else {
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

            // Derive status live from dates instead of reading stale DB column
            ChallengeStatus liveStatus = resolveStatus(c.getStartDate(), c.getEndDate());

            // daysRemaining: clamped to 0 for ended challenges (never negative in My Challenges)
            long rawDaysRemaining = ChronoUnit.DAYS.between(today, c.getEndDate());
            int daysRemaining = (int) Math.max(0, rawDaysRemaining);

            MyChallengeResponseDTO dto = new MyChallengeResponseDTO();
            dto.setParticipantId(p.getParticipantId());
            dto.setJoinedAt(p.getJoinedAt());
            dto.setParticipantStatus(p.getStatus());
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
            dto.setChallengeStatus(liveStatus);
            dto.setActualValue(actualValue);
            dto.setProgressPct(progressPct);

            responseList.add(dto);
        }

        return responseList;
    }

    // Derives current status purely from dates — always accurate regardless of DB column
    private ChallengeStatus resolveStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return ChallengeStatus.UPCOMING;
        if (today.isAfter(endDate))    return ChallengeStatus.COMPLETED;
        return ChallengeStatus.ACTIVE;
    }

    private ActiveChallengeResponseDTO mapToActiveDTO(Challenge c) {
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
        dto.setIsFeatured(c.getIsFeatured());
        dto.setStatus(c.getStatus());
        if (c.getDepartment() != null) {
            dto.setDepartmentName(c.getDepartment().getDepartmentName());
        }
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

    private Integer calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }
}
