package com.infy;

import java.time.LocalDate;

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
import com.infy.api.WeeklyGoalAPI;
import com.infy.dto.WeeklyGoalRequestDTO;
import com.infy.dto.WeeklyGoalResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.WeeklyGoalService;
import com.infy.utility.ExceptionControllerAdvice;

// US 11
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
