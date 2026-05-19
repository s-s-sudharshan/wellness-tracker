package com.infy.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.LeaderboardResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface LeaderboardService {

    // US 04 - Get ranked leaderboard for a challenge from the JWT caller's perspective.
    // requestingUserId removed — caller identity derived from JWT inside implementation.
    // Visibility check (same dept / creator / participant) enforced in service.
    @PreAuthorize("isAuthenticated()")
    public LeaderboardResponseDTO getLeaderboard(Integer challengeId)
            throws WellnessTrackerException;
}