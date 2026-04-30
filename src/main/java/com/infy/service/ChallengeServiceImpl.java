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

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.dto.ChallengeUpdateRequestDTO;
import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.entity.Department;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Role;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.DepartmentRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ChallengeStatusSyncService statusSyncService;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    // US 13 - Manager creates a new challenge
    @Override
    public Integer createChallenge(ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Optional<User> optional = userRepository.findById(requestDTO.getCreatedBy());
        User manager = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (!manager.getRole().equals(Role.MANAGER)) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER");
        }

        if (!requestDTO.getEndDate().isAfter(requestDTO.getStartDate())) {
            throw new WellnessTrackerException("Service.INVALID_CHALLENGE_DATES");
        }

        if (requestDTO.getVisibilityType() == VisibilityType.DEPARTMENT
                && requestDTO.getDepartmentId() == null) {
            throw new WellnessTrackerException("Service.DEPARTMENT_REQUIRED_FOR_VISIBILITY");
        }
        if (requestDTO.getVisibilityType() == VisibilityType.COMPANY_WIDE
                && requestDTO.getDepartmentId() != null) {
            throw new WellnessTrackerException(
                    "Service.DEPARTMENT_NOT_ALLOWED_FOR_COMPANY_WIDE");
        }

        Challenge challenge = new Challenge();
        challenge.setCreatedBy(manager);
        challenge.setTitle(requestDTO.getTitle());
        challenge.setDescription(requestDTO.getDescription());
        challenge.setMetricType(requestDTO.getMetricType());
        challenge.setGoalValue(requestDTO.getGoalValue());
        challenge.setDifficulty(requestDTO.getDifficulty());
        challenge.setStartDate(requestDTO.getStartDate());
        challenge.setEndDate(requestDTO.getEndDate());
        challenge.setVisibilityType(requestDTO.getVisibilityType());
        challenge.setIsFeatured(
                requestDTO.getIsFeatured() != null && requestDTO.getIsFeatured());
        challenge.setStatus(resolveStatus(
                requestDTO.getStartDate(), requestDTO.getEndDate()));

        if (requestDTO.getDepartmentId() != null) {
            Optional<Department> dept = departmentRepository.findById(
                    requestDTO.getDepartmentId());
            Department department = dept.orElseThrow(
                    () -> new WellnessTrackerException("Service.DEPARTMENT_NOT_FOUND"));
            challenge.setDepartment(department);
        }

        return challengeRepository.save(challenge).getChallengeId();
    }

    // US 13 - Manager edits an UPCOMING challenge they created.
    @Override
    public ChallengeResponseDTO updateChallenge(Integer challengeId,
            ChallengeUpdateRequestDTO requestDTO) throws WellnessTrackerException {
 
        // Sync first so challenge.getStatus() is always accurate
        statusSyncService.syncStatuses();
 
        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));
 
        // Only the original creator may edit
        if (!challenge.getCreatedBy().getUserId().equals(requestDTO.getRequestingUserId())) {
            throw new WellnessTrackerException("Service.CHALLENGE_EDIT_FORBIDDEN");
        }
 
        // Only UPCOMING challenges are editable
        if (!challenge.getStatus().equals(ChallengeStatus.UPCOMING)) {
            throw new WellnessTrackerException("Service.CHALLENGE_NOT_EDITABLE");
        }
 
        // endDate must remain after the immutable startDate
        if (!requestDTO.getEndDate().isAfter(challenge.getStartDate())) {
            throw new WellnessTrackerException("Service.INVALID_CHALLENGE_DATES");
        }
 
        challenge.setTitle(requestDTO.getTitle());
        challenge.setDescription(requestDTO.getDescription());
        challenge.setGoalValue(requestDTO.getGoalValue());
        challenge.setDifficulty(requestDTO.getDifficulty());
        challenge.setEndDate(requestDTO.getEndDate());
        challenge.setIsFeatured(
                requestDTO.getIsFeatured() != null && requestDTO.getIsFeatured());
 
        // Re-derive status in case the new endDate now falls in the past
        challenge.setStatus(resolveStatus(challenge.getStartDate(), requestDTO.getEndDate()));
 
        return mapToDTO(challengeRepository.save(challenge));
    }
    
    // US 13 - Manager deletes an UPCOMING challenge they created.
    @Override
    public void deleteChallenge(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException {
 
        // Sync before read so status is accurate
        statusSyncService.syncStatuses();
 
        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));
 
        // Only the original creator may delete
        if (!challenge.getCreatedBy().getUserId().equals(requestingUserId)) {
            throw new WellnessTrackerException("Service.CHALLENGE_DELETE_FORBIDDEN");
        }
 
        // Only UPCOMING challenges can be deleted
        if (!challenge.getStatus().equals(ChallengeStatus.UPCOMING)) {
            throw new WellnessTrackerException("Service.CHALLENGE_NOT_DELETABLE");
        }
 
        // Block deletion if participants have joined
        List<ChallengeParticipant> participants =
                participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(challengeId);
        if (!participants.isEmpty()) {
            throw new WellnessTrackerException("Service.CHALLENGE_HAS_PARTICIPANTS");
        }
 
        challengeRepository.deleteById(challengeId);
    }
    
    // US 13 - Get all challenges created by a manager
    @Override
    @Transactional(readOnly = false)
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId)
            throws WellnessTrackerException {
        Optional<User> optional = userRepository.findById(managerId);
        User manager = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (!manager.getRole().equals(Role.MANAGER)) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER");
        }

        statusSyncService.syncStatuses();

        List<Challenge> challenges = challengeRepository
                .findByCreatedBy_UserIdOrderByCreatedAtDesc(managerId);

        if (challenges.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_CHALLENGES_FOUND");
        }

        List<ChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : challenges) {
            responseList.add(mapToDTO(c));
        }
        return responseList;
    }

    // US 13 / 03 - Get a single challenge by ID (visibility-guarded)
    @Override
    @Transactional(readOnly = false)
    public ChallengeResponseDTO getChallengeById(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException {

    	statusSyncService.syncStatuses();

        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        Optional<User> userOptional = userRepository.findById(requestingUserId);
        User requestingUser = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // DEPARTMENT challenges: same-dept OR creator OR existing participant
        if (challenge.getVisibilityType().equals(VisibilityType.DEPARTMENT)) {
            Integer challengeDeptId = challenge.getDepartment() != null
                    ? challenge.getDepartment().getDepartmentId()
                    : null;
            Integer userDeptId = requestingUser.getDepartment() != null
                    ? requestingUser.getDepartment().getDepartmentId()
                    : null;

            boolean inSameDepartment = challengeDeptId != null
                    && challengeDeptId.equals(userDeptId);

            boolean isCreator = challenge.getCreatedBy().getUserId()
                    .equals(requestingUserId);

            boolean isParticipant = participantRepository
                    .findByChallenge_ChallengeIdAndUser_UserId(challengeId, requestingUserId)
                    .isPresent();

            if (!inSameDepartment && !isCreator && !isParticipant) {
                throw new WellnessTrackerException("Service.CHALLENGE_ACCESS_DENIED");
            }
        }

        return mapToDTO(challenge);
    }

    // US 07 - Return featured, non-expired challenges visible to the user's department.
    @Override
    @Transactional(readOnly = false)
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges(Integer userId)
            throws WellnessTrackerException {
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // HR users cannot see the challenge catalog or featured challenges since they cannot join
        if (user.getRole().equals(Role.HR)) {
            throw new WellnessTrackerException("Service.NOT_AN_EMPLOYEE");
        }

        statusSyncService.syncStatuses();

        List<Challenge> featured = challengeRepository.findFeaturedChallengesForDepartment(
                LocalDate.now(), user.getDepartment().getDepartmentId());

        if (featured.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> challengeIds = new ArrayList<>();
        for (Challenge c : featured) {
            challengeIds.add(c.getChallengeId());
        }

        Set<Integer> joinedIdSet = new HashSet<>(
                participantRepository.findJoinedChallengeIdsByUser(userId, challengeIds));

        List<ActiveChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : featured) {
            ActiveChallengeResponseDTO dto = mapToActiveDTO(c);
            dto.setAlreadyJoined(joinedIdSet.contains(c.getChallengeId()));
            responseList.add(dto);
        }

        return responseList;
    }

    // Derives current status purely from dates — used at creation time
    private ChallengeStatus resolveStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return ChallengeStatus.UPCOMING;
        if (today.isAfter(endDate))    return ChallengeStatus.COMPLETED;
        return ChallengeStatus.ACTIVE;
    }

    private ChallengeResponseDTO mapToDTO(Challenge c) {
        ChallengeResponseDTO dto = new ChallengeResponseDTO();
        dto.setChallengeId(c.getChallengeId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCreatedBy(c.getCreatedBy().getUserId());
        dto.setCreatedByName(
                c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName());
        dto.setMetricType(c.getMetricType());
        dto.setGoalValue(c.getGoalValue());
        dto.setDifficulty(c.getDifficulty());
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setVisibilityType(c.getVisibilityType());
        dto.setIsFeatured(c.getIsFeatured());
        dto.setStatus(c.getStatus());   // accurate after syncStatuses()
        dto.setCreatedAt(c.getCreatedAt());
        if (c.getDepartment() != null) {
            dto.setDepartmentId(c.getDepartment().getDepartmentId());
            dto.setDepartmentName(c.getDepartment().getDepartmentName());
        }
        return dto;
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
}
