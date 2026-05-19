package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.BadgeRequestDTO;
import com.infy.dto.BadgeResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface BadgeService {

    // Manager or HR creates a new badge.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public Integer createBadge(BadgeRequestDTO requestDTO) throws WellnessTrackerException;

    // Manager or HR edits an existing badge.
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public BadgeResponseDTO updateBadge(Integer badgeId, BadgeRequestDTO requestDTO)
            throws WellnessTrackerException;

    // Admin view — all badges with no user context.
    @PreAuthorize("isAuthenticated()")
    public List<BadgeResponseDTO> getAllBadges() throws WellnessTrackerException;

    // Employee view — all badges with live progress for the JWT caller.
    // userId removed — caller identity derived from JWT inside implementation.
    @PreAuthorize("isAuthenticated()")
    public List<BadgeResponseDTO> getUserBadges() throws WellnessTrackerException;
}
