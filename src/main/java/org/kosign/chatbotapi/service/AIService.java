package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.model.AiToolCallResponse;
import org.kosign.chatbotapi.model.ConversationContext;
import org.kosign.chatbotapi.repository.PPCBankContentRepository;
import org.kosign.chatbotapi.util.PromptBuilder;
import org.kosign.chatbotapi.util.QueryAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private enum AiProvider {
        OPENAI,
        GEMINI,
        ANTHROPIC,
        DEEPSEEK
    }

    // --- Configuration Properties ---

    @Value("${ai.provider}")
    private AiProvider activeProvider;

    @Value("${ai.openai.api-key}")
    private String openAIApiKey;

    @Value("${ai.openai.model}")
    private String openAIModel;

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.model}")
    private String geminiModel;

    @Value("${ai.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${ai.anthropic.model}")
    private String anthropicModel;

    @Value("${ai.deepseek.api-key}")
    private String deepSeekApiKey;

    @Value("${ai.deepseek.model}")
    private String deepSeekModel;

    @Autowired
    private PPCBankContentRepository pageContentRepository;
    
    @Autowired
    private BankingDomainService bankingDomainService;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private TransactionAIService transactionAIService;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Session-based conversation context (in production, use Redis or database)
    private final Map<String, ConversationContext> conversationContexts = new HashMap<>();

    // main method that processes the user query
    public String processUserQuery(String userQuery) {
        return processUserQueryWithSession(userQuery, "default-session");
    }
    
    /**
     * Enhanced method with session context support and optimized performance
     */
    public String processUserQueryWithSession(String userQuery, String sessionId) {
        Instant startTime = Instant.now();
        
        try {
            logger.info("🔍 Processing user query: {} (Session: {})", userQuery, sessionId);
            
            // Input validation and sanitization
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return "💬 **No Message Received** - Please share your question about PPC Bank services, and I'll be happy to help!";
            }
            
            // Early greeting detection for faster response
            final String normalizedQuery = normalizeQuery(userQuery);
            if (isGreeting(normalizedQuery)) {
                logger.debug("⚡ Fast greeting response ({}ms)", Duration.between(startTime, Instant.now()).toMillis());
                return "👋 **Hello! Welcome to PPC Bank!**\n\n" +
                       "I'm here to help you with:\n" +
                       "• 🏦 Banking services and products\n" +
                       "• 💳 Account information and requirements\n" +
                       "• 🔍 Transaction status checking\n" +
                       "• 📞 Contact information and branch locations\n" +
                       "• ❓ Any other banking questions\n\n" +
                       "What would you like to know?";
            }

            // Parallel operations for better performance
            CompletableFuture<Boolean> transactionCheckFuture = CompletableFuture.supplyAsync(() -> 
                transactionAIService.isTransactionInquiry(userQuery));
            
            CompletableFuture<Void> cleanupFuture = CompletableFuture.runAsync(() -> {
                searchService.clearCache();
                cleanupExpiredContexts();
            });

            // Wait for transaction check and cleanup
            CompletableFuture.allOf(transactionCheckFuture, cleanupFuture).join();
            
            // Check for transaction inquiries first (high priority)
            if (transactionCheckFuture.get()) {
                logger.info("🔍 Transaction inquiry detected for query: {}", userQuery);
                Duration processingTime = Duration.between(startTime, Instant.now());
                String response = handleTransactionInquiry(userQuery, sessionId);
                logger.info("⚡ Transaction inquiry completed in {}ms", processingTime.toMillis());
                return response;
            }

            // Enhanced domain detection and keyword extraction
            CompletableFuture<List<String>> keywordsFuture = CompletableFuture.supplyAsync(() -> 
                bankingDomainService.extractSmartKeywords(normalizedQuery));
            CompletableFuture<String> domainFuture = CompletableFuture.supplyAsync(() -> 
                bankingDomainService.detectBankingDomain(normalizedQuery));

            List<String> keywords = keywordsFuture.get();
            String primaryDomain = domainFuture.get();
            
            logger.debug("📊 Analysis: keywords={}, domain={}", keywords.size(), primaryDomain);

            // Optimized search with context awareness
            List<PPCBank> relevantPages = performOptimizedSearch(keywords, normalizedQuery, primaryDomain, sessionId);
            
            logger.debug("📄 Found {} relevant pages", relevantPages.size());

            // Store conversation context for future optimization
            if (!relevantPages.isEmpty() && primaryDomain != null) {
                conversationContexts.put(sessionId, 
                    new ConversationContext(primaryDomain, keywords, relevantPages));
            }

            // Enhanced context building and AI response generation
            String contextStr = buildEnhancedContext(relevantPages, userQuery);
            String aiResponse = generateSmartResponse(userQuery, contextStr, relevantPages.isEmpty());

            Duration totalTime = Duration.between(startTime, Instant.now());
            logger.info("✅ Query processed successfully in {}ms (Session: {})", totalTime.toMillis(), sessionId);
            
            return aiResponse;

        } catch (Exception e) {
            Duration errorTime = Duration.between(startTime, Instant.now());
            logger.error("💥 Error processing query after {}ms: {}", errorTime.toMillis(), e.getMessage(), e);
            
            return generateErrorResponse(e, userQuery);
        }
    }
    
    /**
     * Cleans up expired conversation contexts
     */
    private void cleanupExpiredContexts() {
        long maxAge = 600000; // 10 minutes
        conversationContexts.entrySet().removeIf(entry -> entry.getValue().isExpired(maxAge));
    }

    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildEnhancedContext(List<PPCBank> pages, String userQuery) {
        if (pages.isEmpty()) {
            return "While I don't have specific information about your exact query, I can help you with general banking information from PPC Bank.";
        }

        String primaryDomain = bankingDomainService.detectBankingDomain(userQuery);
        StringBuilder context = new StringBuilder();
        
        // Add domain-specific context header
        if (primaryDomain != null) {
            String domainName = bankingDomainService.formatDomainName(primaryDomain);
            context.append(String.format("📋 **%s Information from PPC Bank:**\n\n", domainName));
        } else {
            context.append("📋 **Information from PPC Bank's website:**\n\n");
        }

        // Group and prioritize content by relevance
        List<PPCBank> prioritizedPages = pages.stream().limit(5).collect(Collectors.toList());
        
        for (int i = 0; i < prioritizedPages.size(); i++) {
            PPCBank page = prioritizedPages.get(i);
            context.append(String.format("**Source %d: %s**\n", i + 1,
                    page.getTitle() != null ? page.getTitle() : "PPC Bank Information"));

            // Extract domain-focused content snippets
            String content = searchService.extractDomainRelevantSnippet(page.getContent(), userQuery, primaryDomain);
            context.append(String.format("📄 Content: %s\n", content));
            
            // Add page URL for reference
            if (page.getUrl() != null) {
                context.append(String.format("🔗 Source: %s\n", page.getUrl()));
            }
            
            // Add last updated info if available
            if (page.getUpdatedAt() != null) {
                context.append(String.format("📅 Updated: %s\n", page.getUpdatedAt().toLocalDate()));
            }
            
            context.append("\n");
        }
        
        // Add domain-specific guidance
        if (primaryDomain != null) {
            context.append(bankingDomainService.getDomainSpecificGuidance(primaryDomain));
        }

        return context.toString();
    }

    // Enhanced version with dynamic prompt building
    private String generateSmartResponse(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        PromptBuilder builder = new PromptBuilder(userQuery);

        // Analyze query and add appropriate sections
        QueryAnalysis analysis = QueryAnalysis.analyze(userQuery);

        builder.addDirectAnswerSection()
                .addConditionalTable(analysis.needsDocumentTable(), "Requirements")
                .addConditionalTable(analysis.needsAmountTable(), "Amounts & Fees")
                .addImportantNotes()
                .addNextSteps()
                .addFollowUpQuestions()
                .addContextualInstructions(context);

        return callAiService(builder.build());
    }
    
    private boolean isGreeting(String normalizedQuery) {
        // A set of common greetings.
        final Set<String> greetings = Set.of(
                "hi", "hello", "hey", "yo",
                "good morning", "good afternoon", "good evening",
                "greetings", "howdy"
        );

        // Check for an exact match from the set.
        if (greetings.contains(normalizedQuery)) {
            return true;
        }

        // Also check for short phrases that start with a greeting (e.g., "hello there").
        // We limit this to 3 words to avoid catching complex questions like "hi, can you tell me about loans".
        for (String greeting : greetings) {
            if (normalizedQuery.startsWith(greeting) && normalizedQuery.split("\\s+").length <= 3) {
                return true;
            }
        }

        return false;
    }

    /**
     * Central dispatcher that routes the request to the configured AI provider.
     */
    private String callAiService(String prompt) throws IOException {
        logger.info("Routing AI request to provider: {}", activeProvider);
        switch (activeProvider) {
            case OPENAI:
                return callOpenAIAPI(prompt);
            case GEMINI:
                return callGeminiAPI(prompt);
            case ANTHROPIC:
                return callClaudeAPI(prompt);
            case DEEPSEEK:
                return callDeepSeekAPI(prompt);
            default:
                logger.error("Unsupported AI provider configured: {}", activeProvider);
                throw new IllegalStateException("Unsupported AI provider: " + activeProvider);
        }
    }
    
    //callGeminiAPI method
    private String callGeminiAPI(String prompt) throws IOException {
        // Escape the prompt properly for JSON
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

        String requestBody = String.format("""
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"
                        }]
                    }],
                    "generationConfig": {
                        "temperature": 0.7,
                        "maxOutputTokens": 1500,
                        "topP": 0.8,
                        "topK": 40
                    }
                }
                """, escapedPrompt);

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBody))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AI API error: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            JsonNode candidates = jsonResponse.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }

            return "I apologize, but I'm having trouble generating a response right now. Please try again or contact PPC Bank directly for assistance.";
        }
    }

    //callOpenAIAPI method
    private String callOpenAIAPI(String prompt) throws IOException {
        String apiKey = openAIApiKey;
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> messageUser = new HashMap<>();
        messageUser.put("role", "user");
        messageUser.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(messageUser);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIModel);
        requestBodyMap.put("messages", messages);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                logger.error("OpenAI API error: {} - {}", response.code(), responseBody);
                throw new IOException("OpenAI API error: " + response.code() + " - " + responseBody);
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }

            logger.warn("Could not extract content from OpenAI response: {}", responseBody);
            return "I apologize, but I'm having trouble generating a response right now. Please try again or contact PPC Bank directly for assistance.";
        }
    }

    private String callClaudeAPI(String prompt) throws IOException {
        String url = "https://api.anthropic.com/v1/messages";
        String anthropicVersion = "2023-06-01";

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        List<Map<String, Object>> messages = List.of(userMessage);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", anthropicModel);
        requestBodyMap.put("messages", messages);
        requestBodyMap.put("max_tokens", 1024);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("x-api-key", anthropicApiKey)
                .addHeader("anthropic-version", anthropicVersion)
                .addHeader("Content-Type", "application/json")
                .build();

        return executeRequest(request, "Claude");
    }

    private String callDeepSeekAPI(String prompt) throws IOException {
        String url = "https://api.deepseek.com/chat/completions";

        Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are a helpful AI assistant for PPC Bank.");
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", deepSeekModel);
        requestBodyMap.put("messages", List.of(systemMessage, userMessage));
        requestBodyMap.put("stream", false);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("Authorization", "Bearer " + deepSeekApiKey)
                .build();

        return executeRequest(request, "DeepSeek");
    }
    
    private String executeRequest(Request request, String providerName) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                logger.error("{} API error: {} - {}", providerName, response.code(), responseBody);
                throw new IOException(providerName + " API error: " + response.code() + " - " + responseBody);
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // Handle Claude's unique response structure
            if ("Claude".equals(providerName)) {
                JsonNode contentArray = jsonResponse.path("content");
                if (contentArray.isArray() && !contentArray.isEmpty()) {
                    return contentArray.get(0).path("text").asText();
                }
            } else { // Handle OpenAI, DeepSeek, and other similar structures
                JsonNode choices = jsonResponse.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
            }

            logger.warn("Could not extract content from {} response: {}", providerName, responseBody);
            return "I apologize, but I'm having trouble generating a response right now.";
        }
    }
    
    public String getDatabaseStats() {
        try {
            Long totalPages = pageContentRepository.getTotalPageCount();
            List<PPCBank> recentPages = pageContentRepository.findRecentPages(org.springframework.data.domain.PageRequest.of(0, 5)).getContent();

            StringBuilder stats = new StringBuilder();
            stats.append(String.format("📊 **PPC Bank Information Database**\n\n"));
            stats.append(String.format("📄 **Total pages indexed:** %d\n", totalPages));
            stats.append(String.format("🕒 **Recently updated pages:** %d\n\n", recentPages.size()));

            if (!recentPages.isEmpty()) {
                stats.append("**Recent content:**\n");
                for (PPCBank page : recentPages) {
                    stats.append(String.format("• %s\n",
                            page.getTitle() != null ? page.getTitle() : page.getUrl()));
                }
            }

            stats.append(
                    "\n💡 **Tip:** Ask me about PPC Bank services, accounts, loans, payments, or any banking topic!");

            return stats.toString();
        } catch (Exception e) {
            logger.error("Error getting database stats: {}", e.getMessage(), e);
            return "❌ Unable to retrieve database statistics at the moment.";
        }
    }

    /**
     * Handle transaction inquiry with automatic data extraction
     */
    private String handleTransactionInquiry(String userQuery, String sessionId) {
        logger.info("Handling transaction inquiry: {}", userQuery);
        
        try {
            // Try to extract transaction data from the user query
            TransactionData extractedData = extractTransactionData(userQuery);
            
            if (extractedData.isComplete()) {
                // All data is available, proceed with transaction check
                logger.info("Complete transaction data found, checking status");
                return transactionAIService.checkTransactionStatus(
                    extractedData.hash, 
                    extractedData.amount, 
                    extractedData.currency
                );
            } else {
                // Missing data, request more information
                logger.info("Incomplete transaction data, requesting details");
                StringBuilder response = new StringBuilder();
                response.append("I understand you're experiencing a transaction issue. Let me help you check the transaction status.\n\n");
                
                if (extractedData.hasPartialData()) {
                    response.append("I found some information from your message:\n");
                    if (extractedData.hash != null) {
                        response.append("- Hash: ").append(extractedData.hash).append("\n");
                    }
                    if (extractedData.amount != null) {
                        response.append("- Amount: ").append(extractedData.amount).append("\n");
                    }
                    if (extractedData.currency != null) {
                        response.append("- Currency: ").append(extractedData.currency).append("\n");
                    }
                    response.append("\n");
                }
                
                response.append(transactionAIService.getTransactionDetailsPrompt());
                return response.toString();
            }
            
        } catch (Exception e) {
            logger.error("Error handling transaction inquiry: {}", e.getMessage(), e);
            return "❌ I encountered an error while processing your transaction inquiry. Please try again or contact PPC Bank support.";
        }
    }
    
    /**
     * Extract transaction data from user query using regex patterns
     */
    private TransactionData extractTransactionData(String userQuery) {
        TransactionData data = new TransactionData();
        
        // Enhanced pattern for hash - look for various ways users might provide it
        // Examples: "hash: c250339a", "transaction c250339a", "my hash is abc123", "c250339a"
        Pattern hashPattern = Pattern.compile(
            "(?:hash|transaction|id)[:\\s]+([a-zA-Z0-9]{6,})|" +        // "hash: abc123"
            "\\bhash\\s+([a-zA-Z0-9]{6,})|" +                          // "hash abc123"
            "\\b([a-zA-Z0-9]{8,})\\b(?=\\s|$|,|\\.|!|\\?)",           // standalone alphanumeric 8+ chars
            Pattern.CASE_INSENSITIVE
        );
        Matcher hashMatcher = hashPattern.matcher(userQuery);
        if (hashMatcher.find()) {
            // Get the first non-null group
            for (int i = 1; i <= hashMatcher.groupCount(); i++) {
                if (hashMatcher.group(i) != null) {
                    data.hash = hashMatcher.group(i).trim();
                    break;
                }
            }
        }
        
        // Enhanced pattern for amount - handle various formats
        // Examples: "amount: 50", "50 USD", "$50", "sum 100", "paid 25.50"
        Pattern amountPattern = Pattern.compile(
            "(?:amount|sum|paid|send|sent|transfer)[:\\s]*([0-9]+(?:\\.[0-9]+)?)|" +  // "amount: 50"
            "\\$([0-9]+(?:\\.[0-9]+)?)|" +                                           // "$50"
            "([0-9]+(?:\\.[0-9]+)?)\\s*(?:USD|KHR|dollars?|riels?)",               // "50 USD"
            Pattern.CASE_INSENSITIVE
        );
        Matcher amountMatcher = amountPattern.matcher(userQuery);
        if (amountMatcher.find()) {
            // Get the first non-null group
            for (int i = 1; i <= amountMatcher.groupCount(); i++) {
                if (amountMatcher.group(i) != null) {
                    data.amount = amountMatcher.group(i).trim();
                    break;
                }
            }
        }
        
        // Enhanced pattern for currency - handle various formats
        // Examples: "USD", "KHR", "dollars", "riels", "in USD"
        Pattern currencyPattern = Pattern.compile(
            "\\b(USD|KHR)\\b|" +                    // Direct currency codes
            "\\b(dollars?)\\b|" +                   // "dollar" or "dollars"
            "\\b(riels?)\\b",                      // "riel" or "riels"
            Pattern.CASE_INSENSITIVE
        );
        Matcher currencyMatcher = currencyPattern.matcher(userQuery);
        if (currencyMatcher.find()) {
            String currency = currencyMatcher.group().toLowerCase();
            if (currency.equals("usd") || currency.contains("dollar")) {
                data.currency = "USD";
            } else if (currency.equals("khr") || currency.contains("riel")) {
                data.currency = "KHR";
            } else {
                data.currency = currencyMatcher.group().toUpperCase();
            }
        }
        
        // Additional pattern to extract structured data like "Hash: abc123, Amount: 50, Currency: USD"
        if (data.hash == null || data.amount == null || data.currency == null) {
            extractStructuredData(userQuery, data);
        }
        
        return data;
    }
    
    /**
     * Extract structured transaction data from formatted input
     */
    private void extractStructuredData(String userQuery, TransactionData data) {
        // Look for structured format: "Hash: value, Amount: value, Currency: value"
        String[] parts = userQuery.split("[,\\n]");
        
        for (String part : parts) {
            part = part.trim();
            
            if (data.hash == null && part.toLowerCase().contains("hash")) {
                Pattern hashPart = Pattern.compile("hash[:\\s]*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = hashPart.matcher(part);
                if (matcher.find()) {
                    data.hash = matcher.group(1).trim();
                }
            }
            
            if (data.amount == null && part.toLowerCase().contains("amount")) {
                Pattern amountPart = Pattern.compile("amount[:\\s]*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = amountPart.matcher(part);
                if (matcher.find()) {
                    data.amount = matcher.group(1).trim();
                }
            }
            
            if (data.currency == null && part.toLowerCase().contains("currency")) {
                Pattern currencyPart = Pattern.compile("currency[:\\s]*(USD|KHR|dollars?|riels?)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = currencyPart.matcher(part);
                if (matcher.find()) {
                    String currency = matcher.group(1).toLowerCase();
                    if (currency.equals("usd") || currency.contains("dollar")) {
                        data.currency = "USD";
                    } else if (currency.equals("khr") || currency.contains("riel")) {
                        data.currency = "KHR";
                    } else {
                        data.currency = matcher.group(1).toUpperCase();
                    }
                }
            }
        }
    }
    
    /**
     * Helper class to hold transaction data
     */
    private static class TransactionData {
        String hash;
        String amount;
        String currency;
        
        boolean isComplete() {
            return hash != null && amount != null && currency != null;
        }
        
        boolean hasPartialData() {
            return hash != null || amount != null || currency != null;
        }
    }


// --- Helper Methods ---

    private Request buildPostRequest(String url, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        return new Request.Builder().url(url).post(body).build();
    }

    /**
     * Creates the JSON definition for the 'check_transaction_status' tool,
     * which tells the AI how to use our Java function.
     */
    private Map<String, Object> buildTransactionToolDefinition() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "check_transaction_status");
        tool.put("description", "Checks the status of a bank transaction using its hash, amount, and currency. Use this for any user inquiries about payment failures, pending transactions, or status checks.");

        Map<String, Object> properties = new HashMap<>();
        properties.put("hash", Map.of("type", "STRING", "description", "The unique alphanumeric transaction identifier, e.g., c250339a"));
        properties.put("amount", Map.of("type", "NUMBER", "description", "The numerical amount of the transaction, e.g., 50.00"));
        properties.put("currency", Map.of("type", "STRING", "description", "The currency of the transaction, must be either 'USD' or 'KHR'"));

        tool.put("parameters", Map.of("type", "OBJECT", "properties", properties, "required", List.of("hash", "amount", "currency")));
        return tool;
    }

    private List<PPCBank> performOptimizedSearch(List<String> keywords, String normalizedQuery, 
                                               String primaryDomain, String sessionId) {
        // Check for conversation context first
        ConversationContext context = conversationContexts.get(sessionId);
        List<PPCBank> relevantPages = new ArrayList<>();
        
        if (context != null && !context.isExpired(300000) && // 5 minutes
            primaryDomain != null && primaryDomain.equals(context.getLastDomain())) {
            // Use cached results for better performance
            logger.debug("🎯 Using conversation context for domain: {}", primaryDomain);
            context.updateAccess();
            relevantPages.addAll(context.getLastResults());
            
            // Perform incremental search for new information
            List<PPCBank> additionalResults = searchService.performContextualSearch(keywords, normalizedQuery, context);
            relevantPages.addAll(additionalResults);
            
            // Remove duplicates efficiently
            relevantPages = relevantPages.stream()
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
        } else {
            // Fresh search with optimized strategy
            relevantPages = searchService.performIntelligentSearch(keywords, normalizedQuery);
            
            // Fallback to broader search if needed
            if (relevantPages.isEmpty()) {
                relevantPages = searchService.performBroaderSearch(normalizedQuery);
                logger.debug("🔍 Broader search found {} pages", relevantPages.size());
            }
        }
        
        return relevantPages;
    }

    private String generateErrorResponse(Exception e, String userQuery) {
        String userMessage = e.getMessage();
        
        // Generate context-aware error messages
        if (userQuery != null && !userQuery.isEmpty()) {
            if (userQuery.contains("loan") || userQuery.contains("credit")) {
                return "🏦 **Service Temporarily Unavailable**\n\n" +
                       "I'm experiencing technical difficulties while accessing loan and credit information. " +
                       "For immediate assistance with loans, please:\n\n" +
                       "📞 Call PPC Bank: +855 23 726 999\n" +
                       "🌐 Visit: www.ppcbank.com.kh\n" +
                       "🏢 Visit any PPC Bank branch\n\n" +
                       "I apologize for the inconvenience. Please try again in a few minutes.";
            } else if (userQuery.contains("account") || userQuery.contains("savings")) {
                return "🏦 **Service Temporarily Unavailable**\n\n" +
                       "I'm having trouble accessing account information right now. " +
                       "For immediate help with your account, please:\n\n" +
                       "📞 Call PPC Bank: +855 23 726 999\n" +
                       "🌐 Visit: www.ppcbank.com.kh\n" +
                       "🏢 Visit any PPC Bank branch\n\n" +
                       "Please try again in a few minutes.";
            }
        }
        
        return "❌ **I'm Sorry, Something Went Wrong**\n\n" +
               "I encountered a technical issue while processing your request. " +
               "This is temporary and should resolve shortly.\n\n" +
               "**For immediate assistance:**\n" +
               "📞 Call PPC Bank: +855 23 726 999\n" +
               "🌐 Visit: www.ppcbank.com.kh\n" +
               "🏢 Visit any PPC Bank branch\n\n" +
               "Please try rephrasing your question or contact PPC Bank support directly. " +
               "I apologize for the inconvenience.";
    }
}
