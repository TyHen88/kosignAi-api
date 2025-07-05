package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.model.AiToolCallResponse;
import org.kosign.chatbotapi.model.ConversationContext;
import org.kosign.chatbotapi.model.SessionContext;
import org.kosign.chatbotapi.model.TitleMatchResult;
import org.kosign.chatbotapi.repository.PPCBankContentRepository;
import org.kosign.chatbotapi.util.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

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
    private TransactionAIService transactionAIService;

    // Add intelligent query processing services
    @Autowired
    private TitleMatchingService titleMatchingService;

    @Autowired
    private JsonResponseService jsonResponseService;

    @Autowired
    private ContextStorageService contextStorageService;

    private final Map<String, ConversationContext> conversationContexts = new HashMap<>();


    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // 2 minutes for AI processing
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();


    // main method that processes the user query
    public String processUserQuery(String userQuery) {
        return processUserQueryWithSession(userQuery, "default-session");
    }

    /**
     * Enhanced method with session context support and intelligent title matching
     */
    public String processUserQueryWithSession(String userQuery, String sessionId) {
        Instant startTime = Instant.now();



        try {
            logger.info("🔍 Processing user query: '{}' (Session: {})", userQuery, sessionId);

            // Step 1: Validate input
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return "💬 **No Message Received** - Please ask your question about PPC Bank services.";
            }
    
            String normalizedQuery = normalizeQuery(userQuery);

            // Step 2: Quick greet shortcut
            if (isGreeting(normalizedQuery)) {
                logger.debug("⚡ Greeting detected, fast reply.");
                return """
                        👋 **Hello! Welcome to PPC Bank!**

                        I can assist with:
                        • 🏦 Services and products
                        • 💳 Account info and requirements
                        • 🔍 Transaction status
                        • 📍 Branch info and contact
                        • ❓ General banking questions

                        How may I help you today?
                        """;
            }

            // Step 3: Check if it's a transaction inquiry
            if (transactionAIService.isTransactionInquiry(userQuery)) {
                logger.info("🔁 Transaction inquiry detected");
                String response = handleTransactionInquiry(userQuery, sessionId);
                logger.info("✅ Transaction response ready");
                return response;
            }

            // Step 4: Detect query intent before checking context
            String queryIntent = jsonResponseService.detectQueryIntent(userQuery);
            
            // ConversationContext existingContext = conversationContexts.get(sessionId);
            // if (existingContext != null && existingContext.getJsonContext() != null) {
            //     logger.info("♻️ Reusing cached JSON context for session: {}", sessionId);
    
            //     PromptBuilder builder = new PromptBuilder(userQuery);
            //     builder.addContextFollowupInstructions(userQuery, queryIntent);
    
            //     return callAiService(builder.build());
            // }


            // Step 6: No relevant context found, perform database query
            logger.info("🎯 No relevant context found, performing database query...");
            List<TitleMatchResult> matches = titleMatchingService.matchTitles(userQuery, 5);

            if (!matches.isEmpty()) {
                logger.info("✅ {} title match(es) found", matches.size());

                // Step 7: Create clean JSON context from matched data
                String jsonContext = jsonResponseService.createCleanAIResponse(matches, userQuery);

                // Step 8: Store context for future use
                conversationContexts.put(sessionId, new ConversationContext(jsonContext, Instant.now()));

                // Step 9: Build prompt with new cleaned format
                PromptBuilder builder = new PromptBuilder(userQuery);
                builder.addSmartJsonPrompt(jsonContext, userQuery, queryIntent);
                builder.addConditionalTable(true, "Requirements");
                builder.addConditionalTable(true, "Amounts & Fees");
                builder.addConditionalTable(true, "Interest Rates");
                addRelevantTableTypes(builder, queryIntent, userQuery, jsonContext);
                String prompt = builder.build();

                // Step 10: Send to OpenAI and return final result
                String aiResponse = callAiService(prompt);
                logger.info("✅ AI response generated in {}ms", Duration.between(startTime, Instant.now()).toMillis());
                System.err.println("🔍 AI response: " + aiResponse);
                
                Duration totalTime = Duration.between(startTime, Instant.now());
                logger.info("✅ Intelligent query processed successfully in {}ms with {} matches", 
                    totalTime.toMillis(), matches.size());
                
                // CompletableFuture.runAsync(this::cleanupExpiredContexts);
                return aiResponse;
            }

            // Step 11: No title match found — optional fallback here
            logger.warn("⚠️ No title matches found for query: '{}'", userQuery);
            return "❓ I'm sorry, I couldn't find any PPC Bank information related to your question. Please try rephrasing it or contact PPC Bank directly.";

        } catch (Exception e) {
            logger.error("💥 Error while processing user query: {}", e.getMessage(), e);
            
            // Special handling for timeout errors
            if (e instanceof java.net.SocketTimeoutException || 
                (e.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.warn("⏰ AI API timeout detected for query: '{}'", userQuery);
                return generateTimeoutResponse(userQuery);
            }
            
            return generateErrorResponse(e, userQuery);
        }
    }

    public String extractTextFromImage(MultipartFile image) {
        try {
            logger.info("🔍 Extracting text from image using AI vision: {}", image.getOriginalFilename());
            
            // Validate image file
            validateImageFile(image);
            
            // Use AI vision for text extraction
            String extractedText = extractTextFromImageUsingAI(image);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                logger.warn("⚠️ No text extracted from image: {}", image.getOriginalFilename());
                return "No readable text found in the image.";
            }
            
            logger.info("✅ Successfully extracted {} characters from image", extractedText.length());
            return extractedText.trim();
            
        } catch (Exception e) {
            logger.error("❌ Error extracting text from image: {}", e.getMessage(), e);
            return "Error extracting text: " + e.getMessage();
        }
    }

    /**
     * Extract text from image using AI vision capabilities
     */
    private String extractTextFromImageUsingAI(MultipartFile imageFile) throws IOException {
        try {
            // Convert image to base64
            String base64Image = encodeImageToBase64(imageFile);

            // Use the configured AI provider for OCR
            return switch (activeProvider) {
                case OPENAI -> extractTextUsingOpenAIVision(base64Image);
                default -> {
                    logger.warn("Vision OCR not supported for provider: {}, falling back to OpenAI", activeProvider);
                    yield extractTextUsingOpenAIVision(base64Image);
                }
            };

        } catch (Exception e) {
            logger.error("❌ AI vision text extraction failed: {}", e.getMessage());
            throw new IOException("Failed to extract text from image: " + e.getMessage());
        }
    }

    /**
     * Extract text using OpenAI GPT-4 Vision
     */
    private String extractTextUsingOpenAIVision(String base64Image) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";

        // Create message with image
        Map<String, Object> textContent = Map.of("type", "text", "text",
            "Extract all readable text from this image. Return only the text content, no descriptions or formatting.");

        Map<String, Object> imageContent = Map.of(
            "type", "image_url",
            "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
        );

        Map<String, Object> userMessage = Map.of(
            "role", "user",
            "content", List.of(textContent, imageContent)
        );

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini", // Use GPT-4 Vision model
            "messages", List.of(userMessage),
            "max_tokens", 1000
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + openAIApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                logger.error("OpenAI Vision API error: {} - {}", response.code(), responseBody);
                throw new IOException("OpenAI Vision API error: " + response.code());
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText().trim();
                }
            }

            throw new IOException("No text content found in OpenAI Vision response");
        }
    }

   
   
    /**
     * Convert image file to base64 string
     */
    private String encodeImageToBase64(MultipartFile imageFile) throws IOException {
        try {
            byte[] imageBytes = imageFile.getBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to encode image to base64: {}", e.getMessage());
            throw new IOException("Failed to process image file");
        }
    }

    /**
     * Validate uploaded image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("No image file provided");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IOException("Image file size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        List<String> supportedTypes = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/webp"
        );

        if (contentType == null || !supportedTypes.contains(contentType.toLowerCase())) {
            throw new IOException("Unsupported image format. Supported formats: " + supportedTypes);
        }

        logger.debug("Image validation passed - Size: {}KB, Type: {}",
                    file.getSize() / 1024, contentType);
    }

    private void cleanupExpiredContexts() {
        long maxAge = 600_000; // 10 minutes
        conversationContexts.entrySet().removeIf(entry -> entry.getValue().isExpired(maxAge));
    }
    

    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private boolean isGreeting(String normalizedQuery) {
        // A set of common greetings.
        final Set<String> greetings = Set.of(
                "hi", "hello", "hey", "yo",
                "good morning", "good afternoon", "good evening",
                "greetings", "howdy");

        // Check for an exact match from the set.
        if (greetings.contains(normalizedQuery)) {
            return true;
        }
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
    public String callAiService(String prompt) throws IOException {
        logger.info("Routing AI request to provider: {} (prompt length: {} chars)", activeProvider, prompt.length());
        
        // Log if prompt is very long (might cause timeout)
        if (prompt.length() > 8000) {
            logger.warn("⚠️ Large prompt detected ({} chars) - may cause timeout", prompt.length());
        }
        
        Instant apiStart = Instant.now();
        
        try {
            String response = switch (activeProvider) {
                case OPENAI -> callOpenAIAPI(prompt);
                case GEMINI -> callGeminiAPI(prompt);
                case ANTHROPIC -> callClaudeAPI(prompt);
                case DEEPSEEK -> callDeepSeekAPI(prompt);
                default -> {
                    logger.error("Unsupported AI provider configured: {}", activeProvider);
                    throw new IllegalStateException("Unsupported AI provider: " + activeProvider);
                }
            };
            
            long apiDuration = Duration.between(apiStart, Instant.now()).toMillis();
            logger.info("✅ AI API call completed in {}ms", apiDuration);
            
            return response;
            
        } catch (IOException e) {
            long apiDuration = Duration.between(apiStart, Instant.now()).toMillis();
            logger.error("❌ AI API call failed after {}ms: {}", apiDuration, e.getMessage());
            throw e;
        }
    }

    // callGeminiAPI method
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
                        "maxOutputTokens": 2048,
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

    // callOpenAIAPI method
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

        Map<String, Object> systemMessage = Map.of("role", "system", "content",
                "You are a helpful AI assistant for PPC Bank.");
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

    /**
     * Gets statistics about database and context usage
     */
    

    /**
     * Clear context for a specific session (useful for testing or user request)
     */
    public void clearSessionContext(String sessionId) {
        contextStorageService.clearSession(sessionId);
        logger.info("🗑️ Cleared context for session: {}", sessionId);
    }

    /**
     * Check if a session has relevant cached context
     */
    public boolean hasStoredContext(String sessionId, String query) {
        String queryIntent = jsonResponseService.detectQueryIntent(query);
        return contextStorageService.hasRelevantContext(sessionId, normalizeQuery(query), queryIntent);
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
                        extractedData.currency);
            } else {
                // Missing data, request more information
                logger.info("Incomplete transaction data, requesting details");
                StringBuilder response = new StringBuilder();
                response.append(
                        "I understand you're experiencing a transaction issue. Let me help you check the transaction status.\n\n");

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
        // Examples: "hash: c250339a", "transaction c250339a", "my hash is abc123",
        // "c250339a"
        Pattern hashPattern = Pattern.compile(
                "(?:hash|transaction|id)[:\\s]+([a-zA-Z0-9]{6,})|" + // "hash: abc123"
                        "\\bhash\\s+([a-zA-Z0-9]{6,})|" + // "hash abc123"
                        "\\b([a-zA-Z0-9]{8,})\\b(?=\\s|$|,|\\.|!|\\?)", // standalone alphanumeric 8+ chars
                Pattern.CASE_INSENSITIVE);
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
                "(?:amount|sum|paid|send|sent|transfer)[:\\s]*([0-9]+(?:\\.[0-9]+)?)|" + // "amount: 50"
                        "\\$([0-9]+(?:\\.[0-9]+)?)|" + // "$50"
                        "([0-9]+(?:\\.[0-9]+)?)\\s*(?:USD|KHR|dollars?|riels?)", // "50 USD"
                Pattern.CASE_INSENSITIVE);
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
                "\\b(USD|KHR)\\b|" + // Direct currency codes
                        "\\b(dollars?)\\b|" + // "dollar" or "dollars"
                        "\\b(riels?)\\b", // "riel" or "riels"
                Pattern.CASE_INSENSITIVE);
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

        // Additional pattern to extract structured data like "Hash: abc123, Amount: 50,
        // Currency: USD"
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
                Pattern currencyPart = Pattern.compile("currency[:\\s]*(USD|KHR|dollars?|riels?)",
                        Pattern.CASE_INSENSITIVE);
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

    private String generateTimeoutResponse(String userQuery) {
        String lowerQuery = userQuery.toLowerCase();
        
        // Provide context-specific timeout responses
        if (lowerQuery.contains("loan") || lowerQuery.contains("credit")) {
            return "⏰ **Response Timeout - Loan Information**\n\n" +
                   "I'm experiencing high processing load while accessing loan information. " +
                   "For immediate loan assistance:\n\n" +
                   "📞 **Call PPC Bank Loan Department**: +855 23 726 999\n" +
                   "🌐 **Visit**: www.ppcbank.com.kh/loans\n" +
                   "🏢 **Visit any PPC Bank branch**\n\n" +
                   "💡 **Tip**: Try asking a more specific question like 'What are the car loan rates?' " +
                   "or 'How do I apply for a home loan?'";
        } else if (lowerQuery.contains("account") || lowerQuery.contains("savings")) {
            return "⏰ **Response Timeout - Account Information**\n\n" +
                   "I'm experiencing high processing load while accessing account information. " +
                   "For immediate account assistance:\n\n" +
                   "📞 **Call PPC Bank Customer Service**: +855 23 726 999\n" +
                   "🌐 **Visit**: www.ppcbank.com.kh/accounts\n" +
                   "🏢 **Visit any PPC Bank branch**\n\n" +
                   "💡 **Tip**: Try asking a more specific question like 'What are savings account rates?' " +
                   "or 'How do I open an account?'";
        } else {
            return "⏰ **Response Timeout**\n\n" +
                   "I'm experiencing high processing load right now. Please try again in a moment " +
                   "or contact PPC Bank directly for immediate assistance:\n\n" +
                   "📞 **Call**: +855 23 726 999\n" +
                   "🌐 **Visit**: www.ppcbank.com.kh\n" +
                   "🏢 **Visit any PPC Bank branch**\n\n" +
                   "💡 **Tip**: Try asking a shorter, more specific question for faster response.";
        }
    }

    private void addRelevantTableTypes(PromptBuilder builder, String queryIntent, String userQuery, String jsonContext) {
        String lowerQuery = userQuery.toLowerCase();
        String lowerContext = jsonContext.toLowerCase();
        
        // Check for requirements-related content
        if (queryIntent.equals("REQUIREMENTS") || 
            lowerQuery.contains("requirement") || lowerQuery.contains("document") || 
            lowerQuery.contains("need") || lowerQuery.contains("apply") ||
            lowerContext.contains("requirement") || lowerContext.contains("document")) {
            builder.addConditionalTable(true, "Requirements");
        }
        
        // Check for pricing/fees-related content
        if (queryIntent.equals("PRICING") || 
            lowerQuery.contains("fee") || lowerQuery.contains("cost") || 
            lowerQuery.contains("price") || lowerQuery.contains("charge") ||
            lowerContext.contains("fee") || lowerContext.contains("cost") || 
            lowerContext.contains("price") || lowerContext.contains("charge")) {
            builder.addConditionalTable(true, "Amounts & Fees");
        }
        
        // Check for interest rates - determine specific type
        if (lowerQuery.contains("interest") || lowerQuery.contains("rate") || 
            lowerContext.contains("interest") || lowerContext.contains("rate")) {
            
            // Determine specific interest rate type based on context
            if (lowerQuery.contains("deposit") || lowerQuery.contains("saving") || 
                lowerContext.contains("deposit") || lowerContext.contains("saving")) {
                builder.addConditionalTable(true, "Interest Rates Fixed Deposit");
            } else if (lowerQuery.contains("loan") || lowerQuery.contains("credit") || 
                       lowerContext.contains("loan") || lowerContext.contains("credit")) {
                builder.addConditionalTable(true, "Interest Rates Loan");
            } else if (lowerQuery.contains("piggy") || lowerQuery.contains("child") || 
                       lowerContext.contains("piggy") || lowerContext.contains("child")) {
                builder.addConditionalTable(true, "Interest Rates Piggy Bank");
            } else {
                // Default to fixed deposit if no specific type detected
                builder.addConditionalTable(true, "Interest Rates Fixed Deposit");
            }
        }
        
        // If no specific table type detected, add general ones for comprehensive coverage
        if (!queryIntent.equals("REQUIREMENTS") && !queryIntent.equals("PRICING") && 
            !lowerQuery.contains("interest") && !lowerQuery.contains("rate")) {
            // Add basic table types for general queries
            builder.addConditionalTable(true, "Requirements");
            builder.addConditionalTable(true, "Amounts & Fees");
        }
    }
}

