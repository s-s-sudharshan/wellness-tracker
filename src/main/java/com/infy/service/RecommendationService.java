package com.infy.service;

import java.util.List;

import com.infy.dto.RecommendationResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface RecommendationService {

    // US 08 - Get personalised recommendations for a user.
    // Rule engine runs on every call so the list reflects the latest activity data.
    // Returns 3–5 recommendations ordered by relevance priority.
    public List<RecommendationResponseDTO> getRecommendations(Integer userId)
            throws WellnessTrackerException;
}
