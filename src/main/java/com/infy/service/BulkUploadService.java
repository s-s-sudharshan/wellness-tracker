package com.infy.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.infy.dto.BulkUploadResponseDTO;
import com.infy.dto.BulkUploadResultDTO;
import com.infy.exception.WellnessTrackerException;

public interface BulkUploadService {

    // US 14 - Upload and process a CSV file.
    // uploadType must be "EMPLOYEE" or "BASELINE_METRIC".
    // Returns an immediate result with row counts and any row-level errors.
    // Row-level errors are not persisted — only available in this response.
    public BulkUploadResultDTO uploadCsv(Integer uploadedBy, String uploadType,
            MultipartFile file) throws WellnessTrackerException;

    // US 14 - Import history for a specific HR user, newest first.
    // Returns [] when no uploads exist — not an error.
    public List<BulkUploadResponseDTO> getUploadHistory(Integer userId)
            throws WellnessTrackerException;

    // US 14 - Single upload record detail (summary only, no row-level errors).
    // Ownership-guarded: requestingUserId must match the uploader.
    public BulkUploadResponseDTO getUploadById(Integer bulkUploadId,
            Integer requestingUserId) throws WellnessTrackerException;
}
