package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.LeaderboardResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.LeaderboardService;

@RestController
@RequestMapping("/wellness")
public class LeaderboardAPI {

    @Autowired
    private LeaderboardService leaderboardService;

    // US 04 - Get leaderboard for a challenge from a specific user's perspective
    // requestingUserId drives the isCurrentUser flag and currentUserRank fields
    @GetMapping(value = "/challenges/{challengeId}/leaderboard")
    public ResponseEntity<LeaderboardResponseDTO> getLeaderboard(
            @PathVariable Integer challengeId,
            @RequestParam Integer requestingUserId)
            throws WellnessTrackerException {
        LeaderboardResponseDTO response = leaderboardService
                .getLeaderboard(challengeId, requestingUserId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
