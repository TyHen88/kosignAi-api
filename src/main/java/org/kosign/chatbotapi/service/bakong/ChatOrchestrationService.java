//package org.kosign.chatbotapi.service.bakong;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.kosign.chatbotapi.model.AiToolCallResponse;
//import org.kosign.chatbotapi.model.ConversationContext;
//import org.kosign.chatbotapi.service.AIService;
//import org.kosign.chatbotapi.service.BankingDomainService;
//import org.kosign.chatbotapi.service.SearchService;
//import org.kosign.chatbotapi.service.TransactionAIService;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * Orchestrates the conversation with the AI, handling general queries and tool-use (function calling).
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ChatOrchestrationService {
//
//    private final AIService aiService;
//    private final TransactionAIService transactionAIService;
//    private final SearchService searchService;
//    private final BankingDomainService bankingDomainService;
//
//    /**
//     * Main entry point for processing a user's query.
//     */
//    public String processUserQuery(String userQuery, ConversationContext context) {
//        try {
//            // 1. Determine if the query is about a transaction
//            if (transactionAIService.isTransactionInquiry(userQuery)) {
//                log.debug("Transaction inquiry detected. Initiating tool-calling workflow.");
//                return handleTransactionWorkflow(userQuery, context);
//            } else {
//                // 2. Handle as a general banking query using the search service
//                log.debug("General banking query detected.");
//                return handleGeneralQuery(userQuery, context);
//            }
//        } catch (Exception e) {
//            log.error("Error processing user query: {}", e.getMessage(), e);
//            return "I'm sorry, but I encountered an unexpected error. Please try again in a moment.";
//        }
//    }
//
//    /**
//     * Manages the multi-step conversation for checking a transaction status using AI tool-calling.
//     */
//    private String handleTransactionWorkflow(String userQuery, ConversationContext context) throws Exception {
//        // This simulates a multi-turn conversation with the AI.
//        // First, call the AI with the user's query and the definition of our available tool.
//        AiToolCallResponse aiResponse = aiService.getAiResponseWithTools(userQuery, context);
//
//        // Check if the AI wants to call our tool.
//        if (aiResponse.hasToolCall()) {
//            log.info("AI requested to call a tool: {}", aiResponse.getToolName());
//            // Currently, we only have one tool.
//            if ("check_transaction_status".equals(aiResponse.getToolName())) {
//                Map<String, Object> params = aiResponse.getToolParameters();
//                String hash = (String) params.get("hash");
//                // AI might return amount as Integer or Double, so we handle it as String.
//                String amount = String.valueOf(params.get("amount"));
//                String currency = (String) params.get("currency");
//
//                // Execute the actual Java service method.
//                String toolResult = transactionAIService.checkTransactionStatus(hash, amount, currency);
//                log.debug("Tool execution result: {}", toolResult);
//
//                // Send the result back to the AI so it can formulate a final, user-friendly response.
//                return aiService.getAiResponseAfterToolExecution(userQuery, context, toolResult);
//            }
//        }
//
//        // If the AI didn't call a tool, it's likely asking the user for more information.
//        // Return its response directly.
//        log.info("AI did not request a tool call. Returning its direct response.");
//        return aiResponse.getTextResponse();
//    }
//
//    /**
//     * Handles general queries by searching the knowledge base.
//     */
//    private String handleGeneralQuery(String userQuery, ConversationContext context) throws Exception {
//        List<String> keywords = bankingDomainService.extractSmartKeywords(userQuery);
//        context.setLastKeywords(keywords);
//
//        var searchResults = searchService.performIntelligentSearch(keywords, userQuery);
//        if (searchResults.isEmpty()) {
//            return "I couldn't find specific information about that. Could you please rephrase your question?";
//        }
//
//        // Build context for the AI and get a summarized response.
//        // (This part of the logic would be in your AIService)
//        return aiService.getSummarizedResponse(userQuery, searchResults, context);
//    }
//}