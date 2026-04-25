package com.infy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.api.ActivityLogAPI;
import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivityMetricDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;
import com.infy.utility.ExceptionControllerAdvice;

@WebMvcTest(ActivityLogAPI.class)
@Import(ExceptionControllerAdvice.class)
public class ActivityLogAPITests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ActivityLogService activityLogService;

	private static final String CREATE_SUCCESS = "Activity logged successfully. Activity Log ID: ";
	private static final String USER_NOT_FOUND = "User not found. Please provide a valid user ID.";
	private static final String NO_ACTIVITY_FOUND = "No activity logs found for the given user.";

	// ----------------------------------------------------------
	// US 01 - Create Activity Log
	// ----------------------------------------------------------

	@Test
	public void testCreateActivityLog_Valid() throws Exception {
		ActivityLogRequestDTO request = new ActivityLogRequestDTO();
		request.setUserId(2);
		request.setActivityType(ActivityType.STEPS);
		request.setActivityDate(LocalDate.of(2026, 4, 19));
		request.setActivityValue(9500);
		request.setUnit("steps");
		request.setNotes("Evening walk");

		Integer activityLogId = 5;
		Mockito.when(activityLogService.createActivityLog(ArgumentMatchers.any(ActivityLogRequestDTO.class)))
				.thenReturn(activityLogId);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/activity-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.content().string(CREATE_SUCCESS + activityLogId));
	}

	@Test
	public void testCreateActivityLog_InvalidUserId() throws Exception {
		ActivityLogRequestDTO request = new ActivityLogRequestDTO();
		request.setUserId(null);
		request.setActivityType(ActivityType.STEPS);
		request.setActivityDate(LocalDate.of(2026, 4, 19));
		request.setActivityValue(9500);
		request.setUnit("steps");

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/activity-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
						Matchers.is(HttpStatus.BAD_REQUEST.value())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("User ID is required")));
	}

	@Test
	public void testCreateActivityLog_InvalidActivityValue() throws Exception {
		ActivityLogRequestDTO request = new ActivityLogRequestDTO();
		request.setUserId(2);
		request.setActivityType(ActivityType.STEPS);
		request.setActivityDate(LocalDate.of(2026, 4, 19));
		request.setActivityValue(-100);
		request.setUnit("steps");

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/activity-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
						Matchers.is(HttpStatus.BAD_REQUEST.value())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("Activity value must be greater than 0")));
	}

	@Test
	public void testCreateActivityLog_FutureDate() throws Exception {
		ActivityLogRequestDTO request = new ActivityLogRequestDTO();
		request.setUserId(2);
		request.setActivityType(ActivityType.STEPS);
		request.setActivityDate(LocalDate.now().plusDays(5));
		request.setActivityValue(9500);
		request.setUnit("steps");

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/activity-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("Activity date cannot be in the future")));
	}

	@Test
	public void testCreateActivityLog_UserNotFound() throws Exception {
		ActivityLogRequestDTO request = new ActivityLogRequestDTO();
		request.setUserId(999);
		request.setActivityType(ActivityType.STEPS);
		request.setActivityDate(LocalDate.of(2026, 4, 19));
		request.setActivityValue(9500);
		request.setUnit("steps");

		Mockito.when(activityLogService.createActivityLog(ArgumentMatchers.any(ActivityLogRequestDTO.class)))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/activity-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
						Matchers.is(HttpStatus.BAD_REQUEST.value())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}

	// ----------------------------------------------------------
	// US 01 - Get Activity History
	// ----------------------------------------------------------

	@Test
	public void testGetActivityHistory_Valid() throws Exception {
		ActivityLogResponseDTO log1 = new ActivityLogResponseDTO();
		log1.setActivityLogId(1);
		log1.setUserId(2);
		log1.setUserName("John Doe");
		log1.setActivityType(ActivityType.STEPS);
		log1.setActivityDate(LocalDate.of(2026, 4, 18));
		log1.setActivityValue(7600);
		log1.setUnit("steps");

		ActivityLogResponseDTO log2 = new ActivityLogResponseDTO();
		log2.setActivityLogId(2);
		log2.setUserId(2);
		log2.setUserName("John Doe");
		log2.setActivityType(ActivityType.WORKOUT);
		log2.setActivityDate(LocalDate.of(2026, 4, 15));
		log2.setActivityValue(45);
		log2.setUnit("minutes");

		List<ActivityLogResponseDTO> logs = new ArrayList<>();
		logs.add(log1);
		logs.add(log2);

		Mockito.when(activityLogService.getActivityHistory(ArgumentMatchers.anyInt()))
				.thenReturn(logs);

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/activity-logs/users/{userId}", 2)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].userName", Matchers.is("John Doe")))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].activityType", Matchers.is("STEPS")))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].activityValue", Matchers.is(7600.0)));
	}

	@Test
	public void testGetActivityHistory_UserNotFound() throws Exception {
		Mockito.when(activityLogService.getActivityHistory(ArgumentMatchers.anyInt()))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/activity-logs/users/{userId}", 999)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
						Matchers.is(HttpStatus.BAD_REQUEST.value())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}

	@Test
	public void testGetActivityHistory_NoActivityFound() throws Exception {
		Mockito.when(activityLogService.getActivityHistory(ArgumentMatchers.anyInt()))
				.thenThrow(new WellnessTrackerException("Service.NO_ACTIVITY_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/activity-logs/users/{userId}", 4)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(NO_ACTIVITY_FOUND)));
	}

	// ----------------------------------------------------------
	// US 02 - Get Activity Summary
	// ----------------------------------------------------------

	@Test
	public void testGetActivitySummary_Valid() throws Exception {
		ActivityMetricDTO stepsMetric = new ActivityMetricDTO();
		stepsMetric.setActivityType(ActivityType.STEPS);
		stepsMetric.setTotalValue(46600);
		stepsMetric.setUnit("steps");

		ActivityMetricDTO workoutMetric = new ActivityMetricDTO();
		workoutMetric.setActivityType(ActivityType.WORKOUT);
		workoutMetric.setTotalValue(150);
		workoutMetric.setUnit("minutes");

		List<ActivityMetricDTO> metrics = new ArrayList<>();
		metrics.add(stepsMetric);
		metrics.add(workoutMetric);

		ActivitySummaryDTO summary = new ActivitySummaryDTO();
		summary.setUserId(2);
		summary.setFromDate(LocalDate.of(2026, 4, 1));
		summary.setToDate(LocalDate.of(2026, 4, 30));
		summary.setTotalActivities(15);
		summary.setMetrics(metrics);

		Mockito.when(activityLogService.getActivitySummary(
				ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenReturn(summary);

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/activity-logs/users/{userId}/summary", 2)
				.param("fromDate", "2026-04-01")
				.param("toDate", "2026-04-30")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.userId", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.totalActivities", Matchers.is(15)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.metrics.length()", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.metrics[0].activityType", Matchers.is("STEPS")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.metrics[0].totalValue", Matchers.is(46600.0)));
	}

	@Test
	public void testGetActivitySummary_UserNotFound() throws Exception {
		Mockito.when(activityLogService.getActivitySummary(
				ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/activity-logs/users/{userId}/summary", 999)
				.param("fromDate", "2026-04-01")
				.param("toDate", "2026-04-30")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}
}
