package com.infy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.entity.User;
import com.infy.entity.WeeklyGoal;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.UserRepository;
import com.infy.repository.WeeklyGoalRepository;

// US 11 — Service-level tests for real business logic
@ExtendWith(MockitoExtension.class)
class WeeklyGoalServiceTests {

    @Mock
    private WeeklyGoalRepository weeklyGoalRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WeeklyGoalServiceImpl weeklyGoalService;

    private LocalDate thisWeekMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate nextWeekMonday() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private User stubUser(Integer userId) {
        User user = new User();
        user.setUserId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        return user;
    }

    // --- saveWeeklyGoal ---

    @Test
    public void testSaveWeeklyGoal_CurrentWeekMonday_ShouldSucceed() throws WellnessTrackerException {
        User user = stubUser(2);
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(user));
        Mockito.when(weeklyGoalRepository.findByUser_UserIdAndWeekStartDate(2, thisWeekMonday()))
                .thenReturn(Optional.empty());
        WeeklyGoal saved = new WeeklyGoal();
        saved.setWeeklyGoalId(1);
        Mockito.when(weeklyGoalRepository.save(Mockito.any(WeeklyGoal.class))).thenReturn(saved);

        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(2);
        dto.setWeekStartDate(thisWeekMonday());
        dto.setStepsGoal(50000.0);

        Integer result = weeklyGoalService.saveWeeklyGoal(dto);
        assertEquals(1, result);
    }

    @Test
    public void testSaveWeeklyGoal_NextWeekMonday_ShouldSucceed() throws WellnessTrackerException {
        User user = stubUser(2);
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(user));
        Mockito.when(weeklyGoalRepository.findByUser_UserIdAndWeekStartDate(2, nextWeekMonday()))
                .thenReturn(Optional.empty());
        WeeklyGoal saved = new WeeklyGoal();
        saved.setWeeklyGoalId(2);
        Mockito.when(weeklyGoalRepository.save(Mockito.any(WeeklyGoal.class))).thenReturn(saved);

        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(2);
        dto.setWeekStartDate(nextWeekMonday());
        dto.setStepsGoal(60000.0);

        Integer result = weeklyGoalService.saveWeeklyGoal(dto);
        assertEquals(2, result);
    }

    @Test
    public void testSaveWeeklyGoal_PastWeek_ShouldThrow() {
        User user = stubUser(2);
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(user));

        LocalDate lastWeekMonday = thisWeekMonday().minusWeeks(1);
        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(2);
        dto.setWeekStartDate(lastWeekMonday);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> weeklyGoalService.saveWeeklyGoal(dto));
        assertEquals("Service.INVALID_WEEK_START_DATE", ex.getMessage());
    }

    @Test
    public void testSaveWeeklyGoal_NonMonday_ShouldThrow() {
        User user = stubUser(2);
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(user));

        // Use next Tuesday — definitely in the future but not a Monday
        LocalDate nextTuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(2);
        dto.setWeekStartDate(nextTuesday);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> weeklyGoalService.saveWeeklyGoal(dto));
        assertEquals("Service.INVALID_WEEK_START_DATE", ex.getMessage());
    }

    @Test
    public void testSaveWeeklyGoal_UserNotFound_ShouldThrow() {
        Mockito.when(userRepository.findById(999)).thenReturn(Optional.empty());

        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(999);
        dto.setWeekStartDate(thisWeekMonday());

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> weeklyGoalService.saveWeeklyGoal(dto));
        assertEquals("Service.USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    public void testSaveWeeklyGoal_UpsertExistingGoal_ShouldUpdate() throws WellnessTrackerException {
        User user = stubUser(2);
        Mockito.when(userRepository.findById(2)).thenReturn(Optional.of(user));

        WeeklyGoal existing = new WeeklyGoal();
        existing.setWeeklyGoalId(5);
        Mockito.when(weeklyGoalRepository.findByUser_UserIdAndWeekStartDate(2, thisWeekMonday()))
                .thenReturn(Optional.of(existing));
        Mockito.when(weeklyGoalRepository.save(Mockito.any(WeeklyGoal.class))).thenReturn(existing);

        WeeklyGoalRequestDTO dto = new WeeklyGoalRequestDTO();
        dto.setUserId(2);
        dto.setWeekStartDate(thisWeekMonday());
        dto.setStepsGoal(70000.0);

        Integer result = weeklyGoalService.saveWeeklyGoal(dto);
        assertEquals(5, result);
    }

    // --- getWeeklyGoal ---

    @Test
    public void testGetWeeklyGoal_UserNotFound_ShouldThrow() {
        Mockito.when(userRepository.existsById(999)).thenReturn(false);

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> weeklyGoalService.getWeeklyGoal(999, thisWeekMonday()));
        assertEquals("Service.USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    public void testGetWeeklyGoal_GoalNotFound_ShouldThrow() {
        Mockito.when(userRepository.existsById(2)).thenReturn(true);
        Mockito.when(weeklyGoalRepository.findByUser_UserIdAndWeekStartDate(2, thisWeekMonday()))
                .thenReturn(Optional.empty());

        WellnessTrackerException ex = assertThrows(WellnessTrackerException.class,
                () -> weeklyGoalService.getWeeklyGoal(2, thisWeekMonday()));
        assertEquals("Service.WEEKLY_GOAL_NOT_FOUND", ex.getMessage());
    }
}