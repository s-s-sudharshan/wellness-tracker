package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeParticipantService {

    // US 03 - Join a challenge.
    // userId removed from JoinChallengeRequestDTO — derived from JWT inside implementation.
    @PreAuthorize("isAuthenticated()")
    public Integer joinChallenge(JoinChallengeRequestDTO requestDTO) throws WellnessTrackerException;

    // US 03 - List of active/upcoming challenges visible to the JWT caller (with alreadyJoined flag).
    @PreAuthorize("isAuthenticated()")
    public List<ActiveChallengeResponseDTO> getActiveChallenges() throws WellnessTrackerException;

    // US 03 - My Challenges: all challenges the JWT caller joined + live progress.
    @PreAuthorize("isAuthenticated()")
    public List<MyChallengeResponseDTO> getMyChallenges() throws WellnessTrackerException;
}
