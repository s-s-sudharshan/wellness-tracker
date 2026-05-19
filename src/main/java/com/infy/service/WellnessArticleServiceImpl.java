package com.infy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.WellnessArticleRequestDTO;
import com.infy.dto.WellnessArticleResponseDTO;
import com.infy.entity.User;
import com.infy.entity.WellnessArticle;
import com.infy.enums.WellnessArticleStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.WellnessArticleRepository;
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class WellnessArticleServiceImpl implements WellnessArticleService {

    @Autowired
    private WellnessArticleRepository articleRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // Author identity derived entirely from JWT — createdBy removed from DTO.
    // @PreAuthorize("hasRole('HR')") enforced on interface.
    @Override
    public Integer createArticle(WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {

        // Author is always the JWT caller — no DTO field to cross-check against
        User caller = authenticatedUserResolver.resolveCurrentUser();

        WellnessArticle article = new WellnessArticle();
        article.setCreatedBy(caller);
        article.setTitle(requestDTO.getTitle());
        article.setDescription(requestDTO.getDescription());
        article.setArticleUrl(requestDTO.getArticleUrl());
        article.setRelatedMetric(requestDTO.getRelatedMetric());
        article.setStatus(requestDTO.getStatus() != null
                ? requestDTO.getStatus()
                : WellnessArticleStatus.DRAFT);

        return articleRepository.save(article).getArticleId();
    }

    // @PreAuthorize("hasRole('HR')") enforced on interface.
    // Ownership check: only the author can edit their own article.
    @Override
    public WellnessArticleResponseDTO updateArticle(
            Integer articleId, WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {

        // Caller identity from JWT — no DTO field to cross-check against
        User caller = authenticatedUserResolver.resolveCurrentUser();

        Optional<WellnessArticle> optional = articleRepository.findById(articleId);
        WellnessArticle article = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.ARTICLE_NOT_FOUND"));

        // Ownership check using JWT caller — not a client-supplied createdBy
        if (!article.getCreatedBy().getUserId().equals(caller.getUserId())) {
            throw new WellnessTrackerException("Service.ARTICLE_ACCESS_DENIED");
        }

        article.setTitle(requestDTO.getTitle());
        article.setDescription(requestDTO.getDescription());
        article.setArticleUrl(requestDTO.getArticleUrl());
        article.setRelatedMetric(requestDTO.getRelatedMetric());
        if (requestDTO.getStatus() != null) {
            article.setStatus(requestDTO.getStatus());
        }

        return mapToDTO(articleRepository.save(article));
    }

    // Returns the JWT caller's own articles (HR only — role gate on interface).
    @Override
    @Transactional(readOnly = true)
    public List<WellnessArticleResponseDTO> getArticlesByHr()
            throws WellnessTrackerException {

        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        List<WellnessArticle> articles =
                articleRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(callerId);

        if (articles.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_ARTICLES_FOUND");
        }

        List<WellnessArticleResponseDTO> response = new ArrayList<>();
        for (WellnessArticle a : articles) {
            response.add(mapToDTO(a));
        }
        return response;
    }

    private WellnessArticleResponseDTO mapToDTO(WellnessArticle a) {
        WellnessArticleResponseDTO dto = new WellnessArticleResponseDTO();
        dto.setArticleId(a.getArticleId());
        dto.setCreatedBy(a.getCreatedBy().getUserId());
        dto.setCreatedByName(
                a.getCreatedBy().getFirstName() + " " + a.getCreatedBy().getLastName());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setArticleUrl(a.getArticleUrl());
        dto.setRelatedMetric(a.getRelatedMetric());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
