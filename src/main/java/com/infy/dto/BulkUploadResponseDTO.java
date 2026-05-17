package com.infy.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BulkUploadResponseDTO {

    private Integer bulkUploadId;
    private Integer uploadedBy;
    private String uploadedByName;
    private String uploadType;
    private String fileName;
    private String status;

    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;

    private LocalDateTime uploadedAt;
}
