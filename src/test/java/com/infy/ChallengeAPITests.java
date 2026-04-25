package com.infy;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.infy.api.ChallengeAPI;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ChallengeService;
import com.infy.utility.ExceptionControllerAdvice;

@WebMvcTest(ChallengeAPI.class)
@Import(ExceptionControllerAdvice.class)
class ChallengeAPITests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChallengeService challengeService;

    private static final String CREATE_SUCCESS      = "Challenge created successfully. Challenge ID: ";
    private static final String USER_NOT_FOUND      = "User not found. Please provide a valid user ID.";
    private static final String NOT_A_MANAGER       = "Only managers can create challenges.";
    private static final String INVALID_DATES       = "End date must be after start date.";
    private static final String DEPT_NOT_FOUND      = "Department not found. Please provide a valid department ID.";
    private static final String CHALLENGE_NOT_FOUND = "Challenge not found. Please provide a valid challenge ID.";
    private static final String NO_CHALLENGES_FOUND = "No challenges found for the given manager.";

    // ----------------------------------------------------------
    // Helper — builds a valid request DTO to reduce repetition
    // ----------------------------------------------------------

    private ChallengeRequestDTO validRequest() {
        ChallengeRequestDTO req = new ChallengeRequestDTO();
        req.setCreatedBy(1);
        req.setTitle("Step Up Challenge");
        req.setDescription("Log 50,000 steps over the next week.");
        req.setMetricType(ActivityType.STEPS);
        req.setGoalValue(50000.0);
        req.setDifficulty(Difficulty.MEDIUM);
        req.setStartDate(LocalDate.now().plusDays(2));
        req.setEndDate(LocalDate.now().plusDays(9));
        req.setVisibilityType(VisibilityType.COMPANY_WIDE);
        req.setIsFeatured(true);
        return req;
    }

    // Helper — builds a sample response DTO
    private ChallengeResponseDTO sampleResponse(Integer id) {
        ChallengeResponseDTO res = new ChallengeResponseDTO();
        res.setChallengeId(id);
        res.setTitle("Step Up Challenge");
        res.setDescription("Log 50,000 steps over the next week.");
        res.setCreatedBy(1);
        res.setCreatedByName("Sarah Connor");
        res.setMetricType(ActivityType.STEPS);
        res.setGoalValue(50000.0);
        res.setDifficulty(Difficulty.MEDIUM);
        res.setStartDate(LocalDate.now().plusDays(2));
        res.setEndDate(LocalDate.now().plusDays(9));
        res.setVisibilityType(VisibilityType.COMPANY_WIDE);
        res.setIsFeatured(true);
        res.setStatus(ChallengeStatus.UPCOMING);
        res.setCreatedAt(LocalDateTime.now());
        return res;
    }

    // ----------------------------------------------------------
    // POST /wellness/challenges — Create Challenge
    // ----------------------------------------------------------

    @Test
    public void testCreateChallenge_Valid() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenReturn(6);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(CREATE_SUCCESS + 6));
    }

    @Test
    public void testCreateChallenge_ValidDepartmentScoped() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setVisibilityType(VisibilityType.DEPARTMENT);
        req.setDepartmentId(1);
        req.setIsFeatured(false);

        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenReturn(7);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(CREATE_SUCCESS + 7));
    }

    @Test
    public void testCreateChallenge_NotAManager() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.NOT_A_MANAGER"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
                        Matchers.is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NOT_A_MANAGER)));
    }

    @Test
    public void testCreateChallenge_UserNotFound() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testCreateChallenge_InvalidDates() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.INVALID_CHALLENGE_DATES"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(INVALID_DATES)));
    }

    @Test
    public void testCreateChallenge_DepartmentNotFound() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.DEPARTMENT_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(DEPT_NOT_FOUND)));
    }

    @Test
    public void testCreateChallenge_MissingTitle() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setTitle("");

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Challenge title is required")));
    }

    @Test
    public void testCreateChallenge_MissingDescription() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setDescription("");

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Challenge description is required")));
    }

    @Test
    public void testCreateChallenge_MissingCreatedBy() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setCreatedBy(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Creator user ID is required")));
    }

    @Test
    public void testCreateChallenge_NegativeGoalValue() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setGoalValue(-500.0);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Goal value must be greater than 0")));
    }

    @Test
    public void testCreateChallenge_PastStartDate() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setStartDate(LocalDate.now().minusDays(1));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Start date must be in the future")));
    }

    @Test
    public void testCreateChallenge_MissingMetricType() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setMetricType(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Metric type is required")));
    }

    @Test
    public void testCreateChallenge_MissingDifficulty() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setDifficulty(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Difficulty is required")));
    }

    @Test
    public void testCreateChallenge_MissingVisibilityType() throws Exception {
        ChallengeRequestDTO req = validRequest();
        req.setVisibilityType(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Visibility type is required")));
    }

    // ----------------------------------------------------------
    // GET /wellness/challenges/managers/{managerId}
    // ----------------------------------------------------------

    @Test
    public void testGetChallengesByManager_Valid() throws Exception {
        List<ChallengeResponseDTO> challenges = new ArrayList<>();
        challenges.add(sampleResponse(1));
        challenges.add(sampleResponse(2));

        Mockito.when(challengeService.getChallengesByManager(ArgumentMatchers.anyInt()))
                .thenReturn(challenges);

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/managers/{managerId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].createdByName",
                        Matchers.is("Sarah Connor")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].metricType",
                        Matchers.is("STEPS")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status",
                        Matchers.is("UPCOMING")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].isFeatured",
                        Matchers.is(true)));
    }

    @Test
    public void testGetChallengesByManager_UserNotFound() throws Exception {
        Mockito.when(challengeService.getChallengesByManager(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/managers/{managerId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testGetChallengesByManager_NoChallengesFound() throws Exception {
        Mockito.when(challengeService.getChallengesByManager(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.NO_CHALLENGES_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/managers/{managerId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NO_CHALLENGES_FOUND)));
    }

    // ----------------------------------------------------------
    // GET /wellness/challenges/{challengeId}
    // ----------------------------------------------------------

    @Test
    public void testGetChallengeById_Valid() throws Exception {
        Mockito.when(challengeService.getChallengeById(ArgumentMatchers.anyInt()))
                .thenReturn(sampleResponse(1));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/{challengeId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.challengeId", Matchers.is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title",
                        Matchers.is("Step Up Challenge")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.goalValue", Matchers.is(50000.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.difficulty",
                        Matchers.is("MEDIUM")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdByName",
                        Matchers.is("Sarah Connor")));
    }

    @Test
    public void testGetChallengeById_NotFound() throws Exception {
        Mockito.when(challengeService.getChallengeById(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/{challengeId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(CHALLENGE_NOT_FOUND)));
    }
}