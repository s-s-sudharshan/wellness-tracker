package com.infy.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.WellnessArticleRequestDTO;
import com.infy.dto.WellnessArticleResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.WellnessArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wellness")
public class WellnessArticleAPI {

    @Autowired
    private WellnessArticleService articleService;

    @Autowired
    private Environment env;

    // HR creates a new wellness article (defaults to DRAFT)
    // Set status = PUBLISHED in the request body to make it immediately live
    @PostMapping(value = "/articles")
    public ResponseEntity<String> createArticle(
            @Valid @RequestBody WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {
        Integer articleId = articleService.createArticle(requestDTO);
        String successMessage = env.getProperty("API.CREATE_ARTICLE_SUCCESS") + articleId;
        return new ResponseEntity<>(successMessage, HttpStatus.CREATED);
    }

    // HR updates an existing article (including publishing a draft)
    @PutMapping(value = "/articles/{articleId}")
    public ResponseEntity<WellnessArticleResponseDTO> updateArticle(
            @PathVariable Integer articleId,
            @Valid @RequestBody WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {
        WellnessArticleResponseDTO response = articleService.updateArticle(articleId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Get all articles created by a specific HR user (both DRAFT and PUBLISHED)
    @GetMapping(value = "/articles/hr/{userId}")
    public ResponseEntity<List<WellnessArticleResponseDTO>> getArticlesByHr(
            @PathVariable Integer userId)
            throws WellnessTrackerException {
        List<WellnessArticleResponseDTO> articles = articleService.getArticlesByHr(userId);
        return new ResponseEntity<>(articles, HttpStatus.OK);
    }
}
