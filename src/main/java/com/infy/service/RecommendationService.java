package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.RecommendationResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface RecommendationService {

    // US 08 - Get personalised recommendations for the JWT caller.
    // Rule engine runs on every call so the list reflects the latest activity data.
    // Returns 3–5 recommendations ordered by relevance priority.
    // userId removed — derived from JWT inside implementation.
    @PreAuthorize("isAuthenticated()")
    public List<RecommendationResponseDTO> getRecommendations()
            throws WellnessTrackerException;
}
