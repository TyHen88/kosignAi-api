package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponse;
import org.kosign.chatbotapi.payload.workflow.WorkflowAction;
import org.kosign.chatbotapi.payload.workflow.WorkflowData;
import org.kosign.chatbotapi.service.ApiRequestToolsService.ApiRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for extracting and processing workflow messages
 */
@Service
@RequiredArgsConstructor
public class WorkflowMessageService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMessageService.class);
    private final ApiRequestService apiRequestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractWorkflowMessage(Object metadataData) {
        if (metadataData == null) {
            logger.warn("🟡 Metadata data is null, cannot extract workflow message.");
            return null;
        }

        try {
            String json = metadataData.toString();
            logger.debug("🟢 Parsing JSON: {}", json);

            try {
                List<Map<String, WorkflowData>> workflowArray = objectMapper.readValue(json, new TypeReference<>() {
                });
                logger.info("🟢 Successfully deserialized workflow as an array.");
                for (Map<String, WorkflowData> workflowMap : workflowArray) {
                    String message = extractMessageFromMap(workflowMap);
                    if (message != null) {
                        return message;
                    }
                }
            } catch (Exception e) {
                Map<String, WorkflowData> workflowMap = objectMapper.readValue(json, new TypeReference<>() {
                });
                logger.info("🟢 Successfully deserialized workflow as a map.");
                return extractMessageFromMap(workflowMap);
            }
        } catch (Exception e) {
            logger.warn("❌ Failed to extract workflow message: {}", e.getMessage(), e);
        }

        return null;
    }

    public String extractWorkflowInputType(Object metadataData) {
        if (metadataData == null) {
            return null;
        }

        try {
            String json = metadataData.toString();
            List<Map<String, WorkflowData>> workflowList = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, WorkflowData>>>() {
                    });

            for (Map<String, WorkflowData> workflowMap : workflowList) {
                for (Map.Entry<String, WorkflowData> entry : workflowMap.entrySet()) {
                    WorkflowData data = entry.getValue();

                    try {
                        if ("check_bakong".equals(data.getType())) {
                            String inputType = data.getInputType();
                            if (inputType != null && !inputType.isBlank()) {
                                return inputType;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to process workflow entry key: {}, error: {}",
                                entry.getKey(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract workflow input types from metadata data: " + metadataData, e);
        }

        return null;
    }

    private String extractMessageFromMap(Map<String, WorkflowData> workflowMap) {
        StringBuilder finalResponse = new StringBuilder();
        for (Map.Entry<String, WorkflowData> entry : workflowMap.entrySet()) {
            WorkflowData data = entry.getValue();
            String key = entry.getKey();

            try {
                switch (data.getType()) {
                    case "check_bakong":
                        if ("customize_message".equals(data.getFoundActionType())) {
                            finalResponse.append(data.getIsFound().getMessage());
                        } else if ("call_api".equals(data.getFoundActionType())) {
                            handleApiCall(data, finalResponse, key);
                        }
                        break;

                    case "call_3rd_party":
                        handleApiCall3rd(data, finalResponse, key);
                        break;

                    case "user_input_message":
                        if (data.getUserInput() != null) {
                            finalResponse.append("\n\n").append(data.getUserInput()).append("\n");
                        }
                        break;

                    default:
                        logger.warn("⚠️ Unknown workflow type: {}", data.getType());
                }
            } catch (Exception e) {
                logger.error("❌ Error processing workflow entry [{}]: {}", key, e.getMessage(), e);
            }
        }

        return finalResponse.length() > 0 ? finalResponse.toString().trim() : null;
    }

    private void handleApiCall3rd(WorkflowData data, StringBuilder finalResponse, String key) {
        String apiUrl = data.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            logger.warn("⚠️ Skipping workflow entry [{}] due to missing or invalid API URL.", key);
            return;
        }

        try {
            ApiResponse response = apiRequestService.executeRequest(apiUrl);

            if (response.getStatusCode() == 200) {
                finalResponse.append("\n").append("- Third-Party Integration Request \n\n");
                WorkflowAction onSuccess = data.getOnSuccess() != null ? data.getOnSuccess()
                        : data.getIsFound() != null ? data.getIsFound().getOnSuccess() : null;
                if (onSuccess != null && "customize_message".equals(onSuccess.getActionType())) {
                    finalResponse.append(onSuccess.getMessage()).append("\n");
                } else {
                    finalResponse.append("✅ Successfully retrieved data from the service.\n\n");
                }
            } else {
                finalResponse.append("❌ Failed to get response from API\n");
                WorkflowAction onFailure = data.getOnFailure() != null ? data.getOnFailure()
                        : data.getIsFound() != null ? data.getIsFound().getOnFailure() : null;
                if (onFailure != null && onFailure.getMessage() != null) {
                    finalResponse.append(onFailure.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("❌ Failed to execute API call [{}]: {}", apiUrl, e.getMessage(), e);
        }
    }

    private void handleApiCall(WorkflowData data, StringBuilder finalResponse, String key) {
        String apiUrl = data.getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            logger.warn("⚠️ Skipping workflow entry [{}] due to missing or invalid API URL.", key);
            return;
        }

        try {
            ApiResponse response = apiRequestService.executeRequest(apiUrl);

            if (response.getStatusCode() == 200) {
                finalResponse.append("\n").append("- Call API Customize \n\n");
                WorkflowAction onSuccess = data.getOnSuccess() != null ? data.getOnSuccess()
                        : data.getIsFound() != null ? data.getIsFound().getOnSuccess() : null;
                if (onSuccess != null && "customize_message".equals(onSuccess.getActionType())) {
                    finalResponse.append(onSuccess.getMessage()).append("\n");
                } else {
                    finalResponse.append("✅ Successfully retrieved data from the service.\n\n");
                }
            } else {
                finalResponse.append("❌ Failed to get response from API\n");
                WorkflowAction onFailure = data.getOnFailure() != null ? data.getOnFailure()
                        : data.getIsFound() != null ? data.getIsFound().getOnFailure() : null;
                if (onFailure != null && onFailure.getMessage() != null) {
                    finalResponse.append(onFailure.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("❌ Failed to execute API call [{}]: {}", apiUrl, e.getMessage(), e);
        }
    }
}
