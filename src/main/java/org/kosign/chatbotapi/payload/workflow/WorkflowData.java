package org.kosign.chatbotapi.payload.workflow;

import lombok.Data;

@Data
public class WorkflowData {
    private String id;
    private String type;
    private String token;
    private String apiUrl;
    private String apiCall;
    private String authType;
    private String inputType;
    private String userInput;
    private String failMessage;
    private String failedToken;
    private String failedApiUrl;
    private String customMessage;
    private String failedMessage;
    private String destinationApi;
    private String ifFailedAction;
    private String successMessage;
    private String foundActionType;
    private String ifSuccessAction;
    private String notFoundMessage;
    private String notFoundActionType;
    private String foundSuccessMessage;

    private WorkflowAction onFailure;
    private WorkflowAction onSuccess;
    private WorkflowAction onNotFound;
}
