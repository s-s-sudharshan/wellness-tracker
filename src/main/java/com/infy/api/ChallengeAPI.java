package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ChallengeRequestDTO;
import com.infy.dto.ChallengeResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.ChallengeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wellness")
public class ChallengeAPI {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private Environment env;

    // US 13 - Manager creates a new challenge
    @PostMapping(value = "/challenges")
    public ResponseEntity<String> createChallenge(@Valid @RequestBody ChallengeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer challengeId = challengeService.createChallenge(requestDTO);
        String successMessage = env.getProperty("API.CREATE_CHALLENGE_SUCCESS") + challengeId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // US 13 - Get all challenges created by a manager
    @GetMapping(value = "/challenges/managers/{managerId}")
    public ResponseEntity<List<ChallengeResponseDTO>> getChallengesByManager(@PathVariable Integer managerId)
            throws WellnessTrackerException {
        List<ChallengeResponseDTO> challenges = challengeService.getChallengesByManager(managerId);
        return new ResponseEntity<>(challenges, HttpStatus.OK);
    }

    // US 03 (prep) - Get a single challenge by ID
    @GetMapping(value = "/challenges/{challengeId}")
    public ResponseEntity<ChallengeResponseDTO> getChallengeById(@PathVariable Integer challengeId)
            throws WellnessTrackerException {
        ChallengeResponseDTO challenge = challengeService.getChallengeById(challengeId);
        return new ResponseEntity<>(challenge, HttpStatus.OK);
    }
}
