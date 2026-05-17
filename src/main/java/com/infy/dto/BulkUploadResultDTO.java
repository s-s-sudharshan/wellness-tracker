package com.infy.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BulkUploadResultDTO {

    private Integer bulkUploadId;
    private String fileName;
    private String uploadType;
    private String status;

    // Row counts — importedRows and updatedRows are returned separately
    private Integer totalRows;
    private Integer importedRows;   // new records created
    private Integer updatedRows;    // existing records updated
    private Integer failedRows;     // rows skipped due to validation errors

    // Row-level error messages — e.g. "Row 3: email is missing"
    // Not persisted — only available in the immediate upload response.
    private List<String> errors;

    private LocalDateTime uploadedAt;
}
