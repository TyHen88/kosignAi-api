package org.kosign.chatbotapi.model;


import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Represents a response from the AI service that may contain either a direct text response
 * or a request to call a specific tool (function) with parameters.
 */
@Data
@Builder
public class AiToolCallResponse {

    /**
     * The direct text response from the AI. This is used when no tool is called.
     * e.g., "I can help with that. What is the transaction hash?"
     */
    private String textResponse;

    /**
     * The name of the tool the AI wants to execute.
     * e.g., "check_transaction_status"
     */
    private String toolName;

    /**
     * A map of parameters for the tool call, extracted from the user's query.
     * e.g., {"hash": "c250339a", "amount": 50, "currency": "USD"}
     */
    private Map<String, Object> toolParameters;

    /**
     * A convenience method to check if the AI's response includes a valid tool call request.
     *
     * @return true if a tool name and parameters are present, false otherwise.
     */
    public boolean hasToolCall() {
        return toolName != null && !toolName.isEmpty() && toolParameters != null && !toolParameters.isEmpty();
    }
}