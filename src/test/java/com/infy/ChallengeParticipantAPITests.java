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

    private static final String USER_NOT_FOUND = "User not found. Please provide a valid user ID.";
    private static final String JOIN_SUCCESS = "Successfully joined the challenge. Participant ID: ";
    private static final String NO_JOINED_CHALLENGES = "You have not joined any challenges yet.";

    private JoinChallengeRequestDTO validJoinRequest() {
        JoinChallengeRequestDTO req = new JoinChallengeRequestDTO();
        req.setUserId(2);
        req.setChallengeId(1);
        return req;
    }

    private ActiveChallengeResponseDTO sampleActiveChallenge(Integer id) {
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

    @Test
    public void testJoinChallenge_Valid() throws Exception {
        Mockito.when(participantService.joinChallenge(ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenReturn(1);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(JOIN_SUCCESS + 1));
    }

    @Test
    public void testJoinChallenge_InvalidUserNotFound() throws Exception {
        Mockito.when(participantService.joinChallenge(ArgumentMatchers.any(JoinChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validJoinRequest())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testGetActiveChallenges_Valid() throws Exception {
        List<ActiveChallengeResponseDTO> challenges = new ArrayList<>();
        challenges.add(sampleActiveChallenge(1));

        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenReturn(challenges);

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].title", Matchers.is("10K Steps Daily Challenge")));
    }

    @Test
    public void testGetActiveChallenges_InvalidUserNotFound() throws Exception {
        Mockito.when(participantService.getActiveChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.USER_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(USER_NOT_FOUND)));
    }

    @Test
    public void testGetMyChallenges_Valid() throws Exception {
        List<MyChallengeResponseDTO> myChallenges = new ArrayList<>();
        myChallenges.add(sampleMyChallenge(1, 3));

        Mockito.when(participantService.getMyChallenges(ArgumentMatchers.anyInt()))
                .thenReturn(myChallenges);

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}/my-challenges", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].title", Matchers.is("Hydration Hero")));
    }

    @Test
    public void testGetMyChallenges_InvalidNoJoinedChallenges() throws Exception {
        Mockito.when(participantService.getMyChallenges(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.NO_JOINED_CHALLENGES"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/users/{userId}/my-challenges", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(NO_JOINED_CHALLENGES)));
    }
}
