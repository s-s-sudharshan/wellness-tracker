package com.infy;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.infy.api.ActivityLogAPI;
import com.infy.dto.ActivityTrendPointDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;
import com.infy.utility.ExceptionControllerAdvice;

// US 02 — Trend endpoint API tests
@WebMvcTest(ActivityLogAPI.class)
@Import(ExceptionControllerAdvice.class)
class ActivityTrendAPITests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityLogService activityLogService;

    private static final String NO_ACTIVITY_FOUND = "No activity logs found for the given user.";
    private static final String INVALID_DATE_RANGE = "From date cannot be after to date.";

    @Test
    public void testGetActivityTrend_Valid_AllTypes() throws Exception {
        ActivityTrendPointDTO point1 = new ActivityTrendPointDTO();
        point1.setActivityDate(LocalDate.of(2026, 4, 1));
        point1.setActivityType(ActivityType.STEPS);
        point1.setTotalValue(8500.0);
        point1.setUnit("steps");

        ActivityTrendPointDTO point2 = new ActivityTrendPointDTO();
        point2.setActivityDate(LocalDate.of(2026, 4, 2));
        point2.setActivityType(ActivityType.WORKOUT);
        point2.setTotalValue(45.0);
        point2.setUnit("minutes");

        List<ActivityTrendPointDTO> trend = Arrays.asList(point1, point2);

        Mockito.when(activityLogService.getActivityTrend(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.isNull()))
                .thenReturn(trend);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/trend", 2)
                .param("fromDate", "2026-04-01")
                .param("toDate", "2026-04-30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].activityType", Matchers.is("STEPS")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].totalValue", Matchers.is(8500.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].activityType", Matchers.is("WORKOUT")));
    }

    @Test
    public void testGetActivityTrend_Valid_FilteredByType() throws Exception {
        ActivityTrendPointDTO point = new ActivityTrendPointDTO();
        point.setActivityDate(LocalDate.of(2026, 4, 1));
        point.setActivityType(ActivityType.STEPS);
        point.setTotalValue(8500.0);
        point.setUnit("steps");

        Mockito.when(activityLogService.getActivityTrend(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.eq(ActivityType.STEPS)))
                .thenReturn(List.of(point));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/trend", 2)
                .param("fromDate", "2026-04-01")
                .param("toDate", "2026-04-30")
                .param("metricType", "STEPS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].activityType", Matchers.is("STEPS")));
    }

    @Test
    public void testGetActivityTrend_NoActivity_ShouldFail() throws Exception {
        Mockito.when(activityLogService.getActivityTrend(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.isNull()))
                .thenThrow(new WellnessTrackerException("Service.NO_ACTIVITY_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/trend", 2)
                .param("fromDate", "2026-01-01")
                .param("toDate", "2026-01-31")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NO_ACTIVITY_FOUND)));
    }

    @Test
    public void testGetActivityTrend_InvalidDateRange_ShouldFail() throws Exception {
        Mockito.when(activityLogService.getActivityTrend(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.isNull()))
                .thenThrow(new WellnessTrackerException("Service.INVALID_DATE_RANGE"));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/trend", 2)
                .param("fromDate", "2026-04-30")
                .param("toDate", "2026-04-01")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(INVALID_DATE_RANGE)));
    }
}