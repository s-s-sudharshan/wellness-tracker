package com.infy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.infy.dto.LeaderboardResponseDTO;
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

// US 04 — Service-level tests for leaderboard ranking and access control
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTests {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeParticipantRepository participantRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChallengeStatusSyncService statusSyncService;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    private Department dept(Integer id, String name) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setDepartmentName(name);
        return d;
    }

    private User user(Integer id, String first, String last, Integer deptId, Role role) {
        User u = new User();
        u.setUserId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setRole(role);
        u.setDepartment(dept(deptId, "Dept" + deptId));
        return u;
    }

    private Challenge challenge(Integer id, VisibilityType vis, Integer deptId, ChallengeStatus status) {
        Challenge c = new Challenge();
        c.setChallengeId(id);
        c.setTitle("Test Challenge");
        c.setMetricType(ActivityType.STEPS);
        c.setGoalValue(10000.0);
        c.setDifficulty(Difficulty.MEDIUM);
        c.setStartDate(LocalDate.now().minusDays(3));
        c.setEndDate(LocalDate.now().plusDays(4));
        c.setStatus(status);
        c.setVisibilityType(vis);
        if (deptId != null) c.setDepartment(dept(deptId, "Engineering"));
        User creator = user(1, "Sarah", "Connor", 1, Role.MANAGER);
        c.setCreatedBy(creator);
        return c;
    }

    private ChallengeParticipant participant(Challenge c, User u) {
        ChallengeParticipant p = new ChallengeParticipant();
        p.setChallenge(c);
        p.setUser(u);
        return p;
    }

    // --- Access control ---

    @Test
    public void testGetLeaderboard_CrossDeptDepartmentChallenge_ShouldThrow() {
        Challenge c = challenge(1, VisibilityType.DEPARTMENT, 1, ChallengeStatus.ACTIVE);
        User requester = user(4, "Mike", "Jones", 2, Role.EMPLOYEE); // dept 2, challenge is dept 1

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(4)).thenReturn(Optional.of(requester));
        Mockito.when(participantRepository.findByChallenge_ChallengeIdAndUser_UserId(1, 4))
                .thenReturn(Optional.empty());

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> leaderboardService.getLeaderboard(1, 4));
        assertEquals("Service.LEADERBOARD_ACCESS_DENIED", ex.getMessage());
    }

    @Test
    public void testGetLeaderboard_SameDeptDepartmentChallenge_ShouldSucceed()
            throws WellnessTrackerException {
        Challenge c = challenge(1, VisibilityType.DEPARTMENT, 1, ChallengeStatus.ACTIVE);
        User requester = user(2, "John", "Doe", 1, Role.EMPLOYEE); // same dept

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(requester));

        List<ChallengeParticipant> participants = Arrays.asList(
                participant(c, requester)
        );
        Mockito.when(participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(1))
                .thenReturn(participants);
        Mockito.when(activityLogRepository.findActualsByUsersAndDateRangeAndType(
                Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Collections.emptyList());

        LeaderboardResponseDTO result = leaderboardService.getLeaderboard(1, 2);
        assertNotNull(result);
        assertEquals(1, result.getTotalParticipants());
    }

    // --- Ranking logic ---

    @Test
    public void testGetLeaderboard_TwoParticipants_CorrectRanking()
            throws WellnessTrackerException {
        Challenge c = challenge(1, VisibilityType.COMPANY_WIDE, null, ChallengeStatus.ACTIVE);
        User userA = user(2, "John", "Doe", 1, Role.EMPLOYEE);
        User userB = user(3, "Jane", "Smith", 1, Role.EMPLOYEE);

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(userA));
        Mockito.when(participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(1))
                .thenReturn(Arrays.asList(participant(c, userA), participant(c, userB)));

        // userB has more steps than userA
        List<Object[]> actuals = Arrays.asList(
                new Object[]{2, 5000.0},
                new Object[]{3, 8000.0}
        );
        Mockito.when(activityLogRepository.findActualsByUsersAndDateRangeAndType(
                Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(actuals);

        LeaderboardResponseDTO result = leaderboardService.getLeaderboard(1, 2);

        assertEquals(2, result.getTotalParticipants());
        // Rank 1 = Jane Smith (8000), Rank 2 = John Doe (5000)
        assertEquals("Jane Smith", result.getEntries().get(0).getParticipantName());
        assertEquals(1, result.getEntries().get(0).getRank());
        assertEquals("John Doe", result.getEntries().get(1).getParticipantName());
        assertEquals(2, result.getEntries().get(1).getRank());
        // Requesting user (John, userId=2) is rank 2
        assertEquals(2, result.getCurrentUserRank());
    }

    @Test
    public void testGetLeaderboard_TiedParticipants_SameRank()
            throws WellnessTrackerException {
        Challenge c = challenge(1, VisibilityType.COMPANY_WIDE, null, ChallengeStatus.ACTIVE);
        User userA = user(2, "John", "Doe", 1, Role.EMPLOYEE);
        User userB = user(3, "Jane", "Smith", 1, Role.EMPLOYEE);

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(userA));
        Mockito.when(participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(1))
                .thenReturn(Arrays.asList(participant(c, userA), participant(c, userB)));

        // Both have the same actuals — tied at rank 1
        List<Object[]> actuals = Arrays.asList(
                new Object[]{2, 7000.0},
                new Object[]{3, 7000.0}
        );
        Mockito.when(activityLogRepository.findActualsByUsersAndDateRangeAndType(
                Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(actuals);

        LeaderboardResponseDTO result = leaderboardService.getLeaderboard(1, 2);

        assertEquals(1, result.getEntries().get(0).getRank());
        assertEquals(1, result.getEntries().get(1).getRank());
    }

    @Test
    public void testGetLeaderboard_DaysRemainingNeverNegative() throws WellnessTrackerException {
        Challenge c = challenge(1, VisibilityType.COMPANY_WIDE, null, ChallengeStatus.COMPLETED);
        // Set end date in the past to simulate a completed challenge
        c.setEndDate(LocalDate.now().minusDays(5));
        User requester = user(2, "John", "Doe", 1, Role.EMPLOYEE);

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(requester));
        Mockito.when(participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(1))
                .thenReturn(Arrays.asList(participant(c, requester)));
        Mockito.when(activityLogRepository.findActualsByUsersAndDateRangeAndType(
                Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Collections.emptyList());

        LeaderboardResponseDTO result = leaderboardService.getLeaderboard(1, 2);
        assertTrue(result.getDaysRemaining() >= 0,
                "daysRemaining must never be negative for completed challenges");
        assertEquals(0, result.getDaysRemaining());
    }

    @Test
    public void testGetLeaderboard_NoParticipants_ShouldThrow() {
        Challenge c = challenge(1, VisibilityType.COMPANY_WIDE, null, ChallengeStatus.ACTIVE);
        User requester = user(2, "John", "Doe", 1, Role.EMPLOYEE);

        Mockito.when(challengeRepository.findById(1)).thenReturn(Optional.of(c));
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(requester));
        Mockito.when(participantRepository.findByChallenge_ChallengeIdOrderByJoinedAtAsc(1))
                .thenReturn(Collections.emptyList());

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> leaderboardService.getLeaderboard(1, 2));
        assertEquals("Service.NO_PARTICIPANTS_FOUND", ex.getMessage());
    }

    @Test
    public void testGetLeaderboard_ChallengeNotFound_ShouldThrow() {
        Mockito.when(challengeRepository.findById(999)).thenReturn(Optional.empty());

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> leaderboardService.getLeaderboard(999, 2));
        assertEquals("Service.CHALLENGE_NOT_FOUND", ex.getMessage());
    }
}