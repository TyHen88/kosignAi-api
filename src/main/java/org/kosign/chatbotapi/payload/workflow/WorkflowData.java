package org.kosign.chatbotapi.payload.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowData {
    private String id;
    private String type;
    private String token;
    private String apiUrl;
    private String authKey;
    private String authType;
    private String authValue;
    private String inputType;
    private String httpMethod;
    private String destinationApi;
    private String foundActionType;
    private String userInput;

    private WorkflowAction onNotFound;
    private WorkflowFoundState isFound;
    private WorkflowAction onSuccess;
    private WorkflowAction onFailure;

}
