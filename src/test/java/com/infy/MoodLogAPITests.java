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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.api.MoodLogAPI;
import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.MoodLogService;
import com.infy.utility.ExceptionControllerAdvice;

//US 12
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
