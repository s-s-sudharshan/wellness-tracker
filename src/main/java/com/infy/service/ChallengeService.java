package com.infy.service;

import java.util.List;

import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeService {

    public Integer createChallenge(ChallengeRequestDTO requestDTO) throws WellnessTrackerException;

    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId) throws WellnessTrackerException;

    public ChallengeResponseDTO getChallengeById(Integer challengeId) throws WellnessTrackerException;
}
