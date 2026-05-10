package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.DashboardResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.DashboardService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class DashboardAPI {

    @Autowired
    private DashboardService dashboardService;

    // US 06 - Get aggregated dashboard data for a user
    @GetMapping(value = "/dashboard/users/{userId}")
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        DashboardResponseDTO response = dashboardService.getDashboard(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
