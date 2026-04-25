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
import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.enums.ParticipantStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ChallengeParticipantService;
import com.infy.service.ChallengeService;
import com.infy.utility.ExceptionControllerAdvice;

@WebMvcTest(ChallengeAPI.class)
@Import(ExceptionControllerAdvice.class)
class ChallengeParticipantAPITests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChallengeService challengeService;

    @MockBean
    private ChallengeParticipantService participantService;

    private static final String JOIN_SUCCESS         = "Successfully joined the challenge. Participant ID: ";
    private static final String USER_NOT_FOUND       = "User not found. Please provide a valid user ID.";
    private static final String CHALLENGE_NOT_FOUND  = "Challenge not found. Please provide a valid challenge ID.";
    private static final String ALREADY_JOINED       = "You have already joined this challenge.";
    private static final String CHALLENGE_COMPLETED  = "Cannot join a challenge that has already been completed.";
    private static final String NOT_AN_EMPLOYEE      = "Only employees and managers can join challenges.";
    private static final String NO_CHALLENGES_FOUND  = "No challenges found for the given manager.";
    private static final String NO_JOINED_CHALLENGES = "You have not joined any challenges yet.";

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private JoinChallengeRequestDTO validJoinRequest() {
        JoinChallengeRequestDTO req = new JoinChallengeRequestDTO();
        req.setUserId(2);
        req.setChallengeId(1);
        return req;
    }

    private ActiveChallengeResponseDTO sampleActiveChallenge(Integer id, boolean alreadyJoined) {
        ActiveChallengeResponseDTO dto = new ActiveChallengeResponseDTO();
        dto.setChallengeId(id);
        dto.setTitle("10K Steps Daily Challenge");
        dto.setDescription("Walk 10,000 steps every day.");
        dto.setCreatedByName("Sarah Connor");
        dto.setMetricType(ActivityType.STEPS);
        dto.setUnit("steps");
        dto.setGoalValue(70000.0);
        dto.setDifficulty(Difficulty.MEDIUM);
        dto.setStartDate(LocalDate.now().plusDays(2));
        dto.setEndDate(LocalDate.now().plusDays(9));
        dto.setIsFeatured(true);
        dto.setStatus(ChallengeStatus.UPCOMING);
        dto.setAlreadyJoined(alreadyJoined);
        return dto;
    }

    private ActiveChallengeResponseDTO sampleDepartmentChallenge(Integer id) {
        ActiveChallengeResponseDTO dto = new ActiveChallengeResponseDTO();
        dto.setChallengeId(id);
        dto.setTitle("Morning Workout Sprint");
        dto.setDescription("Complete 120 minutes of workout this week.");
        dto.setCreatedByName("Sarah Connor");
        dto.setMetricType(ActivityType.WORKOUT);
        dto.setUnit("minutes");
        dto.setGoalValue(120.0);
        dto.setDifficulty(Difficulty.EASY);
        dto.setStartDate(LocalDate.now().plusDays(2));
        dto.setEndDate(LocalDate.now().plusDays(9));
        dto.setDepartmentName("Engineering");
        dto.setIsFeatured(false);
        dto.setStatus(ChallengeStatus.UPCOMING);
        dto.setAlreadyJoined(false);
        return dto;
    }

    private MyChallengeResponseDTO sampleMyChallenge(Integer participantId, Integer challengeId) {
        MyChallengeResponseDTO dto = new MyChallengeResponseDTO();
        dto.setParticipantId(participantId);
        dto.setJoinedAt(LocalDateTime.now());
        dto.setParticipantStatus(ParticipantStatus.JOINED);
        dto.setChallengeId(challengeId);
        dto.setTitle("Hydration Hero");
        dto.setDescription("Log at least 21 liters of water intake.");
        dto.setCreatedByName("Sarah Connor");
        dto.setMetricType(ActivityType.WATER);
        dto.setUnit("liters");
        dto.setGoalValue(21.0);
        dto.setDifficulty(Difficulty.EASY);
        dto.setStartDate(LocalDate.now().minusDays(3));
        dto.setEndDate(LocalDate.now().plusDays(4));
        dto.setDaysRemaining(4);
        dto.setChallengeStatus(ChallengeStatus.ACTIVE);
        dto.setActualValue(7.5);
        dto.setProgressPct(35);
        return dto;
    }

    // ----------------------------------------------------------
    // POST /wellness/challenges/join
    // ----------------------------------------------------------

    @Test
    public void testJoinChallenge_Valid() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenReturn(1);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(JOIN_SUCCESS + 1));
    }

    @Test
    public void testJoinChallenge_MissingUserId() throws Exception {
        JoinChallengeRequestDTO req = validJoinRequest();
        req.setUserId(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode",
                        Matchers.is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("User ID is required")));
    }

    @Test
    public void testJoinChallenge_MissingChallengeId() throws Exception {
        JoinChallengeRequestDTO req = validJoinRequest();
        req.setChallengeId(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.containsString("Challenge ID is required")));
    }

    @Test
    public void testJoinChallenge_UserNotFound() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testJoinChallenge_ChallengeNotFound() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(CHALLENGE_NOT_FOUND)));
    }

    @Test
    public void testJoinChallenge_AlreadyJoined() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.ALREADY_JOINED_CHALLENGE"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(ALREADY_JOINED)));
    }

    @Test
    public void testJoinChallenge_ChallengeAlreadyCompleted() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.CHALLENGE_ALREADY_COMPLETED"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(CHALLENGE_COMPLETED)));
    }

    @Test
    public void testJoinChallenge_NotAnEmployee() throws Exception {
        Mockito.when(participantService.joinChallenge(
                ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.NOT_AN_EMPLOYEE"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NOT_AN_EMPLOYEE)));
    }

    // ----------------------------------------------------------
    // GET /wellness/challenges/users/{userId} — Active Challenges
    // ----------------------------------------------------------

    @Test
    public void testGetActiveChallenges_Valid() throws Exception {
        List<ActiveChallengeResponseDTO> challenges = new ArrayList<>();
        challenges.add(sampleActiveChallenge(1, false));
        challenges.add(sampleDepartmentChallenge(2));

        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenReturn(challenges);

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].title",
                        Matchers.is("10K Steps Daily Challenge")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].createdByName",
                        Matchers.is("Sarah Connor")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].metricType",
                        Matchers.is("STEPS")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].unit",
                        Matchers.is("steps")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].goalValue",
                        Matchers.is(70000.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].alreadyJoined",
                        Matchers.is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].unit",
                        Matchers.is("minutes")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].departmentName",
                        Matchers.is("Engineering")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].alreadyJoined",
                        Matchers.is(false)));
    }

    @Test
    public void testGetActiveChallenges_AlreadyJoinedFlagTrue() throws Exception {
        List<ActiveChallengeResponseDTO> challenges = new ArrayList<>();
        challenges.add(sampleActiveChallenge(1, true));

        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenReturn(challenges);

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].alreadyJoined",
                        Matchers.is(true)));
    }

    @Test
    public void testGetActiveChallenges_UserNotFound() throws Exception {
        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testGetActiveChallenges_NoChallengesFound() throws Exception {
        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.NO_CHALLENGES_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NO_CHALLENGES_FOUND)));
    }

    // ----------------------------------------------------------
    // GET /wellness/challenges/users/{userId}/my-challenges
    // ----------------------------------------------------------

    @Test
    public void testGetMyChallenges_Valid() throws Exception {
        List<MyChallengeResponseDTO> myChallenges = new ArrayList<>();
        myChallenges.add(sampleMyChallenge(1, 3));
        myChallenges.add(sampleMyChallenge(2, 4));

        Mockito.when(participantService.getMyChallenges(ArgumentMatchers.anyInt()))
                .thenReturn(myChallenges);

        mockMvc.perform(MockMvcRequestBuilders.get(
                "/wellness/challenges/users/{userId}/my-challenges", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].title",
                        Matchers.is("Hydration Hero")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].createdByName",
                        Matchers.is("Sarah Connor")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].metricType",
                        Matchers.is("WATER")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].unit",
                        Matchers.is("liters")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].actualValue",
                        Matchers.is(7.5)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].progressPct",
                        Matchers.is(35)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].daysRemaining",
                        Matchers.is(4)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].challengeStatus",
                        Matchers.is("ACTIVE")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].participantStatus",
                        Matchers.is("JOINED")));
    }

    @Test
    public void testGetMyChallenges_UserNotFound() throws Exception {
        Mockito.when(participantService.getMyChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get(
                "/wellness/challenges/users/{userId}/my-challenges", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testGetMyChallenges_NoJoinedChallenges() throws Exception {
        Mockito.when(participantService.getMyChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.NO_JOINED_CHALLENGES"));

        mockMvc.perform(MockMvcRequestBuilders.get(
                "/wellness/challenges/users/{userId}/my-challenges", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage",
                        Matchers.is(NO_JOINED_CHALLENGES)));
    }
}
