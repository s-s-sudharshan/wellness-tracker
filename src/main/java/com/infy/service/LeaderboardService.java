package com.infy.service;

import com.infy.dto.LeaderboardResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface LeaderboardService {

    // US 04 - Get ranked leaderboard for a challenge, from perspective of requestingUserId
    public LeaderboardResponseDTO getLeaderboard(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException;
}
