package org.kosign.chatbotapi.payload.workflow;

import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public interface IGetWorkflow {
    @Value("#{target.id}")
    Long getId();

    @Value("#{target.title}")
    String getTitle();

    @Value("#{target.goalStatement}")
    String getGoalStatement();

    @Value("#{target.imageUrl}")
    String getImageUrl();

    @Value("#{target.createdAt}")
    String getCreatedAt();

    @Value("#{target.updatedAt}")
    String getUpdatedAt();

    @Value("#{target.categoryId}")
    Long getCategoryId();

    @Value("#{target.categoryName}")
    String getCategoryName();

    @Value("#{target.metadata}")
    String getMetadata();
}
