package org.kosign.chatbotapi.payload.workflow;

import lombok.Data;

@Data
public class WorkflowAction {
    private String token;
    private String apiUrl;
    private String message;
    private String actionType;
}
