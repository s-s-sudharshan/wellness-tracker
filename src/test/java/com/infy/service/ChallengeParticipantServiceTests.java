package com.infy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.entity.Department;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.Role;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.UserRepository;

// US 03 — Service-level tests for join challenge business rules
@ExtendWith(MockitoExtension.class)
class ChallengeParticipantServiceTests {

    @Mock
    private ChallengeParticipantRepository participantRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private ChallengeStatusSyncService statusSyncService;

    @InjectMocks
    private ChallengeParticipantServiceImpl participantService;

    private Department dept(Integer id) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setDepartmentName("Engineering");
        return d;
    }

    private User employee(Integer userId, Integer deptId) {
        User u = new User();
        u.setUserId(userId);
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setRole(Role.EMPLOYEE);
        u.setDepartment(dept(deptId));
        return u;
    }

    private User hrUser(Integer userId) {
        User u = new User();
        u.setUserId(userId);
        u.setRole(Role.HR);
        u.setDepartment(dept(3));
        return u;
    }

    private Challenge activeChallenge(Integer id, VisibilityType vis, Integer deptId) {
        Challenge c = new Challenge();
        c.setChallengeId(id);
        c.setTitle("Test");
        c.setMetricType(ActivityType.STEPS);
        c.setGoalValue(10000.0);
        c.setDifficulty(Difficulty.EASY);
        c.setStartDate(LocalDate.now().minusDays(1));
        c.setEndDate(LocalDate.now().plusDays(6));
        c.setStatus(ChallengeStatus.ACTIVE);
        c.setVisibilityType(vis);
        if (deptId != null) c.setDepartment(dept(deptId));
        User creator = new User();
        creator.setUserId(1);
        creator.setFirstName("Sarah");
        creator.setLastName("Connor");
        c.setCreatedBy(creator);
        return c;
    }

    // --- HR cannot join ---

    @Test
    public void testJoinChallenge_HRUser_ShouldThrow() {
        Mockito.when(userRepository.findById(5)).thenReturn(Optional.of(hrUser(5)));

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(5);
        dto.setChallengeId(1);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> participantService.joinChallenge(dto));
        assertEquals("Service.NOT_AN_EMPLOYEE", ex.getMessage());
    }

    // --- Completed challenge cannot be joined ---

    @Test
    public void testJoinChallenge_CompletedChallenge_ShouldThrow() {
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(employee(2, 1)));

        Challenge completed = activeChallenge(1, VisibilityType.COMPANY_WIDE, null);
        completed.setStatus(ChallengeStatus.COMPLETED);
        completed.setEndDate(LocalDate.now().minusDays(1));
        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(completed));

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(2);
        dto.setChallengeId(1);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> participantService.joinChallenge(dto));
        assertEquals("Service.CHALLENGE_ALREADY_COMPLETED", ex.getMessage());
    }

    // --- Department visibility enforced on join ---

    @Test
    public void testJoinChallenge_CrossDeptDepartmentChallenge_ShouldThrow() {
        // User is in dept 2, challenge is DEPARTMENT-scoped to dept 1
        Mockito.when(userRepository.findById(4)).thenReturn(Optional.of(employee(4, 2)));
        Mockito.when(challengeRepository.findById(1))
                .thenReturn(Optional.of(activeChallenge(1, VisibilityType.DEPARTMENT, 1)));

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(4);
        dto.setChallengeId(1);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> participantService.joinChallenge(dto));
        assertEquals("Service.CHALLENGE_ACCESS_DENIED", ex.getMessage());
    }

    @Test
    public void testJoinChallenge_SameDeptDepartmentChallenge_ShouldSucceed()
            throws WellnessTrackerException {
        // User is in dept 1, challenge is DEPARTMENT-scoped to dept 1
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(employee(2, 1)));
        Mockito.when(challengeRepository.findById(1))
                .thenReturn(Optional.of(activeChallenge(1, VisibilityType.DEPARTMENT, 1)));
        Mockito.when(participantRepository
                .findByChallenge_ChallengeIdAndUser_UserId(1, 2))
                .thenReturn(Optional.empty());

        ChallengeParticipant saved = new ChallengeParticipant();
        saved.setParticipantId(10);
        Mockito.when(participantRepository.save(Mockito.any(ChallengeParticipant.class)))
                .thenReturn(saved);

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(2);
        dto.setChallengeId(1);

        Integer participantId = participantService.joinChallenge(dto);
        assertEquals(10, participantId);
    }

    @Test
    public void testJoinChallenge_CompanyWideChallenge_AnyDept_ShouldSucceed()
            throws WellnessTrackerException {
        // User in dept 2 can join a COMPANY_WIDE challenge
        Mockito.when(userRepository.findById(4)).thenReturn(Optional.of(employee(4, 2)));
        Mockito.when(challengeRepository.findById(1))
                .thenReturn(Optional.of(activeChallenge(1, VisibilityType.COMPANY_WIDE, null)));
        Mockito.when(participantRepository
                .findByChallenge_ChallengeIdAndUser_UserId(1, 4))
                .thenReturn(Optional.empty());

        ChallengeParticipant saved = new ChallengeParticipant();
        saved.setParticipantId(11);
        Mockito.when(participantRepository.save(Mockito.any(ChallengeParticipant.class)))
                .thenReturn(saved);

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(4);
        dto.setChallengeId(1);

        Integer participantId = participantService.joinChallenge(dto);
        assertEquals(11, participantId);
    }

    // --- Already joined ---

    @Test
    public void testJoinChallenge_AlreadyJoined_ShouldThrow() {
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(employee(2, 1)));
        Mockito.when(challengeRepository.findById(1))
                .thenReturn(Optional.of(activeChallenge(1, VisibilityType.COMPANY_WIDE, null)));

        ChallengeParticipant existing = new ChallengeParticipant();
        Mockito.when(participantRepository
                .findByChallenge_ChallengeIdAndUser_UserId(1, 2))
                .thenReturn(Optional.of(existing));

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(2);
        dto.setChallengeId(1);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> participantService.joinChallenge(dto));
        assertEquals("Service.ALREADY_JOINED_CHALLENGE", ex.getMessage());
    }

    // --- User not found ---

    @Test
    public void testJoinChallenge_UserNotFound_ShouldThrow() {
        Mockito.when(userRepository.findById(999)).thenReturn(Optional.empty());

        JoinChallengeRequestDTO dto = new JoinChallengeRequestDTO();
        dto.setUserId(999);
        dto.setChallengeId(1);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> participantService.joinChallenge(dto));
        assertEquals("Service.USER_NOT_FOUND", ex.getMessage());
    }
}