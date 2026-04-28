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
import com.infy.entity.Challenge;
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
        // Validate creator exists and is a MANAGER
        Optional<User> optional = userRepository.findById(requestDTO.getCreatedBy());
        User manager = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (!manager.getRole().equals(Role.MANAGER)) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER");
        }

        // Validate end date is after start date
        if (!requestDTO.getEndDate().isAfter(requestDTO.getStartDate())) {
            throw new WellnessTrackerException("Service.INVALID_CHALLENGE_DATES");
        }

        // Enforce visibility vs department consistency
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
        // Status set accurately from dates at creation time
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

    @Override
    @Transactional(readOnly = false)
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId)
            throws WellnessTrackerException {
        // Validate user exists and is a MANAGER
        Optional<User> optional = userRepository.findById(managerId);
        User manager = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (!manager.getRole().equals(Role.MANAGER)) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER");
        }

        // Sync DB statuses before reading so response reflects today's reality
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
        // Sync before read so status in response is always accurate
        statusSyncService.syncStatuses();

        // Validate challenge exists
        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        // Validate requesting user exists and fetch their department
        Optional<User> userOptional = userRepository.findById(requestingUserId);
        User requestingUser = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // Enforce visibility — DEPARTMENT challenges are only visible to:
        //   1. Users in the same department
        //   2. Existing participants (joined before department changed, edge case)
        //   3. The manager who created it
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
    
    // US 07 - Return featured, non-expired challenges visible to the user's department
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
 
        // Sync DB statuses before reading so status field in response is accurate
        statusSyncService.syncStatuses();
 
        List<Challenge> featured = challengeRepository.findFeaturedChallengesForDepartment(
                LocalDate.now(), user.getDepartment().getDepartmentId());
 
        if (featured.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_FEATURED_CHALLENGES_FOUND");
        }
 
        // Batch-fetch alreadyJoined status for all featured challenges in one query
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
