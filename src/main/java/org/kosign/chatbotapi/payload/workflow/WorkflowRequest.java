package org.kosign.chatbotapi.payload.workflow;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowRequest {
    private String title;
    private String imageUrl;
    private String categoryName;
    private String goalStatement;
    private Map<String, Object> metadata;


    @Builder
    public WorkflowRequest(String title, String imageUrl, String categoryName, String goalStatement, Map<String, Object> metadata) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.goalStatement = goalStatement;
        this.metadata = metadata;
    }
}
