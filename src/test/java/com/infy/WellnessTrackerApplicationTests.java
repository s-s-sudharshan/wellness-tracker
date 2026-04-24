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
import com.infy.api.MoodLogAPI;
import com.infy.api.WeeklyGoalAPI;
import com.infy.dto.ActivityLogRequestDTO;
import com.infy.dto.ActivityLogResponseDTO;
import com.infy.dto.ActivityMetricDTO;
import com.infy.dto.ActivitySummaryDTO;
import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.enums.ActivityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ActivityLogService;
import com.infy.service.MoodLogService;
import com.infy.service.WeeklyGoalService;
import com.infy.utility.ExceptionControllerAdvice;

// ============================================================
// Each API class is tested in its own @WebMvcTest slice.
// Three separate test classes are written below.
// Split them into three separate files in your project.
// ============================================================


// ============================================================
// US 01 and US 02 — ActivityLogAPI Tests
// ============================================================

@WebMvcTest(ActivityLogAPI.class)
@Import(ExceptionControllerAdvice.class)
class ActivityLogAPITests {

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


// ============================================================
// US 11 — WeeklyGoalAPI Tests
// File: WeeklyGoalAPITests.java
// ============================================================

@WebMvcTest(WeeklyGoalAPI.class)
@Import(ExceptionControllerAdvice.class)
class WeeklyGoalAPITests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WeeklyGoalService weeklyGoalService;

	private static final String WEEKLY_GOAL_SUCCESS = "Weekly goal saved successfully. Weekly Goal ID: ";
	private static final String USER_NOT_FOUND = "User not found. Please provide a valid user ID.";
	private static final String WEEKLY_GOAL_NOT_FOUND = "No weekly goal found for the given user and week.";

	@Test
	public void testSaveWeeklyGoal_Valid() throws Exception {
		WeeklyGoalRequestDTO request = new WeeklyGoalRequestDTO();
		request.setUserId(2);
		request.setWeekStartDate(LocalDate.of(2026, 4, 21));
		request.setStepsGoal(50000.0);
		request.setWorkoutGoal(150.0);
		request.setWaterGoal(14.0);
		request.setMeditationGoal(60.0);
		request.setSleepGoalHours(49.0);

		Integer weeklyGoalId = 1;
		Mockito.when(weeklyGoalService.saveWeeklyGoal(ArgumentMatchers.any(WeeklyGoalRequestDTO.class)))
				.thenReturn(weeklyGoalId);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/weekly-goals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.content().string(WEEKLY_GOAL_SUCCESS + weeklyGoalId));
	}

	@Test
	public void testSaveWeeklyGoal_InvalidUserId() throws Exception {
		WeeklyGoalRequestDTO request = new WeeklyGoalRequestDTO();
		request.setUserId(null);
		request.setWeekStartDate(LocalDate.of(2026, 4, 21));
		request.setStepsGoal(50000.0);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/weekly-goals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
						Matchers.is(HttpStatus.BAD_REQUEST.value())))
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("User ID is required")));
	}

	@Test
	public void testSaveWeeklyGoal_InvalidGoalValue() throws Exception {
		WeeklyGoalRequestDTO request = new WeeklyGoalRequestDTO();
		request.setUserId(2);
		request.setWeekStartDate(LocalDate.of(2026, 4, 21));
		request.setStepsGoal(-1000.0);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/weekly-goals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("Steps goal must be greater than 0")));
	}

	@Test
	public void testSaveWeeklyGoal_UserNotFound() throws Exception {
		WeeklyGoalRequestDTO request = new WeeklyGoalRequestDTO();
		request.setUserId(999);
		request.setWeekStartDate(LocalDate.of(2026, 4, 21));
		request.setStepsGoal(50000.0);

		Mockito.when(weeklyGoalService.saveWeeklyGoal(ArgumentMatchers.any(WeeklyGoalRequestDTO.class)))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/weekly-goals")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}

	@Test
	public void testGetWeeklyGoal_Valid() throws Exception {
		WeeklyGoalResponseDTO response = new WeeklyGoalResponseDTO();
		response.setWeeklyGoalId(1);
		response.setUserId(2);
		response.setWeekStartDate(LocalDate.of(2026, 4, 21));
		response.setWeekEndDate(LocalDate.of(2026, 4, 27));
		response.setStepsGoal(50000.0);
		response.setWorkoutGoal(150.0);
		response.setStepsActual(18900.0);
		response.setWorkoutActual(45.0);
		response.setStepsProgressPct(37);
		response.setWorkoutProgressPct(30);

		Mockito.when(weeklyGoalService.getWeeklyGoal(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class)))
				.thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/weekly-goals/users/{userId}", 2)
				.param("weekStartDate", "2026-04-21")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.userId", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.stepsGoal", Matchers.is(50000.0)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.stepsActual", Matchers.is(18900.0)))
				.andExpect(MockMvcResultMatchers.jsonPath("$.stepsProgressPct", Matchers.is(37)));
	}

	@Test
	public void testGetWeeklyGoal_NotFound() throws Exception {
		Mockito.when(weeklyGoalService.getWeeklyGoal(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class)))
				.thenThrow(new WellnessTrackerException("Service.WEEKLY_GOAL_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/weekly-goals/users/{userId}", 2)
				.param("weekStartDate", "2026-04-21")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(WEEKLY_GOAL_NOT_FOUND)));
	}
}


// ============================================================
// US 12 — MoodLogAPI Tests
// File: MoodLogAPITests.java
// ============================================================

