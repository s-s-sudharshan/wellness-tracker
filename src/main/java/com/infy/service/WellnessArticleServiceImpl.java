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
import com.infy.enums.Role;
import com.infy.enums.WellnessArticleStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.UserRepository;
import com.infy.repository.WellnessArticleRepository;

@Service
@Transactional
public class WellnessArticleServiceImpl implements WellnessArticleService {

    @Autowired
    private WellnessArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Integer createArticle(WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {

        User author = resolveHrUser(requestDTO.getCreatedBy());

        WellnessArticle article = new WellnessArticle();
        article.setCreatedBy(author);
        article.setTitle(requestDTO.getTitle());
        article.setDescription(requestDTO.getDescription());
        article.setArticleUrl(requestDTO.getArticleUrl());
        article.setRelatedMetric(requestDTO.getRelatedMetric());
        // Honour explicit status from request; entity @PrePersist defaults to DRAFT
        article.setStatus(requestDTO.getStatus() != null
                ? requestDTO.getStatus()
                : WellnessArticleStatus.DRAFT);

        return articleRepository.save(article).getArticleId();
    }

    @Override
    public WellnessArticleResponseDTO updateArticle(
            Integer articleId, WellnessArticleRequestDTO requestDTO)
            throws WellnessTrackerException {

        // Validate the requesting user is HR
        resolveHrUser(requestDTO.getCreatedBy());

        Optional<WellnessArticle> optional = articleRepository.findById(articleId);
        WellnessArticle article = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.ARTICLE_NOT_FOUND"));

        // Only the author can edit their own article
        if (!article.getCreatedBy().getUserId().equals(requestDTO.getCreatedBy())) {
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

    @Override
    @Transactional(readOnly = true)
    public List<WellnessArticleResponseDTO> getArticlesByHr(Integer userId)
            throws WellnessTrackerException {

        resolveHrUser(userId);

        List<WellnessArticle> articles =
                articleRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(userId);

        if (articles.isEmpty()) {
            throw new WellnessTrackerException("Service.NO_ARTICLES_FOUND");
        }

        List<WellnessArticleResponseDTO> response = new ArrayList<>();
        for (WellnessArticle a : articles) {
            response.add(mapToDTO(a));
        }
        return response;
    }

    // Validates user exists and is HR. Throws appropriate exceptions otherwise.
    private User resolveHrUser(Integer userId) throws WellnessTrackerException {
        Optional<User> optional = userRepository.findById(userId);
        User user = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));
        if (!Role.HR.equals(user.getRole())) {
            throw new WellnessTrackerException("Service.NOT_HR");
        }
        return user;
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
