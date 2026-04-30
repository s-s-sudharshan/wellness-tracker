package com.infy.service;

import java.util.List;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.dto.ChallengeUpdateRequestDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeService {

    // US 13 - Manager creates a new challenge
    public Integer createChallenge(ChallengeRequestDTO requestDTO) throws WellnessTrackerException;
 
    // US 13 - Get all challenges created by a manager
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId) throws WellnessTrackerException;
 
    // US 13 - Get a single challenge by ID (visibility-guarded)
    public ChallengeResponseDTO getChallengeById(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException;
 
    // US 13 - Manager edits an UPCOMING challenge they created
    public ChallengeResponseDTO updateChallenge(Integer challengeId, ChallengeUpdateRequestDTO requestDTO)
            throws WellnessTrackerException;
 
    // US 13 - Manager deletes an UPCOMING challenge they created (no participants)
    public void deleteChallenge(Integer challengeId, Integer requestingUserId)
            throws WellnessTrackerException;
 
    // US 07 - Featured challenges visible to the requesting user's department
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges(Integer userId)
            throws WellnessTrackerException;
}
