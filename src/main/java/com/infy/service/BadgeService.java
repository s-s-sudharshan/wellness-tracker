package com.infy.service;

import java.util.List;

import com.infy.dto.BadgeRequestDTO;
import com.infy.dto.BadgeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface BadgeService {

    // Manager/HR creates a new badge
    public Integer createBadge(BadgeRequestDTO requestDTO) throws WellnessTrackerException;

    // Manager/HR edits an existing badge
    public BadgeResponseDTO updateBadge(Integer badgeId, BadgeRequestDTO requestDTO)
            throws WellnessTrackerException;

    // Admin view — all badges with no user context
    public List<BadgeResponseDTO> getAllBadges() throws WellnessTrackerException;

    // Employee view — all badges with live progress for a specific user
    public List<BadgeResponseDTO> getUserBadges(Integer userId) throws WellnessTrackerException;
}
