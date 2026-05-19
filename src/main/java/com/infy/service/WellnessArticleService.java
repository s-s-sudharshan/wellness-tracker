package com.infy.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.infy.dto.WellnessArticleRequestDTO;
import com.infy.dto.WellnessArticleResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface WellnessArticleService {

    // HR creates a new article (status defaults to DRAFT).
    @PreAuthorize("hasRole('HR')")
    public Integer createArticle(WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException;

    // HR updates an existing article (title, description, url, metric, status).
    @PreAuthorize("hasRole('HR')")
    public WellnessArticleResponseDTO updateArticle(
            Integer articleId, WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException;

    // Get all articles created by the JWT caller (HR only).
    // userId removed — derived from JWT inside implementation.
    @PreAuthorize("hasRole('HR')")
    public List<WellnessArticleResponseDTO> getArticlesByHr()
            throws WellnessTrackerException;
}
