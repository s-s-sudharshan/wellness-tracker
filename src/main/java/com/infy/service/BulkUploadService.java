package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.infy.dto.BulkUploadResponseDTO;
import com.infy.dto.BulkUploadResultDTO;
import com.infy.exception.WellnessTrackerException;

public interface BulkUploadService {

    // US 14 - Upload and process a CSV file (HR only).
    // uploadedBy param removed — caller identity derived from JWT inside implementation.
    // uploadType must be "EMPLOYEE" or "BASELINE_METRIC".
    // Returns an immediate result with row counts and any row-level errors.
    @PreAuthorize("hasRole('HR')")
    public BulkUploadResultDTO uploadCsv(String uploadType, MultipartFile file)
            throws WellnessTrackerException;

    // US 14 - Import history for the JWT caller (HR only).
    // userId param removed — derived from JWT.
    // Returns [] when no uploads exist — not an error.
    @PreAuthorize("hasRole('HR')")
    public List<BulkUploadResponseDTO> getUploadHistory() throws WellnessTrackerException;

    // US 14 - Single upload record detail (ownership-guarded, HR only).
    // requestingUserId param removed — ownership checked against JWT caller.
    @PreAuthorize("hasRole('HR')")
    public BulkUploadResponseDTO getUploadById(Integer bulkUploadId)
            throws WellnessTrackerException;
}
