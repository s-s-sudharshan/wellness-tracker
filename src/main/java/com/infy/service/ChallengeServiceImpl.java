package com.infy.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.dto.ChallengeUpdateRequestDTO;
import com.infy.entity.Badge;
import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.entity.Department;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.NotificationType;
import com.infy.enums.Role;
import com.infy.enums.UserStatus;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.BadgeRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.DepartmentRepository;
import com.infy.repository.UserRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private static final Log LOGGER = LogFactory.getLog(ChallengeServiceImpl.class);

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // US 13 - Manager or HR creates a new challenge.
    // Creator identity derived entirely from JWT — createdBy removed from DTO.
    // @PreAuthorize("hasRole('MANAGER') or hasRole('HR')") enforced on interface.
    @Override
    public Integer createChallenge(ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();

        // Defence-in-depth: @PreAuthorize already enforced MANAGER or HR.
        if (Role.EMPLOYEE.equals(caller.getRole())) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER_OR_HR");
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

        if (requestDTO.getVisibilityType() == VisibilityType.DEPARTMENT) {
            Integer callerDeptId = caller.getDepartment() != null
                    ? caller.getDepartment().getDepartmentId() : null;
            if (!requestDTO.getDepartmentId().equals(callerDeptId)) {
                throw new WellnessTrackerException("Service.HR_CHALLENGE_DEPT_MISMATCH");
            }
        }

        if (!requestDTO.getEndDate().isAfter(requestDTO.getStartDate())) {
            throw new WellnessTrackerException("Service.INVALID_CHALLENGE_DATES");
        }

        Challenge challenge = new Challenge();
        challenge.setCreatedBy(caller);
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

        if (requestDTO.getRewardBadgeId() != null) {
            Optional<Badge> badge = badgeRepository.findById(requestDTO.getRewardBadgeId());
            badge.orElseThrow(
                    () -> new WellnessTrackerException("Service.BADGE_NOT_FOUND"));
            challenge.setRewardBadgeId(requestDTO.getRewardBadgeId());
        }

        Challenge saved = challengeRepository.save(challenge);

        broadcastNewChallengeNotification(saved, requestDTO.getVisibilityType(),
                requestDTO.getDepartmentId());

        return saved.getChallengeId();
    }

    private void broadcastNewChallengeNotification(Challenge challenge,
            VisibilityType visibilityType, Integer departmentId) {
        try {
            List<User> recipients;
            if (visibilityType == VisibilityType.COMPANY_WIDE) {
                recipients = userRepository.findByStatus(UserStatus.ACTIVE);
            } else {
                recipients = userRepository.findByDepartment_DepartmentIdAndStatus(
                        departmentId, UserStatus.ACTIVE);
            }

            String title   = "New Challenge: " + challenge.getTitle();
            String message = challenge.getDescription()
                    + " Join now before " + challenge.getEndDate() + ".";

            for (User recipient : recipients) {
                notificationService.createNotification(
                        recipient.getUserId(), NotificationType.CHALLENGE,
                        title, message, null);
            }
        } catch (Exception e) {
            LOGGER.error("Notification broadcast failed for challengeId="
                    + challenge.getChallengeId(), e);
        }
    }

    // US 13 - Edit an UPCOMING challenge.
    // syncStatuses() removed — handled by scheduled job at midnight IST.
    @Override
    public ChallengeResponseDTO updateChallenge(Integer challengeId,
            ChallengeUpdateRequestDTO requestDTO) throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();

        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        if (!challenge.getCreatedBy().getUserId().equals(caller.getUserId())) {
            throw new WellnessTrackerException("Service.CHALLENGE_EDIT_FORBIDDEN");
        }

        if (!challenge.getStatus().equals(ChallengeStatus.UPCOMING)) {
            throw new WellnessTrackerException("Service.CHALLENGE_NOT_EDITABLE");
        }

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
        challenge.setStatus(resolveStatus(challenge.getStartDate(), requestDTO.getEndDate()));

        return mapToDTO(challengeRepository.save(challenge));
    }

    // US 13 - Delete an UPCOMING challenge.
    // syncStatuses() removed — handled by scheduled job at midnight IST.
    @Override
    public void deleteChallenge(Integer challengeId) throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();

        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        if (!challenge.getCreatedBy().getUserId().equals(caller.getUserId())) {
            throw new WellnessTrackerException("Service.CHALLENGE_DELETE_FORBIDDEN");
        }

        if (!challenge.getStatus().equals(ChallengeStatus.UPCOMING)) {
            throw new WellnessTrackerException("Service.CHALLENGE_NOT_DELETABLE");
        }

        List<ChallengeParticipant> participants =
                participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(challengeId);
        if (!participants.isEmpty()) {
            throw new WellnessTrackerException("Service.CHALLENGE_HAS_PARTICIPANTS");
        }

        challengeRepository.deleteById(challengeId);
    }

    // US 13 - Get all challenges created by a manager or HR user.
    // syncStatuses() removed — handled by scheduled job at midnight IST.
    @Override
    @Transactional(readOnly = false)
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId)
            throws WellnessTrackerException {
        Optional<User> optional = userRepository.findById(managerId);
        User manager = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        if (Role.EMPLOYEE.equals(manager.getRole())) {
            throw new WellnessTrackerException("Service.NOT_A_MANAGER_OR_HR");
        }

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

    // US 13 / 03 - Get a single challenge by ID (visibility-guarded).
    // syncStatuses() removed — handled by scheduled job at midnight IST.
    @Override
    @Transactional(readOnly = false)
    public ChallengeResponseDTO getChallengeById(Integer challengeId)
            throws WellnessTrackerException {

        User caller = authenticatedUserResolver.resolveCurrentUser();

        Optional<Challenge> optional = challengeRepository.findById(challengeId);
        Challenge challenge = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        if (challenge.getVisibilityType().equals(VisibilityType.DEPARTMENT)) {
            Integer challengeDeptId = challenge.getDepartment() != null
                    ? challenge.getDepartment().getDepartmentId() : null;
            Integer userDeptId = caller.getDepartment() != null
                    ? caller.getDepartment().getDepartmentId() : null;

            boolean inSameDepartment = challengeDeptId != null
                    && challengeDeptId.equals(userDeptId);
            boolean isCreator = challenge.getCreatedBy().getUserId()
                    .equals(caller.getUserId());
            boolean isParticipant = participantRepository
                    .findByChallenge_ChallengeIdAndUser_UserId(
                            challengeId, caller.getUserId())
                    .isPresent();

            if (!inSameDepartment && !isCreator && !isParticipant) {
                throw new WellnessTrackerException("Service.CHALLENGE_ACCESS_DENIED");
            }
        }

        return mapToDTO(challenge);
    }

    // US 07 - Featured challenges visible to the JWT caller's department.
    // syncStatuses() removed — handled by scheduled job at midnight IST.
    @Override
    @Transactional(readOnly = false)
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges()
            throws WellnessTrackerException {
        User caller = authenticatedUserResolver.resolveCurrentUser();

        if (caller.getDepartment() == null) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        List<Challenge> featured = challengeRepository.findFeaturedChallengesForDepartment(
                today, caller.getDepartment().getDepartmentId());

        if (featured.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> challengeIds = new ArrayList<>();
        for (Challenge c : featured) {
            challengeIds.add(c.getChallengeId());
        }

        Set<Integer> joinedIdSet = new HashSet<>(
                participantRepository.findJoinedChallengeIdsByUser(
                        caller.getUserId(), challengeIds));

        List<ActiveChallengeResponseDTO> responseList = new ArrayList<>();
        for (Challenge c : featured) {
            ActiveChallengeResponseDTO dto = mapToActiveDTO(c, today);
            dto.setAlreadyJoined(joinedIdSet.contains(c.getChallengeId()));
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
        if (c.getRewardBadgeId() != null) {
            dto.setRewardBadgeId(c.getRewardBadgeId());
        }
        return dto;
    }

    private ActiveChallengeResponseDTO mapToActiveDTO(Challenge c, LocalDate today) {
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