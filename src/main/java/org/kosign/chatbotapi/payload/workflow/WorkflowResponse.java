package org.kosign.chatbotapi.payload.workflow;

import java.util.List;

import org.kosign.chatbotapi.components.common.Pagination;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkflowResponse {
    private Long id;
    private String title;
    private String goalStatement;
    private String imageUrl;
    private String createdAt;
    private String updatedAt;
    private String category;

    @Builder
    public WorkflowResponse(Long id, String title, String goalStatement, String imageUrl, String createdAt, String updatedAt, String category) {
        this.id = id;
        this.title = title;
        this.goalStatement = goalStatement;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.category = category;
    }
}
