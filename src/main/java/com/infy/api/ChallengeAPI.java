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
import org.springframework.web.bind.annotation.RequestParam;
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

    // US 13 - Create a new challenge
    @PostMapping(value = "/challenges")
    public ResponseEntity<String> createChallenge(@Valid @RequestBody ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer challengeId = challengeService.createChallenge(requestDTO);
        String successMessage = env.getProperty("API.CREATE_CHALLENGE_SUCCESS") + challengeId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }
    
    // US 13 - Edits an UPCOMING challenge they created
    // requestingUserId is included in the body (ChallengeUpdateRequestDTO) for ownership check
    @PutMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<ChallengeResponseDTO> updateChallenge(
            @PathVariable Integer challengeId,
            @Valid @RequestBody ChallengeUpdateRequestDTO requestDTO)
            throws WellnessTrackerException {
        ChallengeResponseDTO updated = challengeService.updateChallenge(challengeId, requestDTO);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }
    
    // US 13 - Deletes an UPCOMING challenge they created (no participants)
    // requestingUserId passed as query param — avoids a wrapper request body for a DELETE
    @DeleteMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<String> deleteChallenge(
            @PathVariable Integer challengeId,
            @RequestParam Integer requestingUserId)
            throws WellnessTrackerException {
        challengeService.deleteChallenge(challengeId, requestingUserId);
        String successMessage = env.getProperty("API.DELETE_CHALLENGE_SUCCESS");
        return new ResponseEntity<>(successMessage, HttpStatus.OK);
    }

    // US 13 - Get all challenges they created
    @GetMapping(value = "/challenges/managers/{managerId}")
    public ResponseEntity<List<ChallengeResponseDTO>> getChallengesByManager(
            @PathVariable Integer managerId)
            throws WellnessTrackerException {
        List<ChallengeResponseDTO> challenges = challengeService.getChallengesByManager(managerId);
        return new ResponseEntity<>(challenges, HttpStatus.OK);
    }

     // US 13 / US 03 - Get a single challenge by ID
 	 // requestingUserId enforces visibility — DEPARTMENT challenges blocked for cross-dept users
 	 @GetMapping(value = "/challenges/{challengeId}")
 	 public ResponseEntity<ChallengeResponseDTO> getChallengeById(
 	         @PathVariable Integer challengeId,
 	         @RequestParam Integer requestingUserId)
 	         throws WellnessTrackerException {
 	     ChallengeResponseDTO challenge = challengeService.getChallengeById(
 	             challengeId, requestingUserId);
 	     return new ResponseEntity<>(challenge, HttpStatus.OK);
 	 }

    // US 03 - Get all active/upcoming challenges visible to a user
    @GetMapping(value = "/challenges/users/{userId}")
    public ResponseEntity<List<ActiveChallengeResponseDTO>> getActiveChallenges(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<ActiveChallengeResponseDTO> challenges = participantService.getActiveChallenges(userId);
        return new ResponseEntity<>(challenges, HttpStatus.OK);
    }
    
    // US 07 - Get featured challenges visible to a user
    @GetMapping(value = "/challenges/users/{userId}/featured")
    public ResponseEntity<List<ActiveChallengeResponseDTO>> getFeaturedChallenges(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<ActiveChallengeResponseDTO> featured = challengeService.getFeaturedChallenges(userId);
        return new ResponseEntity<>(featured, HttpStatus.OK);
    }

    // US 03 - Employee joins a challenge
    @PostMapping(value = "/challenges/join")
    public ResponseEntity<String> joinChallenge(@Valid @RequestBody JoinChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer participantId = participantService.joinChallenge(requestDTO);
        String successMessage = env.getProperty("API.JOIN_CHALLENGE_SUCCESS") + participantId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }
    
    // US 03 - Get My Challenges (all joined challenges with live progress)
    @GetMapping(value = "/challenges/users/{userId}/my-challenges")
    public ResponseEntity<List<MyChallengeResponseDTO>> getMyChallenges(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<MyChallengeResponseDTO> myChallenges = participantService.getMyChallenges(userId);
        return new ResponseEntity<>(myChallenges, HttpStatus.OK);
    }
}
