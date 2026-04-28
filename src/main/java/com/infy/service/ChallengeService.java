package com.infy.service;

import java.util.List;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeService {

    public Integer createChallenge(ChallengeRequestDTO requestDTO) throws WellnessTrackerException;

    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId) throws WellnessTrackerException;

    public ChallengeResponseDTO getChallengeById(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException;
    
    // US 07 - Featured challenges visible to the requesting user's department
    // Returns ActiveChallengeResponseDTO (same shape as catalog) — includes alreadyJoined flag
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges(Integer userId)
            throws WellnessTrackerException;
}
