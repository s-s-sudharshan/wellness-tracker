package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ActiveChallengeResponseDTO;
import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.dto.ChallengeUpdateRequestDTO;
import com.infy.dto.JoinChallengeRequestDTO;
import com.infy.dto.MyChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ChallengeParticipantService;
import com.infy.service.ChallengeService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class ChallengeAPI {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private ChallengeParticipantService participantService;

    @Autowired
    private Environment env;

    // US 13 - Create a new challenge (MANAGER or HR only — role gate on service).
    @PostMapping(value = "/challenges")
    public ResponseEntity<String> createChallenge(
            @Valid @RequestBody ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer challengeId = challengeService.createChallenge(requestDTO);
        String successMessage = env.getProperty("API.CREATE_CHALLENGE_SUCCESS") + challengeId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 13 - Edit an UPCOMING challenge.
    // requestingUserId removed from ChallengeUpdateRequestDTO — ownership verified via JWT.
    @PutMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<ChallengeResponseDTO> updateChallenge(
            @PathVariable Integer challengeId,
            @Valid @RequestBody ChallengeUpdateRequestDTO requestDTO)
            throws WellnessTrackerException {
        ChallengeResponseDTO updated = challengeService.updateChallenge(challengeId, requestDTO);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    // US 13 - Delete an UPCOMING challenge (no participants).
    // requestingUserId query param removed — ownership verified via JWT inside service.
    @DeleteMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<String> deleteChallenge(
            @PathVariable Integer challengeId)
            throws WellnessTrackerException {
        challengeService.deleteChallenge(challengeId);
        String successMessage = env.getProperty("API.DELETE_CHALLENGE_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 13 - Get all challenges created by a specific manager/HR (target userId in path).
    // managerId is the TARGET being viewed — not necessarily the JWT caller.
    @GetMapping(value = "/challenges/managers/{managerId}")
    public ResponseEntity<List<ChallengeResponseDTO>> getChallengesByManager(
            @PathVariable Integer managerId)
            throws WellnessTrackerException {
        List<ChallengeResponseDTO> challenges = challengeService.getChallengesByManager(managerId);
        return new ResponseEntity<>(challenges, HttpStatus.OK);
    }

    // US 13 / US 03 - Get a single challenge by ID.
    // requestingUserId query param removed — visibility check uses JWT caller inside service.
    @GetMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<ChallengeResponseDTO> getChallengeById(
            @PathVariable Integer challengeId)
            throws WellnessTrackerException {
        ChallengeResponseDTO challenge = challengeService.getChallengeById(challengeId);
        return new ResponseEntity<>(challenge, HttpStatus.OK);
    }

    // US 03 - Get all active/upcoming challenges visible to the JWT caller.
    // Path changed from /challenges/users/{userId} to /challenges/available.
    @GetMapping(value = "/challenges/available")
    public ResponseEntity<List<ActiveChallengeResponseDTO>> getActiveChallenges()
            throws WellnessTrackerException {
        List<ActiveChallengeResponseDTO> challenges = participantService.getActiveChallenges();
        return new ResponseEntity<>(challenges, HttpStatus.OK);
    }

    // US 07 - Get featured challenges visible to the JWT caller.
    // Path changed from /challenges/users/{userId}/featured to /challenges/featured.
    @GetMapping(value = "/challenges/featured")
    public ResponseEntity<List<ActiveChallengeResponseDTO>> getFeaturedChallenges()
            throws WellnessTrackerException {
        List<ActiveChallengeResponseDTO> featured = challengeService.getFeaturedChallenges();
        return new ResponseEntity<>(featured, HttpStatus.OK);
    }

    // US 03 - JWT caller joins a challenge.
    // userId removed from JoinChallengeRequestDTO — derived from JWT inside service.
    @PostMapping(value = "/challenges/join")
    public ResponseEntity<String> joinChallenge(
            @Valid @RequestBody JoinChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer participantId = participantService.joinChallenge(requestDTO);
        String successMessage = env.getProperty("API.JOIN_CHALLENGE_SUCCESS") + participantId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 03 - Get the JWT caller's joined challenges with live progress.
    // Path changed from /challenges/users/{userId}/my-challenges to /challenges/mine.
    @GetMapping(value = "/challenges/mine")
    public ResponseEntity<List<MyChallengeResponseDTO>> getMyChallenges()
            throws WellnessTrackerException {
        List<MyChallengeResponseDTO> myChallenges = participantService.getMyChallenges();
        return new ResponseEntity<>(myChallenges, HttpStatus.OK);
    }
}