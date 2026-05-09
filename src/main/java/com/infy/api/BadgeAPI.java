package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.BadgeRequestDTO;
import com.infy.dto.BadgeResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.BadgeService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class BadgeAPI {

    @Autowired
    private BadgeService badgeService;

    @Autowired
    private Environment env;

    // Manager/HR — create a new badge
    @PostMapping(value = "/badges")
    public ResponseEntity<String> createBadge(
            @Valid @RequestBody BadgeRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer badgeId = badgeService.createBadge(requestDTO);
        String successMessage = env.getProperty("API.CREATE_BADGE_SUCCESS") + badgeId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // Manager/HR — edit an existing badge
    @PutMapping(value = "/badges/{badgeId}")
    public ResponseEntity<BadgeResponseDTO> updateBadge(
            @PathVariable Integer badgeId,
            @Valid @RequestBody BadgeRequestDTO requestDTO)
            throws WellnessTrackerException {
        BadgeResponseDTO response = badgeService.updateBadge(badgeId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Admin — list all badges with no user context
    @GetMapping(value = "/badges")
    public ResponseEntity<List<BadgeResponseDTO>> getAllBadges()
            throws WellnessTrackerException {
        List<BadgeResponseDTO> badges = badgeService.getAllBadges();
        return new ResponseEntity<>(badges, HttpStatus.OK);
    }

    // Employee — get all badges with live progress for a specific user
    // Returns flat list sorted: EARNED → IN_PROGRESS → LOCKED
    // newlyUnlocked=true on any badge that was just awarded — use this for toast
    @GetMapping(value = "/badges/users/{userId}")
    public ResponseEntity<List<BadgeResponseDTO>> getUserBadges(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<BadgeResponseDTO> badges = badgeService.getUserBadges(userId);
        return new ResponseEntity<>(badges, HttpStatus.OK);
    }
}