@WebMvcTest(MoodLogAPI.class)
@Import(ExceptionControllerAdvice.class)
class MoodLogAPITests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private MoodLogService moodLogService;

	private static final String MOOD_LOG_SUCCESS = "Mood logged successfully. Mood Log ID: ";
	private static final String USER_NOT_FOUND = "User not found. Please provide a valid user ID.";
	private static final String NO_MOOD_FOUND = "No mood logs found for the given user and date range.";

	@Test
	public void testSaveMoodLog_Valid() throws Exception {
		MoodLogRequestDTO request = new MoodLogRequestDTO();
		request.setUserId(2);
		request.setLogDate(LocalDate.of(2026, 4, 19));
		request.setMoodScore(4);
		request.setNote("Feeling good after workout");

		Integer moodLogId = 1;
		Mockito.when(moodLogService.saveMoodLog(ArgumentMatchers.any(MoodLogRequestDTO.class)))
				.thenReturn(moodLogId);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/mood-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.content().string(MOOD_LOG_SUCCESS + moodLogId));
	}

	@Test
	public void testSaveMoodLog_InvalidMoodScore() throws Exception {
		MoodLogRequestDTO request = new MoodLogRequestDTO();
		request.setUserId(2);
		request.setLogDate(LocalDate.of(2026, 4, 19));
		request.setMoodScore(6);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/mood-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("Mood score must be between 1 and 5")));
	}

	@Test
	public void testSaveMoodLog_FutureDate() throws Exception {
		MoodLogRequestDTO request = new MoodLogRequestDTO();
		request.setUserId(2);
		request.setLogDate(LocalDate.now().plusDays(1));
		request.setMoodScore(3);

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/mood-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.containsString("Log date cannot be in the future")));
	}

	@Test
	public void testSaveMoodLog_UserNotFound() throws Exception {
		MoodLogRequestDTO request = new MoodLogRequestDTO();
		request.setUserId(999);
		request.setLogDate(LocalDate.of(2026, 4, 19));
		request.setMoodScore(3);

		Mockito.when(moodLogService.saveMoodLog(ArgumentMatchers.any(MoodLogRequestDTO.class)))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.post("/wellness/mood-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}

	@Test
	public void testGetMoodTrend_Valid() throws Exception {
		MoodLogResponseDTO mood1 = new MoodLogResponseDTO();
		mood1.setMoodLogId(1);
		mood1.setUserId(2);
		mood1.setLogDate(LocalDate.of(2026, 4, 1));
		mood1.setMoodScore(3);
		mood1.setMoodLabel("OK");

		MoodLogResponseDTO mood2 = new MoodLogResponseDTO();
		mood2.setMoodLogId(2);
		mood2.setUserId(2);
		mood2.setLogDate(LocalDate.of(2026, 4, 2));
		mood2.setMoodScore(4);
		mood2.setMoodLabel("Good");

		List<MoodLogResponseDTO> trend = new ArrayList<>();
		trend.add(mood1);
		trend.add(mood2);

		Mockito.when(moodLogService.getMoodTrend(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenReturn(trend);

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/mood-logs/users/{userId}", 2)
				.param("fromDate", "2026-04-01")
				.param("toDate", "2026-04-30")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].moodScore", Matchers.is(3)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].moodLabel", Matchers.is("OK")))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].moodScore", Matchers.is(4)));
	}

	@Test
	public void testGetMoodTrend_NoMoodFound() throws Exception {
		Mockito.when(moodLogService.getMoodTrend(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenThrow(new WellnessTrackerException("Service.NO_MOOD_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/mood-logs/users/{userId}", 2)
				.param("fromDate", "2026-01-01")
				.param("toDate", "2026-01-31")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(NO_MOOD_FOUND)));
	}

	@Test
	public void testGetMoodCorrelation_Valid() throws Exception {
		MoodCorrelationDTO day1 = new MoodCorrelationDTO();
		day1.setDate(LocalDate.of(2026, 4, 1));
		day1.setMoodScore(3);
		day1.setMoodLabel("OK");
		day1.setHadActivity(true);
		day1.setActivityCount(2);

		MoodCorrelationDTO day2 = new MoodCorrelationDTO();
		day2.setDate(LocalDate.of(2026, 4, 7));
		day2.setMoodScore(2);
		day2.setMoodLabel("Low");
		day2.setHadActivity(false);
		day2.setActivityCount(0);

		List<MoodCorrelationDTO> correlation = new ArrayList<>();
		correlation.add(day1);
		correlation.add(day2);

		Mockito.when(moodLogService.getMoodCorrelation(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenReturn(correlation);

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/mood-logs/users/{userId}/correlation", 2)
				.param("fromDate", "2026-04-01")
				.param("toDate", "2026-04-30")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].hadActivity", Matchers.is(true)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[0].activityCount", Matchers.is(2)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].hadActivity", Matchers.is(false)))
				.andExpect(MockMvcResultMatchers.jsonPath("$[1].moodLabel", Matchers.is("Low")));
	}

	@Test
	public void testGetMoodCorrelation_UserNotFound() throws Exception {
		Mockito.when(moodLogService.getMoodCorrelation(ArgumentMatchers.anyInt(),
				ArgumentMatchers.any(LocalDate.class),
				ArgumentMatchers.any(LocalDate.class)))
				.thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

		mockMvc.perform(MockMvcRequestBuilders.get("/wellness/mood-logs/users/{userId}/correlation", 999)
				.param("fromDate", "2026-04-01")
				.param("toDate", "2026-04-30")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
						Matchers.is(USER_NOT_FOUND)));
	}
}
