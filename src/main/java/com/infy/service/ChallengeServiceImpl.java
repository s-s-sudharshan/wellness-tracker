package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public Integer createChallenge(ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {

        Optional<User> optional = userRepository.findById(requestDTO.getCreatedBy());
        User creator = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // MANAGER: can create any challenge (COMPANY_WIDE or DEPARTMENT, any dept)
        // HR: can create COMPANY_WIDE challenges or DEPARTMENT challenges scoped to
        //     their own department only. Cannot create cross-dept challenges.
        // EMPLOYEE: cannot create challenges.
        if (Role.EMPLOYEE.equals(creator.getRole())) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER_OR_HR");
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

        // HR additional constraint: DEPARTMENT challenges must be scoped to HR's own dept
        if (Role.HR.equals(creator.getRole())
                && requestDTO.getVisibilityType() == VisibilityType.DEPARTMENT) {
            Integer creatorDeptId = creator.getDepartment() != null
                    ? creator.getDepartment().getDepartmentId() : null;
            if (!requestDTO.getDepartmentId().equals(creatorDeptId)) {
                throw new WellnessTrackerException("Service.HR_CHALLENGE_DEPT_MISMATCH");
            }
        }

        Challenge challenge = new Challenge();
        challenge.setCreatedBy(creator);
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
    
	@Override
	public void deleteChallenge(Integer challengeId, Integer requestingUserId) throws WellnessTrackerException {
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

    @Override
    @Transactional(readOnly = false)
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId)
            throws WellnessTrackerException {

        Optional<User> optional = userRepository.findById(managerId);
        User user = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // Both MANAGER and HR can view their own created challenges
        if (Role.EMPLOYEE.equals(user.getRole())) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER_OR_HR");
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

        // Visibility check for DEPARTMENT challenges:
        // MANAGER/EMPLOYEE: same-dept OR creator OR existing participant
        // HR: same-dept (HR dept) OR creator — HR can join their own dept challenges
        if (challenge.getVisibilityType().equals(VisibilityType.DEPARTMENT)) {
            Integer challengeDeptId = challenge.getDepartment() != null
                    ? challenge.getDepartment().getDepartmentId() : null;
            Integer userDeptId = requestingUser.getDepartment() != null
                    ? requestingUser.getDepartment().getDepartmentId() : null;

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

    @Override
    @Transactional(readOnly = false)
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges(Integer userId)
            throws WellnessTrackerException {

        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // HR can now see featured challenges (they can join HR dept + COMPANY_WIDE)
        // EMPLOYEE block removed — all roles can view featured challenges now
        statusSyncService.syncStatuses();

        List<Challenge> featured = challengeRepository.findFeaturedChallengesForDepartment(
                LocalDate.now(), user.getDepartment().getDepartmentId());

        if (featured.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_FEATURED_CHALLENGES_FOUND");
        }

        List<Integer> challengeIds = new ArrayList<>();
        for (Challenge c : featured) {
            challengeIds.add(c.getChallengeId());
        }

        List<Integer> joinedIds = participantRepository
                .findJoinedChallengeIdsByUser(userId, challengeIds);

        List<ActiveChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : featured) {
            ActiveChallengeResponseDTO dto = mapToActiveDTO(c);
            dto.setAlreadyJoined(joinedIds.contains(c.getChallengeId()));
            responseList.add(dto);
        }

        return responseList;
    }

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
        dto.setStatus(c.getStatus());
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
