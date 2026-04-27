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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infy.api.ChallengeAPI;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.enums.ActivityType;
import com.infy.enums.Difficulty;
import com.infy.enums.VisibilityType;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ChallengeParticipantService;
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

    @MockBean
    private ChallengeParticipantService participantService;

    private static final String NOT_A_MANAGER = "Only managers can create challenges.";

    private ChallengeRequestDTO validChallengeRequest() {
        ChallengeRequestDTO req = new ChallengeRequestDTO();
        req.setCreatedBy(1);
        req.setTitle("Test Challenge");
        req.setDescription("A test challenge.");
        req.setMetricType(ActivityType.STEPS);
        req.setGoalValue(10000.0);
        req.setDifficulty(Difficulty.EASY);
        req.setStartDate(LocalDate.now().plusDays(2));
        req.setEndDate(LocalDate.now().plusDays(9));
        req.setVisibilityType(VisibilityType.DEPARTMENT);
        req.setDepartmentId(1);
        return req;
    }

    @Test
    public void testCreateChallenge_Valid() throws Exception {
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenReturn(8);

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validChallengeRequest())))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .string("Challenge created successfully. Challenge ID: 8"));
    }

    @Test
    public void testCreateChallenge_InvalidNonManager() throws Exception {
        ChallengeRequestDTO req = validChallengeRequest();
        req.setCreatedBy(2);
        Mockito.when(challengeService.createChallenge(ArgumentMatchers.any(ChallengeRequestDTO.class)))
                .thenThrow(new WellnessTrackerException("Service.NOT_A_MANAGER"));

        mockMvc.perform(MockMvcRequestBuilders.post("/wellness/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(NOT_A_MANAGER)));
    }

    @Test
    public void testGetChallengesByManager_Valid() throws Exception {
        Mockito.when(challengeService.getChallengesByManager(ArgumentMatchers.anyInt()))
                .thenReturn(new ArrayList<>() {{
                    add(new com.infy.dto.ChallengeResponseDTO());
                }});

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/managers/{managerId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testGetChallengesByManager_InvalidNotManager() throws Exception {
        Mockito.when(challengeService.getChallengesByManager(ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.NOT_A_MANAGER"));

        mockMvc.perform(MockMvcRequestBuilders.get("/wellness/challenges/managers/{managerId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(NOT_A_MANAGER)));
    }
}
