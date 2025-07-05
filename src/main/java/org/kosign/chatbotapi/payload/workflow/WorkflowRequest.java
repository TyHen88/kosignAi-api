package org.kosign.chatbotapi.payload.workflow;


import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowRequest {
    private String title;
    private String imageUrl;
    private String category;
    private String goalStatement;

    @Builder
    public WorkflowRequest(String title, String imageUrl, String category, String goalStatement) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.category = category;
        this.goalStatement = goalStatement;
    }
}
