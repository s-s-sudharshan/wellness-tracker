package com.infy;
 
import java.time.LocalDate;
import java.util.ArrayList;

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
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;
import com.infy.utility.ExceptionControllerAdvice;

@WebMvcTest(ActivityLogAPI.class)
@Import(ExceptionControllerAdvice.class)
class ActivityLogFixRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityLogService activityLogService;

    private static final String INVALID_DATE_RANGE = "From date cannot be after to date.";

    // GET /wellness/activity-logs/users/{userId}/summary (invalid)
    @Test
    public void testGetActivitySummary_InvalidDateRange_ShouldFail() throws Exception {
        Mockito.when(activityLogService.getActivitySummary(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class)))
                .thenThrow(new WellnessTrackerException("Service.INVALID_DATE_RANGE"));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/summary", 2)
                .param("fromDate", "2026-04-30")   // fromDate AFTER toDate
                .param("toDate",   "2026-04-01")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(INVALID_DATE_RANGE)));
    }

    // GET /wellness/activity-logs/users/{userId}/summary (valid)
    @Test
    public void testGetActivitySummary_ValidDateRange_ShouldPass() throws Exception {
        com.infy.dto.ActivitySummaryDTO summary = new com.infy.dto.ActivitySummaryDTO();
        summary.setUserId(2);
        summary.setFromDate(LocalDate.of(2026, 4, 1));
        summary.setToDate(LocalDate.of(2026, 4, 30));
        summary.setTotalActivities(10);
        summary.setMetrics(new ArrayList<>());

        Mockito.when(activityLogService.getActivitySummary(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(summary);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/activity-logs/users/{userId}/summary", 2)
                .param("fromDate", "2026-04-01")
                .param("toDate",   "2026-04-30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalActivities",
                        Matchers.is(10)));
    }
}
