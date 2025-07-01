package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.repository.PPCBankContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
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

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Banking-related keywords and synonyms for better matching
    private final Map<String, List<String>> bankingKeywords = Map.of(
            "payments", Arrays.asList("payment", "pay", "bill", "bills", "transfer", "remittance", "send money"),
            "loans", Arrays.asList("loan", "credit", "lending", "borrow", "mortgage", "financing"),
            "accounts", Arrays.asList("account", "savings", "checking", "deposit", "current account"),
            "cards", Arrays.asList("card", "credit card", "debit card", "atm card", "visa", "mastercard"),
            "services", Arrays.asList("service", "banking service", "financial service", "product"),
            "rates", Arrays.asList("rate", "interest", "fees", "charges", "pricing"),
            "mobile", Arrays.asList("mobile banking", "app", "online banking", "digital", "internet banking"),
            "branches", Arrays.asList("branch", "location", "atm", "office", "address"),
            "business", Arrays.asList("corporate", "commercial", "enterprise", "company", "sme"),
            "forex", Arrays.asList("foreign exchange", "currency", "exchange rate", "usd", "dollar"));

    // main method that processes the user query
    public String processUserQuery(String userQuery) {
        try {
            logger.info("Processing user query: {}", userQuery);

            // Clean and normalize the query
            String normalizedQuery = normalizeQuery(userQuery);

            // Check for simple greetings and provide a direct, simple response.
            if (isGreeting(normalizedQuery)) {
                logger.info("Greeting detected. Bypassing full AI generation for a simple response.");
                return "Hello! How can I assist you today?";
            }

            // Extract smart keywords with intent recognition
            List<String> keywords = extractSmartKeywords(normalizedQuery);
            logger.debug("Extracted smart keywords: {}", keywords);

            // Perform intelligent search
            List<PPCBank> relevantPages = performIntelligentSearch(keywords, normalizedQuery);
            logger.debug("Found {} relevant pages", relevantPages.size());

            // If no specific results, try broader search
            if (relevantPages.isEmpty()) {
                relevantPages = performBroaderSearch(normalizedQuery);
                logger.debug("Broader search found {} pages", relevantPages.size());
            }

            // Build enhanced context
            String context = buildEnhancedContext(relevantPages, userQuery);

            // Generate intelligent AI response
            String aiResponse = generateSmartResponse(userQuery, context, relevantPages.isEmpty());

            logger.info("Successfully generated AI response for query: {}", userQuery);
            return aiResponse;

        } catch (Exception e) {
            logger.error("Error processing user query: {}", e.getMessage(), e);
            return "❌ I apologize, but I encountered an error while processing your request. Please try rephrasing your question or contact support.";
        }
    }

    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> extractSmartKeywords(String query) {
        Set<String> keywords = new HashSet<>();

        // Split query into words
        String[] words = query.split("\\s+");

        // Add original words (filtering out very common words)
        Set<String> stopWords = Set.of("the", "is", "are", "was", "were", "a", "an", "and", "or", "but",
                "in", "on", "at", "to", "for", "of", "with", "by", "from", "about", "into", "through",
                "during", "before", "after", "above", "below", "up", "down", "out", "off", "over",
                "under", "again", "further", "then", "once", "what", "how", "when", "where", "why",
                "tell", "me", "you", "i", "can", "could", "would", "should", "will", "do", "does", "did");

        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        // Add banking-related synonyms
        for (Map.Entry<String, List<String>> entry : bankingKeywords.entrySet()) {
            for (String synonym : entry.getValue()) {
                if (query.contains(synonym)) {
                    keywords.add(entry.getKey());
                    keywords.addAll(entry.getValue());
                }
            }
        }

        // Add phrase-based keywords
        if (query.contains("bill payment") || query.contains("pay bill")) {
            keywords.addAll(Arrays.asList("payment", "bill", "pay", "transfer", "online"));
        }
        if (query.contains("exchange rate") || query.contains("currency")) {
            keywords.addAll(Arrays.asList("forex", "exchange", "rate", "currency", "usd"));
        }
        if (query.contains("mobile banking") || query.contains("app")) {
            keywords.addAll(Arrays.asList("mobile", "app", "online", "digital", "banking"));
        }

        return new ArrayList<>(keywords);
    }

    private List<PPCBank> performIntelligentSearch(List<String> keywords, String originalQuery) {
        Set<PPCBank> allResults = new LinkedHashSet<>();

        // 1. Exact phrase search first
        List<PPCBank> exactResults = pageContentRepository.findByContentContainingIgnoreCase(originalQuery);
        allResults.addAll(exactResults);

        // 2. Multi-keyword search
        if (keywords.size() >= 2) {
            String keyword1 = keywords.size() > 0 ? keywords.get(0) : null;
            String keyword2 = keywords.size() > 1 ? keywords.get(1) : null;
            String keyword3 = keywords.size() > 2 ? keywords.get(2) : null;
            List<PPCBank> multiResults = pageContentRepository.findByMultipleKeywords(keyword1, keyword2, keyword3);
            allResults.addAll(multiResults);
        }

        // 3. Individual keyword search with relevance scoring
        Map<PPCBank, Integer> relevanceScore = new HashMap<>();

        for (String keyword : keywords) {
            List<PPCBank> keywordResults = pageContentRepository.findByTitleOrContentContainingIgnoreCase(keyword);
            for (PPCBank page : keywordResults) {
                relevanceScore.put(page, relevanceScore.getOrDefault(page, 0) + 1);
                allResults.add(page);
            }
        }

        // Sort by relevance score and return top results
        return allResults.stream()
                .sorted((p1, p2) -> Integer.compare(
                        relevanceScore.getOrDefault(p2, 0),
                        relevanceScore.getOrDefault(p1, 0)))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<PPCBank> performBroaderSearch(String query) {
        // If no specific results, try to find any banking-related content
        List<String> generalTerms = Arrays.asList("bank", "service", "account", "payment", "loan", "card");

        for (String term : generalTerms) {
            List<PPCBank> results = pageContentRepository.findByTitleOrContentContainingIgnoreCase(term);
            if (!results.isEmpty()) {
                return results.stream().limit(5).collect(Collectors.toList());
            }
        }

        // Last resort: get recent pages
        return pageContentRepository.findRecentPages(PageRequest.of(0, 5)).getContent();
    }

    private String buildEnhancedContext(List<PPCBank> pages, String userQuery) {
        if (pages.isEmpty()) {
            return "While I don't have specific information about your exact query, I can help you with general banking information from PPC Bank.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Based on information from PPC Bank's website, here's what I found:\n\n");

        for (int i = 0; i < Math.min(pages.size(), 5); i++) {
            PPCBank page = pages.get(i);
            context.append(String.format("**Source %d: %s**\n", i + 1,
                    page.getTitle() != null ? page.getTitle() : "PPC Bank Information"));

            // Extract most relevant content snippets
            String content = extractRelevantSnippet(page.getContent(), userQuery);
            context.append(String.format("Content: %s\n", content));
            context.append(String.format("URL: %s\n\n", page.getUrl()));
        }

        return context.toString();
    }

    private String extractRelevantSnippet(String content, String query) {
        if (content == null)
            return "No content available.";

        // Try to find the most relevant part of the content
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();

        int bestStart = 0;
        int maxMatches = 0;

        // Find the section with most query word matches
        for (int i = 0; i < content.length() - 400; i += 100) {
            int end = Math.min(i + 400, content.length());
            String snippet = lowerContent.substring(i, end);

            int matches = 0;
            for (String word : queryWords) {
                if (word.length() > 2 && snippet.contains(word)) {
                    matches++;
                }
            }

            if (matches > maxMatches) {
                maxMatches = matches;
                bestStart = i;
            }
        }

        int end = Math.min(bestStart + 500, content.length());
        String snippet = content.substring(bestStart, end);

        if (end < content.length()) {
            snippet += "...";
        }

        return snippet;
    }

    private String generateIntelligentResponse(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        String basePrompt = buildBasePrompt(userQuery);
        String enhancedPrompt = noSpecificData ? basePrompt + buildGeneralResponseInstructions()
                : basePrompt + buildContextualResponseInstructions(context);

        return callAiService(enhancedPrompt);
//        return callOpenAIAPI(enhancedPrompt);
    }

    private String buildBasePrompt(String userQuery) {
        return String.format("""
                You are PPC Bank's AI assistant. User asks: "%s"

                RESPONSE STRUCTURE:
                🎯 **Direct Answer** - Answer the specific question immediately
                📋 **Details** - Key information in table format when applicable
                💡 **Important Notes** - Critical considerations or warnings
                🔗 **Next Steps** - Clear action items
                ❓ **Related Questions** - 2-3 relevant follow-up questions

                TONE: Professional, warm, helpful
                FORMAT: Use tables for requirements/amounts/documents
                """, userQuery);
    }

    private String buildGeneralResponseInstructions() {
        return """

                INSTRUCTIONS:
                - Provide general banking guidance since no specific PPC Bank data is available
                - Use banking best practices and common industry standards
                - Include disclaimer: "For specific PPC Bank requirements, please visit a branch or check our website"
                - Structure information clearly with tables where helpful
                - Focus on practical, actionable advice
                """;
    }

    private String buildContextualResponseInstructions(String context) {
        return String.format("""

                PPC BANK INFORMATION:
                %s

                INSTRUCTIONS:
                - Use the provided PPC Bank information as your primary source
                - Answer directly and specifically based on this data
                - If information is incomplete, note what might be missing
                - Create tables for document requirements, fees, or limits
                - Suggest contacting PPC Bank for any unclear details
                """, context);
    }

    // Alternative: Single optimized method approach
    private String generateIntelligentResponseOptimized(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        StringBuilder prompt = new StringBuilder();

        // Core prompt - always included
        prompt.append(String.format("""
                You are PPC Bank's helpful AI assistant. User question: "%s"

                RESPONSE FORMAT:
                🎯 **Answer**: [Direct response to the question]
                """, userQuery));

        // Add specific sections based on query type
        if (containsDocumentQuery(userQuery)) {
            prompt.append("""
                    📋 **Requirements**:
                    | Document | Validity | Notes |
                    |----------|----------|-------|
                    | [Doc 1] | [Period] | [Details] |

                    """);
        }

        if (containsAmountQuery(userQuery)) {
            prompt.append("""
                    💰 **Amounts & Fees**:
                    | Type | Amount | Fee | Limit |
                    |------|--------|-----|-------|
                    | [Type] | [Amount] | [Fee] | [Limit] |

                    """);
        }

        // Always include these sections
        prompt.append("""
                💡 **Important**: [Key considerations]
                🔗 **Next Steps**: [What to do next]
                ❓ **You might ask**: [2-3 follow-up questions]

                """);

        // Context-specific instructions
        if (noSpecificData) {
            prompt.append("""
                    GUIDELINES:
                    - Provide general banking guidance (no specific PPC Bank data available)
                    - Include: "Contact PPC Bank directly for specific requirements"
                    - Use industry-standard practices and common requirements
                    """);
        } else {
            prompt.append(String.format("""
                    PPC BANK DATA:
                    %s

                    GUIDELINES:
                    - Use provided PPC Bank information as primary source
                    - Be specific and accurate based on this data
                    - If data seems incomplete, mention what might be missing
                    """, context));
        }

        return callAiService(prompt.toString());
//        return callOpenAIAPI(prompt.toString());
    }

    // Helper methods for query analysis
    private boolean containsDocumentQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("document") || lowerQuery.contains("requirement") ||
                lowerQuery.contains("passport") || lowerQuery.contains("certificate") ||
                lowerQuery.contains("need") || lowerQuery.contains("bring");
    }

    private boolean containsAmountQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("amount") || lowerQuery.contains("fee") ||
                lowerQuery.contains("cost") || lowerQuery.contains("minimum") ||
                lowerQuery.contains("maximum") || lowerQuery.contains("limit") ||
                lowerQuery.contains("charge");
    }

    // Enhanced version with dynamic prompt building
    private String generateSmartResponse(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        PromptBuilder builder = new PromptBuilder(userQuery);

        // Analyze query and add appropriate sections
        QueryAnalysis analysis = analyzeQuery(userQuery);

        builder.addDirectAnswerSection()
                .addConditionalTable(analysis.needsDocumentTable(), "Requirements")
                .addConditionalTable(analysis.needsAmountTable(), "Amounts & Fees")
                .addImportantNotes()
                .addNextSteps()
                .addFollowUpQuestions();

        if (noSpecificData) {
            // Use the new instructions for external search
            builder.addExternalSearchInstructions();
        } else {
            builder.addContextualInstructions(context);
        }

//        return callOpenAIAPI(builder.build());
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

    private static class PromptBuilder {
        private StringBuilder prompt;
        private String userQuery;

        public PromptBuilder(String userQuery) {
            this.userQuery = userQuery;
            this.prompt = new StringBuilder();
            prompt.append(String.format("You are PPC Bank's AI assistant. User asks: \"%s\"\n\n", userQuery));
        }

        public PromptBuilder addDirectAnswerSection() {
            prompt.append("🎯 **Direct Answer**: [Answer the question immediately]\n\n");
            return this;
        }

        public PromptBuilder addConditionalTable(boolean condition, String type) {
            if (condition) {
                if ("Requirements".equals(type)) {
                    prompt.append("""
                            📋 **Requirements**:
                            | Document | Validity | Purpose |
                            |----------|----------|---------|
                            | [Item] | [Period] | [Reason] |

                            """);
                } else if ("Amounts & Fees".equals(type)) {
                    prompt.append("""
                            💰 **Amounts & Fees**:
                            | Type | Amount | Fee |
                            |------|--------|-----|
                            | [Type] | [Amount] | [Fee] |

                            """);
                }
            }
            return this;
        }

        public PromptBuilder addImportantNotes() {
            prompt.append("💡 **Important**: [Critical considerations]\n");
            return this;
        }

        public PromptBuilder addNextSteps() {
            prompt.append("🔗 **Next Steps**: [Clear actions to take]\n");
            return this;
        }

        public PromptBuilder addFollowUpQuestions() {
            prompt.append("❓ **Related Questions**: [2-3 relevant follow-ups]\n\n");
            return this;
        }

        public PromptBuilder addGeneralBankingInstructions() {
            prompt.append("""
                    INSTRUCTIONS:
                    - Provide helpful general banking guidance
                    - Include: "Contact PPC Bank for specific requirements"
                    - Use professional, warm tone
                    """);
            return this;
        }

        public PromptBuilder addContextualInstructions(String context) {
            prompt.append(String.format("""
                    PPC BANK INFO: %s

                    INSTRUCTIONS:
                    - Use provided information as primary source
                    - Be specific and accurate
                    - Professional, helpful tone
                    """, context));
            return this;
        }
        public PromptBuilder addExternalSearchInstructions() {
            prompt.append("""
                    INSTRUCTIONS:
                    - The internal search for PPC Bank information did not return a specific answer.
                    - Your task is to now act as a general, helpful AI assistant.
                    - Use your broad knowledge and search capabilities to find the best possible answer to the user's question.
                    - **Do NOT invent information about PPC Bank.**
                    - If the user's question was about a general topic (e.g., "what is a loan?"), answer it comprehensively.
                    - If the user's question was specifically about PPC Bank (e.g., "what are PPC Bank's car loan rates?"), you must state that you could not find specific information on the PPC Bank website, but you can provide general information on the topic. Then, provide that general information.
                    """);
            return this;
        }

        public String build() {
            return prompt.toString();
        }
    }

    private static class QueryAnalysis {
        private boolean needsDocumentTable;
        private boolean needsAmountTable;

        public QueryAnalysis(boolean needsDocumentTable, boolean needsAmountTable) {
            this.needsDocumentTable = needsDocumentTable;
            this.needsAmountTable = needsAmountTable;
        }

        public boolean needsDocumentTable() {
            return needsDocumentTable;
        }

        public boolean needsAmountTable() {
            return needsAmountTable;
        }
    }

    private QueryAnalysis analyzeQuery(String query) {
        String lower = query.toLowerCase();
        boolean docTable = lower.matches(".*(document|requirement|passport|certificate|valid|bring|need).*");
        boolean amountTable = lower.matches(".*(amount|fee|cost|minimum|maximum|limit|charge|price).*");
        return new QueryAnalysis(docTable, amountTable);
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
        // Note: The variable name 'googleApiKey' is misleading here.
        // It holds your OpenAI key as per your @Value("${chatgpt.api-key}") annotation.
        // Consider renaming it to 'openAIApiKey' for better clarity.
        String apiKey = openAIApiKey;

        // 1. Set the correct URL for OpenAI
        String url = "https://api.openai.com/v1/chat/completions";

        // 2. Build the request body in the format OpenAI expects
        // We use a Map and ObjectMapper to create the JSON safely, avoiding manual escaping.
        Map<String, Object> messageUser = new HashMap<>();
        messageUser.put("role", "user");
        messageUser.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(messageUser);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIModel); // Uses the model from your application.properties
        requestBodyMap.put("messages", messages);
        // Optional: Add other parameters like temperature, max_tokens, etc.
        // requestBodyMap.put("temperature", 0.7);
        // requestBodyMap.put("max_tokens", 1500);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        // 3. Build the request with the correct headers, including Authorization
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey) // Crucial for OpenAI
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                // Log the detailed error from the API for easier debugging
                logger.error("OpenAI API error: {} - {}", response.code(), responseBody);
                throw new IOException("OpenAI API error: " + response.code() + " - " + responseBody);
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // 4. Parse the OpenAI response structure
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
        // 1. Set the correct URL and required headers for Anthropic
        String url = "https://api.anthropic.com/v1/messages";
        String anthropicVersion = "2023-06-01";

        // 2. Build the request body in the format Claude expects
        // Note: Claude can take a system prompt, but it's a top-level parameter, not in the messages array.
        // For simplicity and alignment with the curl command, we'll stick to the user message.
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        List<Map<String, Object>> messages = List.of(userMessage);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", anthropicModel);
        requestBodyMap.put("messages", messages);
        requestBodyMap.put("max_tokens", 1024); // This is a required parameter for Claude

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        // 3. Build the request with the correct headers for Anthropic
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("x-api-key", anthropicApiKey) // Correct header for authentication
                .addHeader("anthropic-version", anthropicVersion) // Required version header
                .addHeader("Content-Type", "application/json")
                .build();

        // 4. The 'executeRequest' method in your full file context is a great pattern.
        // We can call it here to handle the response.
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
            List<PPCBank> recentPages = pageContentRepository.findRecentPages(PageRequest.of(0, 5)).getContent();

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
}
