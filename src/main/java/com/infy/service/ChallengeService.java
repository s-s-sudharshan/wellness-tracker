package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.dto.ChallengeUpdateRequestDTO;
import com.infy.exception.WellnessTrackerException;

public interface ChallengeService {

    // US 13 - Manager or HR creates a new challenge.
    // Role gate here; additional role check in service for dept mismatch etc.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public Integer createChallenge(ChallengeRequestDTO requestDTO) throws WellnessTrackerException;

    // US 13 - Get all challenges created by the manager/HR identified by managerId.
    // managerId is the TARGET — not necessarily the caller.
    // Role gate applied; service verifies the target is manager/HR.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public List<ChallengeResponseDTO> getChallengesByManager(Integer managerId)
            throws WellnessTrackerException;

    // US 13 / 03 - Get a single challenge by ID (visibility-guarded).
    // requestingUserId removed — derived from JWT inside implementation.
    @PreAuthorize("isAuthenticated()")
    public ChallengeResponseDTO getChallengeById(Integer challengeId)
            throws WellnessTrackerException;

    // US 13 - Manager edits an UPCOMING challenge they created.
    // requestingUserId removed from DTO — derived from JWT for ownership check.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ChallengeResponseDTO updateChallenge(Integer challengeId,
            ChallengeUpdateRequestDTO requestDTO) throws WellnessTrackerException;

    // US 13 - Manager deletes an UPCOMING challenge they created (no participants).
    // requestingUserId removed — derived from JWT for ownership check.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public void deleteChallenge(Integer challengeId) throws WellnessTrackerException;

    // US 07 - Featured challenges visible to the JWT caller's department.
    @PreAuthorize("isAuthenticated()")
    public List<ActiveChallengeResponseDTO> getFeaturedChallenges()
            throws WellnessTrackerException;
}
