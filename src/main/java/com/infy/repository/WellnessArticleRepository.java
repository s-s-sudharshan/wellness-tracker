package com.infy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.WellnessArticle;
import com.infy.enums.ActivityType;
import com.infy.enums.WellnessArticleStatus;

public interface WellnessArticleRepository extends CrudRepository<WellnessArticle, Integer> {

    // All articles published by a specific HR user
    List<WellnessArticle> findByCreatedBy_UserIdOrderByCreatedAtDesc(Integer userId);

    // Published articles for a specific metric — used by recommendation rule engine
    // as the article fallback when no matching challenge is available
    List<WellnessArticle> findByRelatedMetricAndStatusOrderByCreatedAtDesc(
            ActivityType relatedMetric, WellnessArticleStatus status);

    // Published general articles (relatedMetric is null) — used for padding in Pass B
    @Query("SELECT a FROM WellnessArticle a " +
           "WHERE a.relatedMetric IS NULL " +
           "AND a.status = :status " +
           "ORDER BY a.createdAt DESC")
    List<WellnessArticle> findGeneralPublishedArticles(
            @Param("status") WellnessArticleStatus status);
}
