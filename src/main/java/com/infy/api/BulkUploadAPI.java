package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.infy.dto.BulkUploadResponseDTO;
import com.infy.dto.BulkUploadResultDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.BulkUploadService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class BulkUploadAPI {

    @Autowired
    private BulkUploadService bulkUploadService;

    // US 14 - HR uploads a CSV file for bulk employee or baseline metric import.
    // uploadedBy param removed — caller identity derived from JWT inside service.
    // uploadType: EMPLOYEE or BASELINE_METRIC
    // file: multipart/form-data, must be a .csv file
    @PostMapping(value = "/bulk-uploads", consumes = "multipart/form-data")
    public ResponseEntity<BulkUploadResultDTO> uploadCsv(
            @RequestParam String uploadType,
            @RequestParam("file") MultipartFile file)
            throws WellnessTrackerException {
        BulkUploadResultDTO result = bulkUploadService.uploadCsv(uploadType, file);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // US 14 - Import history for the JWT caller (HR only), newest first.
    // Path changed from /bulk-uploads/users/{userId} to /bulk-uploads/mine.
    // Returns [] when no uploads exist.
    @GetMapping(value = "/bulk-uploads/mine")
    public ResponseEntity<List<BulkUploadResponseDTO>> getUploadHistory()
            throws WellnessTrackerException {
        List<BulkUploadResponseDTO> history = bulkUploadService.getUploadHistory();
        return new ResponseEntity<>(history, HttpStatus.OK);
    }

    // US 14 - Single upload record detail (ownership-guarded, HR only).
    // requestingUserId query param removed — ownership checked against JWT caller.
    @GetMapping(value = "/bulk-uploads/{bulkUploadId}")
    public ResponseEntity<BulkUploadResponseDTO> getUploadById(
            @PathVariable Integer bulkUploadId)
            throws WellnessTrackerException {
        BulkUploadResponseDTO response = bulkUploadService.getUploadById(bulkUploadId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
