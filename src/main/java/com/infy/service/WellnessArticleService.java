package com.infy.service;

import java.util.List;

import com.infy.dto.WellnessArticleRequestDTO;
import com.infy.dto.WellnessArticleResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface WellnessArticleService {

    // HR creates a new article (status defaults to DRAFT)
    public Integer createArticle(WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException;

    // HR updates an existing article (title, description, url, metric, status)
    public WellnessArticleResponseDTO updateArticle(
            Integer articleId, WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException;

    // Get all articles created by a specific HR user
    public List<WellnessArticleResponseDTO> getArticlesByHr(Integer userId)
            throws WellnessTrackerException;
}
