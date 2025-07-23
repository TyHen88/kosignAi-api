package org.kosign.chatbotapi.payload.workflow;

import lombok.Data;

@Data
public class WorkflowFoundState {
    private String message;
    private WorkflowAction onFailure;
    private WorkflowAction onSuccess;
    private String actionType;
}
