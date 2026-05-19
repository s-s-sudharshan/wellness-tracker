package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.LeaderboardResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.LeaderboardService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class LeaderboardAPI {

    @Autowired
    private LeaderboardService leaderboardService;

    // US 04 - Get leaderboard for a challenge from the JWT caller's perspective.
    // requestingUserId query param removed — caller identity derived from JWT inside service.
    // isCurrentUser flag and currentUserRank fields are still populated correctly.
    @GetMapping(value = "/challenges/{challengeId}/leaderboard")
    public ResponseEntity<LeaderboardResponseDTO> getLeaderboard(
            @PathVariable Integer challengeId)
            throws WellnessTrackerException {
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(challengeId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
