package com.infy.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.BulkUpload;

public interface BulkUploadRepository extends CrudRepository<BulkUpload, Integer> {

    // US 14 - Import history for a specific HR user, newest first
    List<BulkUpload> findByUploadedBy_UserIdOrderByUploadedAtDesc(Integer userId);
}
