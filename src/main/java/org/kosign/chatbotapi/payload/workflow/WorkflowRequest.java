package org.kosign.chatbotapi.payload.workflow;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kosign.chatbotapi.payload.openApi.OpenApiRequest;

import java.util.List;
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
    private List<OpenApiRequest> openApiRequests;


    @Builder
    public WorkflowRequest(String title, String imageUrl, String categoryName, String goalStatement, Map<String, Object> metadata, List<OpenApiRequest> openApiRequests) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.goalStatement = goalStatement;
        this.metadata = metadata;
        this.openApiRequests = openApiRequests;
    }
}
