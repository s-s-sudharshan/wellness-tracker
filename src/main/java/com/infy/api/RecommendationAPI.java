package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.RecommendationResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.RecommendationService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class RecommendationAPI {

    @Autowired
    private RecommendationService recommendationService;

    // US 08 - Get personalised wellness recommendations for the JWT caller.
    // Path changed from /recommendations/users/{userId} to /recommendations/mine.
    // userId removed — derived from JWT inside service.
    @GetMapping(value = "/recommendations/mine")
    public ResponseEntity<List<RecommendationResponseDTO>> getRecommendations()
            throws WellnessTrackerException {
        List<RecommendationResponseDTO> recommendations =
                recommendationService.getRecommendations();
        return new ResponseEntity<>(recommendations, HttpStatus.OK);
    }
}