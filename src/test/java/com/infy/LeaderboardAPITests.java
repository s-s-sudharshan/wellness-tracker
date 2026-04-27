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

import com.infy.api.LeaderboardAPI;
import com.infy.dto.LeaderboardEntryDTO;
import com.infy.dto.LeaderboardResponseDTO;
import com.infy.enums.ActivityType;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.Difficulty;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.LeaderboardService;
import com.infy.utility.ExceptionControllerAdvice;

@WebMvcTest(LeaderboardAPI.class)
@Import(ExceptionControllerAdvice.class)
class LeaderboardAPITests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    private static final String CHALLENGE_NOT_FOUND = "Challenge not found. Please provide a valid challenge ID.";

    private LeaderboardEntryDTO entry(int rank, String name, double value, String unit, int pct, boolean isCurrentUser) {
        LeaderboardEntryDTO e = new LeaderboardEntryDTO();
        e.setRank(rank);
        e.setParticipantName(name);
        e.setActualValue(value);
        e.setUnit(unit);
        e.setProgressPct(pct);
        e.setIsCurrentUser(isCurrentUser);
        return e;
    }

    private LeaderboardResponseDTO sampleLeaderboard() {
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        entries.add(entry(1, "Jane Smith", 200.0, "minutes", 66, false));
        entries.add(entry(2, "John Doe", 120.0, "minutes", 40, true));
        entries.add(entry(3, "Mike Jones", 100.0, "minutes", 33, false));

        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setChallengeId(4);
        dto.setChallengeTitle("Mindfulness Month");
        dto.setMetricType(ActivityType.MEDITATION);
        dto.setUnit("minutes");
        dto.setGoalValue(300.0);
        dto.setDifficulty(Difficulty.HARD);
        dto.setStartDate(LocalDate.of(2026, 4, 1));
        dto.setEndDate(LocalDate.of(2026, 4, 30));
        dto.setDaysRemaining(4);
        dto.setChallengeStatus(ChallengeStatus.ACTIVE);
        dto.setTotalParticipants(3);
        dto.setCurrentUserRank(2);
        dto.setCurrentUserValue(120.0);
        dto.setCurrentUserProgressPct(40);
        dto.setEntries(entries);
        return dto;
    }

    @Test
    public void testGetLeaderboard_Valid() throws Exception {
        Mockito.when(leaderboardService.getLeaderboard(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(sampleLeaderboard());

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/challenges/{challengeId}/leaderboard", 4)
                .param("requestingUserId", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.challengeTitle", Matchers.is("Mindfulness Month")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.entries.length()", Matchers.is(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currentUserRank", Matchers.is(2)));
    }

    @Test
    public void testGetLeaderboard_InvalidChallengeNotFound() throws Exception {
        Mockito.when(leaderboardService.getLeaderboard(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenThrow(new WellnessTrackerException("Service.CHALLENGE_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/wellness/challenges/{challengeId}/leaderboard", 999)
                .param("requestingUserId", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage", Matchers.is(CHALLENGE_NOT_FOUND)));
    }
}
