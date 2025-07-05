package org.kosign.chatbotapi.payload.workflow;

import org.springframework.beans.factory.annotation.Value;

public interface IGetWorkflow {
    @Value("#{target.id}")
    Long getId();

    @Value("#{target.title}")
    String getTitle();

    @Value("#{target.goal_statement}")
    String getGoalStatement();

    @Value("#{target.image_url}")
    String getImageUrl();

    @Value("#{target.created_at}")
    String getCreatedAt();

    @Value("#{target.updated_at}")
    String getUpdatedAt();

    @Value("#{target.category}")
    String getCategory();
}
