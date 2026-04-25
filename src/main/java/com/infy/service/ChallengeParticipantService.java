package com.infy.service;

import java.util.List;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeParticipantService {

    // US 03 - Join a challenge
    public Integer joinChallenge(JoinChallengeRequestDTO requestDTO) throws WellnessTrackerException;

    // US 03 - List of active/upcoming challenges visible to a user (with alreadyJoined flag)
    public List<ActiveChallengeResponseDTO> getActiveChallenges(Integer userId) throws WellnessTrackerException;

    // US 03 - My Challenges: all challenges the user joined + live progress
    public List<MyChallengeResponseDTO> getMyChallenges(Integer userId) throws WellnessTrackerException;
}
