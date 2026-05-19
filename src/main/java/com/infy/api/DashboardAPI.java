package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    // US 06 - Get aggregated dashboard data for the JWT caller.
    // Path changed from /dashboard/users/{userId} to /dashboard/mine.
    // userId removed — derived from JWT inside service.
    @GetMapping(value = "/dashboard/mine")
    public ResponseEntity<DashboardResponseDTO> getDashboard()
            throws WellnessTrackerException {
        DashboardResponseDTO response = dashboardService.getDashboard();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}