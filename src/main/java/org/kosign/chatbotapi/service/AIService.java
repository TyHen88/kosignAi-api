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
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.kosign.chatbotapi.service.workflow.WorkflowServices;
import org.kosign.chatbotapi.util.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private SearchConfigService searchConfigService;

    private WorkflowQueryService workflowQueryService;

    private WorkflowServices workflowServices;
    @Autowired
    private WorkflowRepository workflowRepository;

    private final Map<String, ConversationContext> conversationContexts = new HashMap<>();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 2 minutes for AI processing
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // main method that processes the user query
    public String processUserQuery(String userQuery, String targetLanguage) {
        return processUserQueryWithSession(userQuery, "default-session", targetLanguage);
    }

    /**
     * Enhanced method with session context support, intelligent title matching, and real-time translation
     */
    public String processUserQueryWithSession(String userQuery, String sessionId, String targetLanguage) {
        Instant startTime = Instant.now();

        try {
            logger.info("🔍 Processing user query: '{}' (Session: {}, Target Language: {})", userQuery, sessionId, targetLanguage);

            // Step 1: Validate input
            if (userQuery == null || userQuery.trim().isEmpty()) {
                String emptyMessage = "💬 **No Message Received** - Please ask your question about PPC Bank services.";
                // Translate empty message if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() && 
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    return translateText(emptyMessage, targetLanguage);
                }
                return emptyMessage;
            }

            // Step 2: Auto-detect input language if no target language specified
            String detectedLanguage = null;
            if (targetLanguage == null || targetLanguage.isEmpty()) {
                detectedLanguage = detectLanguage(userQuery);
                logger.info("🌐 Detected input language: {}", detectedLanguage);
                
                // If input is not English, set target language to input language for response
                if (!detectedLanguage.equals("en")) {
                    targetLanguage = detectedLanguage;
                    logger.info("🔄 Setting target language to detected language: {}", targetLanguage);
                }
            }
            String englishQuery = translateQueryToEnglish(userQuery, detectedLanguage);
            String normalizedQuery = normalizeQuery(englishQuery);

            // Step 3: Quick greet shortcut
            if (isGreeting(normalizedQuery)) {
                logger.debug("⚡ Greeting detected, fast reply.");
                String greetingMessage = """
                        👋 **Hello! Welcome to PPC Bank!**

                        I can assist with:
                        • 🏦 Services and products
                        • 💳 Account info and requirements
                        • 🔍 Transaction status
                        • 📍 Branch info and contact
                        • ❓ General banking questions

                        How may I help you today?
                        """;
                
                // Translate greeting if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() && 
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    return translateText(greetingMessage, targetLanguage);
                }
                return greetingMessage;
            }

            var workflowData = workflowRepository.findAllActiveWorkflowsByTitle(englishQuery);
            
            // Step 4: Check if it's a transaction inquiry
            if (transactionAIService.isTransactionInquiry(englishQuery)) {
                logger.info("🔁 Transaction inquiry detected");
                String response = handleTransactionInquiry(workflowData, userQuery, englishQuery, sessionId);

                // Translate transaction response if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() &&
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    response = translateText(response, targetLanguage);
                }

                logger.info("✅ Transaction response ready");
                return response;
            }

            // Step 5: Check if the query is about account blocked
            if (containsAccountBlockedKeywords(userQuery)) {
                logger.info("🔒 Account blocked detected");
                String response = handleAccountBlocked(workflowData, englishQuery, sessionId);

                // Translate account blocked response if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() &&
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    response = translateText(response, targetLanguage);
                }

                logger.info("✅ Account blocked response ready");
                return response;
            }

            // Step 6: Detect query intent before checking context
            String queryIntent = jsonResponseService.detectQueryIntent(englishQuery);

            // Step 7: No relevant context found, perform database query
            logger.info("🎯 No relevant context found, performing database query...");
            
            // Check and log current search mode
            boolean isDbOnlyMode = searchConfigService.isDbOnlyMode();
            logger.info("🔍 Current search mode: {} ({})", 
                isDbOnlyMode ? "Database Only" : "Hybrid Search",
                isDbOnlyMode ? "Internal data only" : "Internal + External sources");
            
            List<TitleMatchResult> matches = titleMatchingService.matchTitles(userQuery, 5);

            if (!matches.isEmpty()) {
                logger.info("✅ {} title match(es) found", matches.size());

                // Step 8: Create clean JSON context from matched data
                String jsonContext = jsonResponseService.createCleanAIResponse(matches, englishQuery);

                // Step 9: Store context for future use
                conversationContexts.put(sessionId, new ConversationContext(jsonContext, Instant.now()));

                // Step 10: Build prompt with new cleaned format
                PromptBuilder builder = new PromptBuilder(userQuery);
                builder.addSmartJsonPrompt(jsonContext, userQuery, queryIntent);
                builder.addConditionalTable(true, "Requirements");
                builder.addConditionalTable(true, "Amounts & Fees");
                builder.addConditionalTable(true, "Interest Rates");
                addRelevantTableTypes(builder, queryIntent, englishQuery, jsonContext);
                String prompt = builder.build();

                // Step 11: Send to OpenAI with translation support
                String aiResponse = callOpenAIAPI(prompt, targetLanguage);
                logger.info("✅ AI response generated in {}ms", Duration.between(startTime, Instant.now()).toMillis());

                Duration totalTime = Duration.between(startTime, Instant.now());
                logger.info("✅ Intelligent query processed successfully in {}ms with {} matches",
                        totalTime.toMillis(), matches.size());

                return aiResponse;
            }

            // Step 12: No title match found — optional fallback here
            logger.warn("⚠️ No title matches found for query: '{}'", userQuery);
            String noMatchMessage = "❓ I'm sorry, I couldn't find any PPC Bank information related to your question. Please try rephrasing it or contact PPC Bank directly.";
            
            // Translate no match message if needed
            if (targetLanguage != null && !targetLanguage.isEmpty() && 
                !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                return translateText(noMatchMessage, targetLanguage);
            }
            return noMatchMessage;

        } catch (Exception e) {
            logger.error("💥 Error while processing user query: {}", e.getMessage(), e);

            // Special handling for timeout errors
            if (e instanceof java.net.SocketTimeoutException ||
                    (e.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.warn("⏰ AI API timeout detected for query: '{}'", userQuery);
                String timeoutResponse = generateTimeoutResponse(userQuery);
                
                // Translate timeout response if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() && 
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    try {
                        return translateText(timeoutResponse, targetLanguage);
                    } catch (Exception te) {
                        logger.warn("Failed to translate timeout response: {}", te.getMessage());
                        return timeoutResponse; // Return untranslated if translation fails
                    }
                }
                return timeoutResponse;
            }

            String errorResponse = generateErrorResponse(e, userQuery);
            
            // Translate error response if needed
            if (targetLanguage != null && !targetLanguage.isEmpty() && 
                !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                try {
                    return translateText(errorResponse, targetLanguage);
                } catch (Exception te) {
                    logger.warn("Failed to translate error response: {}", te.getMessage());
                    return errorResponse; // Return untranslated if translation fails
                }
            }
            return errorResponse;

        } catch (Throwable e) {
            throw new RuntimeException(e);
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
                "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent));

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini", // Use GPT-4 Vision model
                "messages", List.of(userMessage),
                "max_tokens", 1000);

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
                "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/webp");

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
    // callOpenAIAPI method
    private String callOpenAIAPI(String prompt, String targetLanguage) throws IOException {
        String apiKey = openAIApiKey;
        String url = "https://api.openai.com/v1/chat/completions";

        // Create the user message with the prompt
        Map<String, Object> messageUser = new HashMap<>();
        messageUser.put("role", "user");
        messageUser.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(messageUser);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIModel);
        requestBodyMap.put("messages", messages);
        requestBodyMap.put("temperature", 0.7); // Standard temperature for chatbot responses

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
                    String aiResponse = message.get("content").asText();
                    
                    // Apply translation if targetLanguage is specified and not English
                    if (targetLanguage != null && !targetLanguage.isEmpty() && 
                        !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                        
                        logger.info("🌐 Translating response to: {}", targetLanguage);
                        return translateText(aiResponse, targetLanguage);
                    }
                    
                    return aiResponse;
                }
            }

            logger.warn("Could not extract content from OpenAI response: {}", responseBody);
            return "I apologize, but I'm having trouble generating a response right now. Please try again or contact PPC Bank directly for assistance.";
        }
    }

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
     * @param medataData Workflow metadata
     * @param userQuery The original user query (for data extraction)
     * @param englishQuery The English translated query (for AI processing)
     * @param sessionId Session identifier
     */
    private String handleTransactionInquiry(Object medataData, String userQuery, String englishQuery, String sessionId) {
        logger.info("Handling transaction inquiry - Original: '{}', English: '{}'", userQuery, englishQuery);
        try {
            // Extract transaction data from the ORIGINAL user query to preserve exact values
            TransactionData extractedData = extractTransactionData(userQuery);
            System.err.println("extractedData: " + extractedData.hash + " | " + extractedData.amount + " | " + extractedData.currency);
            
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

                // Get workflow data and build AI prompt using English query for better AI understanding
                if (medataData != null) {
                    PromptBuilder prompt = new PromptBuilder(englishQuery); // Use English query for AI processing

                    prompt.workflowPromptWithMetadata(medataData, englishQuery, "workflow");

                    // Get AI response incorporating workflow data
                    var aiResponse = callOpenAIAPI(prompt.build(), "");
                    logger.info("✅ Generated simple transaction inquiry response");

                    return aiResponse;
                }

                // Fallback to transaction details prompt if no workflow data
                response.append(transactionAIService.getTransactionDetailsPrompt());
                return response.toString();
            }

        } catch (Exception e) {
            logger.error("Error handling transaction inquiry: {}", e.getMessage(), e);
            return "❌ I encountered an error while processing your transaction inquiry. Please try again or contact PPC Bank support.";
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract transaction data from user query using regex patterns
     */
    private TransactionData extractTransactionData(String userQuery) {
        TransactionData data = new TransactionData();
        
        // Add debug logging
        logger.debug("Input text for extraction: {}", userQuery);

        // Enhanced patterns to catch all hash variations including "External Transaction Reference"
        Pattern hashPattern = Pattern.compile(
                // Priority 1: "External Transaction Reference" (with markdown support)
                "(?:\\*\\*)?(?:external\\s+transaction\\s+reference)(?:\\*\\*)?[:\\s]+([a-zA-Z0-9]{6,12})|" +
                
                // Priority 2: "Bakong Hash" label
                "(?:bakong\\s+(?:hash|id)\\s*[#:]?)\\s*([a-zA-Z0-9]{6,12})|" +

                // Priority 3: "Transaction Hash/ID" variations
                "(?:transaction\\s+hash[/]?id|transaction\\s+(?:hash|id))[:\\s#]+([a-zA-Z0-9]{6,12})|" +

                // Priority 4: Common labels with colon/space before the hash
                "(?:external\\s+reference|reference\\s+id|ref(?:erence)?|hash|id)[:\\s#]+([a-zA-Z0-9]{6,12})|" +

                // Priority 5: "my transaction hash"
                "(?:my\\s+)?(?:transaction\\s+)?(?:hash|id)[:\\s#]+([a-zA-Z0-9]{6,12})|" +

                // Priority 6: Standalone 8-char alphanumeric with at least 1 digit + 1 letter (Bakong)
                "\\b(?=[a-zA-Z0-9]{8}\\b)(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]{8}\\b",

                Pattern.CASE_INSENSITIVE);

        Matcher hashMatcher = hashPattern.matcher(userQuery);
        String bestHash = null;
        int bestPriority = 999;

        // Debug: Check what the pattern is finding
        logger.debug("Looking for hash patterns in text...");
        
        while (hashMatcher.find()) {
            logger.debug("Found match: '{}'", hashMatcher.group());
            for (int i = 1; i <= hashMatcher.groupCount(); i++) {
                String group = hashMatcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    logger.debug("Group {}: '{}'", i, group);
                    if (!isCommonWord(group.trim())) {
                        String candidate = group.trim();
                        int priority = getPriorityForHashGroup(i, candidate);

                        // Prefer 8-character hashes (typical for Bakong)
                        if (candidate.length() == 8) {
                            priority -= 10; // Higher priority for 8-char hashes
                        }

                        if (priority < bestPriority) {
                            bestHash = candidate;
                            bestPriority = priority;
                        }

                        logger.debug("Hash candidate: '{}' (length: {}, priority: {})", candidate, candidate.length(),
                                priority);
                    } else {
                        logger.debug("Skipping common word: '{}'", group);
                    }
                }
            }
        }

        if (bestHash != null) {
            data.hash = bestHash;
            logger.debug("Selected best hash: '{}' (priority: {})", bestHash, bestPriority);
        } else {
            logger.debug("No hash found in primary patterns, trying fallback...");
            
            // Try a more specific fallback pattern for markdown format
            Pattern markdownPattern = Pattern.compile("\\*\\*[^*]+\\*\\*[:\\s]+([a-zA-Z0-9]{6,12})\\b", Pattern.CASE_INSENSITIVE);
            Matcher markdownMatcher = markdownPattern.matcher(userQuery);
            if (markdownMatcher.find()) {
                String candidate = markdownMatcher.group(1);
                if (!isCommonWord(candidate)) {
                    data.hash = candidate;
                    logger.debug("Found hash with markdown pattern: '{}'", candidate);
                }
            }
            
            // If still no hash, try simple 8-character pattern
            if (data.hash == null) {
                Pattern fallbackPattern = Pattern.compile("\\b([a-zA-Z0-9]{8})\\b", Pattern.CASE_INSENSITIVE);
                Matcher fallbackMatcher = fallbackPattern.matcher(userQuery);
                while (fallbackMatcher.find()) {
                    String candidate = fallbackMatcher.group(1);
                    if (!isCommonWord(candidate) && candidate.matches(".*[a-zA-Z].*") && candidate.matches(".*[0-9].*")) {
                        data.hash = candidate;
                        logger.debug("Found hash with fallback pattern: '{}'", candidate);
                        break;
                    }
                }
            }
        }

        // Enhanced pattern for amount - handle various formats including negative
        Pattern amountPattern = Pattern.compile(
                "(?:\\d+\\.\\s*)?\\*\\*\\s*(?:amount|original\\s+amount)\\s*\\*\\*[:\\s]*(-?[0-9]+(?:\\.[0-9]+)?)|" +  // Markdown
                        "(?:amount|original\\s+amount|sum|paid|send|sent|transfer)[:\\s]*(-?[0-9]+(?:\\.[0-9]+)?)|" +          // Standard
                        "\\$\\s*(-?[0-9]+(?:\\.[0-9]+)?)|" +                                                                  // $ prefixed
                        "(-?[0-9]+(?:\\.[0-9]+)?)\\s*(?:USD|KHR|dollars?|riels?)",                                            // Number + currency
                Pattern.CASE_INSENSITIVE
        );


        Matcher amountMatcher = amountPattern.matcher(userQuery);
        if (amountMatcher.find()) {
            // Get the first non-null group
            for (int i = 1; i <= amountMatcher.groupCount(); i++) {
                String amount = amountMatcher.group(i);
                if (amount != null && !amount.trim().isEmpty()) {
                    // Store both original and absolute values
                    String originalAmount = amount.trim();
                    data.amount = originalAmount.replaceFirst("^-", "");
                    logger.debug("Extracted amount: {} (original: {})", data.amount, originalAmount);
                    break;
                }
            }
        }

        // Enhanced pattern for currency - handle various formats
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
            logger.debug("Extracted currency: {}", data.currency);
        }

        // Additional pattern to extract structured data
        if (data.hash == null || data.amount == null || data.currency == null) {
            extractStructuredData(userQuery, data);
        }

        logger.info("Final extraction - Hash: {}, Amount: {}, Currency: {}", data.hash, data.amount, data.currency);
        return data;
    }

    /**
     * Check if a string is a common word that shouldn't be considered a hash
     */
    private boolean isCommonWord(String word) {
        String lowerWord = word.toLowerCase();
        String[] commonWords = {
                "error", "amount", "currency", "transaction", "payment", "transfer",
                "from", "account", "bank", "date", "time", "seller", "original",
                "reference", "extracted", "details", "here", "are", "the"
        };

        for (String commonWord : commonWords) {
            if (lowerWord.equals(commonWord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get priority for hash group (lower number = higher priority)
     */
    private int getPriorityForHashGroup(int groupNumber, String candidate) {
        // Group 1: External Transaction Reference (highest priority)
        if (groupNumber == 1)
            return 1;
        // Group 2: Bakong hash 
        if (groupNumber == 2)
            return 2;
        // Group 3: Transaction Hash/ID variations
        if (groupNumber == 3)
            return 3;
        // Group 4: Common labels with colon/space
        if (groupNumber == 4)
            return 4;
        // Group 5: "my transaction hash" patterns
        if (groupNumber == 5)
            return 5;
        // Group 6: 8-character standalone
        if (groupNumber == 6)
            return 6;
        // Other groups
        return 7;
    }

    /**
     * Extract structured transaction data from formatted input
     */
    private void extractStructuredData(String userQuery, TransactionData data) {
        // Look for structured format: "Hash: value, Amount: value, Currency: value"
        String[] parts = userQuery.split("[,\\n]");

        for (String part : parts) {
            part = part.trim();

            if (data.hash == null && (part.toLowerCase().contains("hash") || part.toLowerCase().contains("id") || part.toLowerCase().contains("reference"))) {
                // Enhanced pattern to match various hash formats including "External Transaction Reference" with markdown
                Pattern hashPart = Pattern.compile(
                        "(?:\\*\\*)?(?:external\\s+transaction\\s+reference|bakong\\s+hash\\s*[#:]?|transaction\\s+hash[/]?id|hash[/]?id|transaction\\s+hash|external\\s+reference|reference\\s+id|hash|id)(?:\\*\\*)?[:\\s]*([a-zA-Z0-9]{6,12})",
                        Pattern.CASE_INSENSITIVE);
                Matcher matcher = hashPart.matcher(part);

                String bestHash = null;
                while (matcher.find()) {
                    String candidate = matcher.group(1).trim();
                    if (!isCommonWord(candidate)) {
                        // Prefer 8-character hashes (Bakong standard)
                        if (candidate.length() == 8) {
                            bestHash = candidate;
                            break; // Found ideal 8-char hash, stop looking
                        } else if (bestHash == null) {
                            bestHash = candidate; // Use as fallback
                        }
                    }
                }

                if (bestHash != null) {
                    data.hash = bestHash;
                    logger.debug("Extracted hash from structured data: {}", data.hash);
                }
            }

            if (data.amount == null && part.toLowerCase().contains("amount")) {
                // Handle both Markdown format "**Amount:** -2.00" and standard "amount: 50"
                Pattern amountPart = Pattern.compile(
                        "(?:\\*\\*(?:original\\s+)?amount\\*\\*|(?:original\\s+)?amount)[:\\s]*-?([0-9]+(?:\\.[0-9]+)?)",
                        Pattern.CASE_INSENSITIVE);
                Matcher matcher = amountPart.matcher(part);
                if (matcher.find()) {
                    String amount = matcher.group(1).trim();
                    // Remove negative sign for transaction checking
                    if (amount.startsWith("-")) {
                        amount = amount.substring(1);
                    }
                    data.amount = amount;
                    logger.debug("Extracted amount from structured data: {}", data.amount);
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
                    logger.debug("Extracted currency from structured data: {}", data.currency);
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

    private void addRelevantTableTypes(PromptBuilder builder, String queryIntent, String userQuery,
            String jsonContext) {
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

        // If no specific table type detected, add general ones for comprehensive
        // coverage
        if (!queryIntent.equals("REQUIREMENTS") && !queryIntent.equals("PRICING") &&
                !lowerQuery.contains("interest") && !lowerQuery.contains("rate")) {
            // Add basic table types for general queries
            builder.addConditionalTable(true, "Requirements");
            builder.addConditionalTable(true, "Amounts & Fees");
        }
    }

    /**
     * Process transaction receipt image - extract details and verify transaction
     */
    public String processTransactionReceiptImage(MultipartFile image, String sessionId) {
        try {
            logger.info("🧾 Processing transaction receipt image: {}", image.getOriginalFilename());

            // Validate image file
            validateImageFile(image);

            // Extract text from image with transaction-focused prompt
            String extractedText = extractTransactionDataFromImage(image);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return "❌ **No Text Found**\n\nI couldn't extract any readable text from your receipt image. Please ensure the image is clear and try again.";
            }

            logger.info("📄 Extracted text from receipt: {}", extractedText);

            // Check if user is requesting transaction verification
            if (isTransactionInquiry(extractedText) || containsTransactionKeywords(extractedText)) {
                logger.info("🔍 Transaction details detected in image, attempting verification...");

                // Try to extract transaction details from the image text
                TransactionData transactionData = extractTransactionData(extractedText);

                if (transactionData.isComplete()) {
                    // All data found, proceed with verification
                    logger.info("✅ Complete transaction data extracted from image");
                    return transactionAIService.checkTransactionStatus(
                            transactionData.hash,
                            transactionData.amount,
                            transactionData.currency);
                } else if (transactionData.hasPartialData()) {
                    // Partial data found, request missing details
                    return generatePartialDataResponse(transactionData, extractedText);
                } else {
                    // No transaction data found, provide helpful guidance
                    return generateTransactionGuidanceResponse(extractedText);
                }
            } else {
                // No transaction keywords found, just return extracted text with helpful
                // message
                return generateGeneralImageResponse(extractedText);
            }

        } catch (Exception e) {
            logger.error("❌ Error processing transaction receipt image: {}", e.getMessage(), e);
            return "❌ **Error Processing Image**\n\nI encountered an error while processing your receipt image: "
                    + e.getMessage() + "\n\nPlease try again or contact PPC Bank support.";
        }
    }

    /**
     * Extract transaction data from image using AI with transaction-focused prompts
     */
    private String extractTransactionDataFromImage(MultipartFile imageFile) throws IOException {
        try {
            // Convert image to base64
            String base64Image = encodeImageToBase64(imageFile);

            // Use transaction-focused prompt for better extraction
            return switch (activeProvider) {
                case OPENAI -> extractTransactionTextUsingOpenAIVision(base64Image);
                default -> {
                    logger.warn("Vision OCR not supported for provider: {}, falling back to OpenAI", activeProvider);
                    yield extractTransactionTextUsingOpenAIVision(base64Image);
                }
            };

        } catch (Exception e) {
            logger.error("❌ AI vision transaction extraction failed: {}", e.getMessage());
            throw new IOException("Failed to extract transaction data from image: " + e.getMessage());
        }
    }

    /**
     * Extract transaction text using OpenAI GPT-4 Vision with transaction-focused
     * prompt
     */
    private String extractTransactionTextUsingOpenAIVision(String base64Image) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";

        // Create transaction-focused prompt
        Map<String, Object> textContent = Map.of("type", "text", "text",
                """
                        Extract all text from this transaction receipt or payment screenshot. Focus on finding:

                        1. Transaction Hash/ID / Bakong Hash / External Transaction Reference (alphanumeric code like: c250339a) hash only 8 characters
                        2. Amount (numbers with decimals like: 50, 25.75, 100.00)
                        3. Currency (USD, KHR, dollars, riels)
                        4. Transaction status or any error messages
                        5. Bank names, account information
                        6. Date and time information

                        Return all the text exactly as it appears, maintaining the original formatting and structure.
                        If you see transaction details, make them clearly visible in your response.
                        """);

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent));

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(userMessage),
                "max_tokens", 1500);

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
     * Check if extracted text contains transaction-related keywords
     */
    private boolean containsTransactionKeywords(String text) {
        String lowerText = text.toLowerCase();
        String[] keywords = {
                "transaction", "payment", "transfer", "hash", "amount", "currency",
                "usd", "khr", "failed", "error", "pending", "bakong", "ppc bank",
                "receipt", "confirmation", "reference", "id", "verify", "check",
        };

        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if text represents a transaction inquiry using TransactionAIService
     */
    private boolean isTransactionInquiry(String text) {
        return transactionAIService.isTransactionInquiry(text);
    }

    /**
     * Generate response when partial transaction data is found
     */
    private String generatePartialDataResponse(TransactionData data, String extractedText) {
        StringBuilder response = new StringBuilder();
        response.append("🔍 **Transaction Receipt Detected**\n\n");
        response.append("I found some transaction details in your image:\n\n");

        if (data.hash != null) {
            response.append("• **Hash**: ").append(data.hash).append("\n");
        }
        if (data.amount != null) {
            response.append("• **Amount**: ").append(data.amount).append("\n");
        }
        if (data.currency != null) {
            response.append("• **Currency**: ").append(data.currency).append("\n");
        }

        response.append("\n**📝 Missing Information:**\n");
        if (data.hash == null) {
            response.append("• Transaction Hash/ID\n");
        }
        if (data.amount == null) {
            response.append("• Transaction Amount\n");
        }
        if (data.currency == null) {
            response.append("• Currency (USD or KHR)\n");
        }

        response.append("\n💬 **Want me to check this transaction?**\n");
        response.append("Please provide the missing details or upload a clearer image of your receipt.\n\n");

        response.append("**📄 Extracted Text:**\n");
        response.append("```\n").append(extractedText).append("\n```");

        return response.toString();
    }

    /**
     * Generate response when no transaction data is found but keywords suggest
     * transaction intent
     */
    private String generateTransactionGuidanceResponse(String extractedText) {
        return """
                🧾 **Transaction Receipt Upload**

                I can see this appears to be a transaction-related image, but I couldn't extract the specific details needed for verification.

                **For transaction verification, I need:**
                • **Hash**: Transaction ID (like: c250339a)
                • **Amount**: Transaction amount (like: 50)
                • **Currency**: USD or KHR

                💡 **Tips for better results:**
                • Ensure the image is clear and well-lit
                • Make sure transaction details are fully visible
                • Try uploading a higher resolution image

                💬 **Alternative:** You can also provide the details manually:
                ```
                Hash: [your-transaction-hash]
                Amount: [amount]
                Currency: [USD/KHR]
                ```

                **📄 Extracted Text:**
                ```
                """
                + extractedText + """
                        ```

                        Would you like to try uploading another image or provide the details manually?
                        """;
    }

    /**
     * Generate response for general (non-transaction) images
     */
    private String generateGeneralImageResponse(String extractedText) {
        return """
                📄 **Image Text Extracted**

                Here's the text I found in your image:

                ```
                """ + extractedText + """
                ```

                💬 If this contains transaction details and you'd like me to verify a transaction, please let me know!

                I can help check transaction status if you provide:
                • Transaction Hash
                • Amount
                • Currency (USD or KHR)

                How can I assist you with this information?
                """;
    }

    private boolean containsAccountBlockedKeywords(String text) {
        String lowerText = text.toLowerCase();
        String[] keywords = {
                "blocked", "locked", "account", "block", "unlock", "password",
                "check", "status", "account", "how to unblock", "how to unlock",
                "cannot access", "can't access", "can't login", "cannot login",
                "how do i unblock", "how do i unlock", "help unlock", "help unblock",
                "how to check account", "check account block", "account blocked",
                "unlock account", "unblock account", "account unlock", "account unblock"
        };

        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user input contains structured account verification data
     * Format: Full Name: [name], Date of Birth: [date], National ID: [id], Account Number: [number], Mobile: [mobile]
     */
    private boolean containsAccountVerificationData(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for key patterns that indicate structured verification data
        // Support both colon-only format and dash-separated format
        boolean hasFullName = lowerText.contains("full name:") || lowerText.contains("name:");
        boolean hasDateOfBirth = lowerText.contains("date of birth:") || lowerText.contains("dob:");
        boolean hasNationalId = lowerText.contains("national id:") || lowerText.contains("id:");
        boolean hasAccountNumber = lowerText.contains("account number:") || lowerText.contains("account:");
        boolean hasMobile = lowerText.contains("mobile:") || lowerText.contains("phone:");
        
        // Require at least 3 of the 5 fields to consider it verification data
        int fieldCount = 0;
        if (hasFullName) fieldCount++;
        if (hasDateOfBirth) fieldCount++;
        if (hasNationalId) fieldCount++;
        if (hasAccountNumber) fieldCount++;
        if (hasMobile) fieldCount++;
        
        return fieldCount >= 3;
    }

    /**
     * Validate account verification data and return missing/invalid fields
     * Returns validation result with detailed feedback for each field
     */
    private AccountVerificationResult validateAccountVerificationData(String text) {
        // Extract values using the same logic as PromptBuilder
        String fullName = extractAccountValue(text, "full name", "name");
        String dateOfBirth = extractAccountValue(text, "date of birth", "dob");
        String nationalId = extractAccountValue(text, "national id", "id");
        String accountNumber = extractAccountValue(text, "account number", "account");
        String mobile = extractAccountValue(text, "mobile", "phone");
        
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        Map<String, String> invalidReasons = new HashMap<>();
        
        // Validate Full Name
        if (fullName.isEmpty()) {
            missingFields.add("Full Name");
        } else if (!isValidFullName(fullName)) {
            invalidFields.add("Full Name");
            invalidReasons.put("Full Name", getFullNameValidationError(fullName));
        }
        
        // Validate Date of Birth
        if (dateOfBirth.isEmpty()) {
            missingFields.add("Date of Birth");
        } else if (!isValidDateOfBirth(dateOfBirth)) {
            invalidFields.add("Date of Birth");
            invalidReasons.put("Date of Birth", getDateOfBirthValidationError(dateOfBirth));
        }
        
        // Validate National ID
        if (nationalId.isEmpty()) {
            missingFields.add("National ID");
        } else if (!isValidNationalId(nationalId)) {
            invalidFields.add("National ID");
            invalidReasons.put("National ID", getNationalIdValidationError(nationalId));
        }
        
        // Validate Account Number
        if (accountNumber.isEmpty()) {
            missingFields.add("Account Number");
        } else if (!isValidAccountNumber(accountNumber)) {
            invalidFields.add("Account Number");
            invalidReasons.put("Account Number", getAccountNumberValidationError(accountNumber));
        }
        
        // Validate Mobile
        if (mobile.isEmpty()) {
            missingFields.add("Mobile");
        } else if (!isValidMobile(mobile)) {
            invalidFields.add("Mobile");
            invalidReasons.put("Mobile", getMobileValidationError(mobile));
        }
        
        boolean isValid = missingFields.isEmpty() && invalidFields.isEmpty();
        
        return new AccountVerificationResult(isValid, missingFields, invalidFields, invalidReasons, 
                                           fullName, dateOfBirth, nationalId, accountNumber, mobile);
    }
    
    // Field validation methods
    private boolean isValidFullName(String fullName) {
        // Full name should contain only letters, spaces, and common punctuation
        // Minimum 2 characters, maximum 100 characters
        if (fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            return false;
        }
        // Allow letters (any language), spaces, hyphens, apostrophes, dots
        return fullName.matches("^[\\p{L}\\s\\-'\\.]+$");
    }
    
    private String getFullNameValidationError(String fullName) {
        if (fullName.trim().length() < 2) {
            return "Full name is too short (minimum 2 characters)";
        }
        if (fullName.trim().length() > 100) {
            return "Full name is too long (maximum 100 characters)";
        }
        if (!fullName.matches("^[\\p{L}\\s\\-'\\.]+$")) {
            return "Full name contains invalid characters (only letters, spaces, hyphens, apostrophes, and dots are allowed)";
        }
        return "Invalid full name format";
    }
    
    private boolean isValidDateOfBirth(String dateOfBirth) {
        // Support formats: DD/MM/YYYY, DD-MM-YYYY, DDMMYYYY
        String cleaned = dateOfBirth.replaceAll("[/-]", "");
        
        // Check if it's 8 digits
        if (!cleaned.matches("\\d{8}")) {
            return false;
        }
        
        try {
            int day = Integer.parseInt(cleaned.substring(0, 2));
            int month = Integer.parseInt(cleaned.substring(2, 4));
            int year = Integer.parseInt(cleaned.substring(4, 8));
            
            // Basic validation
            if (day < 1 || day > 31) return false;
            if (month < 1 || month > 12) return false;
            if (year < 1900 || year > 2010) return false; // Reasonable age range for banking
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String getDateOfBirthValidationError(String dateOfBirth) {
        String cleaned = dateOfBirth.replaceAll("[/-]", "");
        
        if (!cleaned.matches("\\d{8}")) {
            return "Date format invalid. Use DD/MM/YYYY, DD-MM-YYYY, or DDMMYYYY (e.g., 10/11/2001 or 10112001)";
        }
        
        try {
            int day = Integer.parseInt(cleaned.substring(0, 2));
            int month = Integer.parseInt(cleaned.substring(2, 4));
            int year = Integer.parseInt(cleaned.substring(4, 8));
            
            if (day < 1 || day > 31) {
                return "Invalid day: " + day + " (must be between 1-31)";
            }
            if (month < 1 || month > 12) {
                return "Invalid month: " + month + " (must be between 1-12)";
            }
            if (year < 1900 || year > 2010) {
                return "Invalid year: " + year + " (must be between 1900-2010)";
            }
        } catch (NumberFormatException e) {
            return "Date contains non-numeric characters";
        }
        
        return "Invalid date of birth format";
    }
    
    private boolean isValidNationalId(String nationalId) {
        // Remove spaces and hyphens
        String cleaned = nationalId.replaceAll("[\\s-]", "");
        // National ID should be 9-12 digits (typical for most countries)
        return cleaned.matches("\\d{9,12}");
    }
    
    private String getNationalIdValidationError(String nationalId) {
        String cleaned = nationalId.replaceAll("[\\s-]", "");
        
        if (!cleaned.matches("\\d+")) {
            return "National ID must contain only numbers (spaces and hyphens are allowed for formatting)";
        }
        if (cleaned.length() < 9) {
            return "National ID is too short (minimum 9 digits)";
        }
        if (cleaned.length() > 12) {
            return "National ID is too long (maximum 12 digits)";
        }
        return "Invalid National ID format";
    }
    
    private boolean isValidAccountNumber(String accountNumber) {
        // Account numbers can have various formats: 1-120000127284-0, 1234567890, etc.
        // Remove spaces, hyphens for validation
        String cleaned = accountNumber.replaceAll("[\\s-]", "");
        // Should be 10-20 digits
        return cleaned.matches("\\d{10,20}");
    }
    
    private String getAccountNumberValidationError(String accountNumber) {
        String cleaned = accountNumber.replaceAll("[\\s-]", "");
        
        if (!cleaned.matches("\\d+")) {
            return "Account number must contain only numbers (spaces and hyphens are allowed for formatting)";
        }
        if (cleaned.length() < 10) {
            return "Account number is too short (minimum 10 digits)";
        }
        if (cleaned.length() > 20) {
            return "Account number is too long (maximum 20 digits)";
        }
        return "Invalid account number format";
    }
    
    private boolean isValidMobile(String mobile) {
        // Remove spaces, hyphens, plus signs
        String cleaned = mobile.replaceAll("[\\s\\-\\+]", "");
        // Should be 8-15 digits (international format)
        return cleaned.matches("\\d{8,15}");
    }
    
    private String getMobileValidationError(String mobile) {
        String cleaned = mobile.replaceAll("[\\s\\-\\+]", "");
        
        if (!cleaned.matches("\\d+")) {
            return "Mobile number must contain only numbers (spaces, hyphens, and + are allowed for formatting)";
        }
        if (cleaned.length() < 8) {
            return "Mobile number is too short (minimum 8 digits)";
        }
        if (cleaned.length() > 15) {
            return "Mobile number is too long (maximum 15 digits)";
        }
        return "Invalid mobile number format";
    }

    /**
     * Helper method to extract account values (same logic as PromptBuilder.extractValue)
     */
    private String extractAccountValue(String text, String... fieldNames) {
        for (String fieldName : fieldNames) {
            // Pattern 1: Standard format "Field Name: Value" or "Field: Value" 
            String pattern1 = fieldName + ":\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            String pattern2 = fieldName + "\\s*:\\s*(.+?)(?:\\n|$|,|\\s{2,})";
            
            // Pattern 3: Dash-separated format "Field Name: Value -" or "Field: Value -"
            String pattern3 = fieldName + ":\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";
            String pattern4 = fieldName + "\\s*:\\s*(.+?)\\s*-\\s*(?:[A-Za-z]|$)";
            
            Pattern p1 = Pattern.compile(pattern1, Pattern.CASE_INSENSITIVE);
            Pattern p2 = Pattern.compile(pattern2, Pattern.CASE_INSENSITIVE);
            Pattern p3 = Pattern.compile(pattern3, Pattern.CASE_INSENSITIVE);
            Pattern p4 = Pattern.compile(pattern4, Pattern.CASE_INSENSITIVE);
            
            // Try dash-separated patterns first (more specific)
            Matcher m3 = p3.matcher(text);
            if (m3.find()) {
                return m3.group(1).trim();
            }
            
            Matcher m4 = p4.matcher(text);
            if (m4.find()) {
                return m4.group(1).trim();
            }
            
            // Then try standard colon patterns
            Matcher m1 = p1.matcher(text);
            if (m1.find()) {
                return m1.group(1).trim();
            }
            
            Matcher m2 = p2.matcher(text);
            if (m2.find()) {
                return m2.group(1).trim();
            }
        }
        return "";
    }
    
    /**
     * Generate validation error prompt for account verification
     */
    private String generateValidationErrorPrompt(AccountVerificationResult validationResult) {
        StringBuilder response = new StringBuilder();
        
        // Handle missing fields
        if (!validationResult.missingFields.isEmpty()) {
            response.append("❌ **Missing Required Information**\n\n");
            response.append("I cannot find the following required information:\n\n");
            
            for (String field : validationResult.missingFields) {
                response.append("• ").append(field).append("\n");
            }
            response.append("\n");
        }
        
        // Handle invalid fields
        if (!validationResult.invalidFields.isEmpty()) {
            response.append("⚠️ **Invalid Information Found**\n\n");
            response.append("The following information appears to be invalid:\n\n");
            
            for (String field : validationResult.invalidFields) {
                String reason = validationResult.invalidReasons.get(field);
                response.append("• **").append(field).append("**: ").append(reason).append("\n");
            }
            response.append("\n");
        }
        
        response.append("💬 **Please provide the correct information in this format:**\n");
        response.append("```\n");
        response.append("Full Name: ").append(validationResult.fullName).append("\n");
        response.append("Date of Birth: ").append(validationResult.dateOfBirth).append("\n");
        response.append("National ID: ").append(validationResult.nationalId).append("\n");
        response.append("Account Number: ").append(validationResult.accountNumber).append("\n");
        response.append("Mobile: ").append(validationResult.mobile).append("\n");
        response.append("```\n\n");
        response.append("📝 **Example:**\n");
        response.append("```\n");
        response.append("Full Name: John Doe\n");
        response.append("Date of Birth: 10/11/2001\n");
        response.append("National ID: 160524688\n");
        response.append("Account Number: 1-120000127284-0\n");
        response.append("Mobile: 010297859\n");
        response.append("```\n\n");
        response.append("Once you provide all the correct information, I'll be able to verify your account.");
        
        return response.toString();
    }
    
    /**
     * Enhanced helper class to hold account verification validation results
     */
    private static class AccountVerificationResult {
        final boolean isValid;
        final List<String> missingFields;
        final List<String> invalidFields;
        final Map<String, String> invalidReasons;
        final String fullName;
        final String dateOfBirth;
        final String nationalId;
        final String accountNumber;
        final String mobile;
        
        // Constructor for enhanced validation results
        AccountVerificationResult(boolean isValid, List<String> missingFields, List<String> invalidFields, 
                                Map<String, String> invalidReasons, String fullName, String dateOfBirth, 
                                String nationalId, String accountNumber, String mobile) {
            this.isValid = isValid;
            this.missingFields = missingFields;
            this.invalidFields = invalidFields;
            this.invalidReasons = invalidReasons;
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.nationalId = nationalId;
            this.accountNumber = accountNumber;
            this.mobile = mobile;
        }
        
        // Legacy constructor for backward compatibility
        AccountVerificationResult(boolean isValid, List<String> missingFields, String fullName, 
                                String dateOfBirth, String nationalId, String accountNumber, String mobile) {
            this(isValid, missingFields, new ArrayList<>(), new HashMap<>(), 
                 fullName, dateOfBirth, nationalId, accountNumber, mobile);
        }
    }

    private String handleAccountBlocked(Object medataData, String userQuery, String sessionId) {
        PromptBuilder promptBuilder = new PromptBuilder(userQuery);
        System.err.println("Account blocked::" + userQuery);
        // Check if user provided structured account verification data
        if (containsAccountVerificationData(userQuery)) {
            logger.info("🔍 Account verification data detected, validating completeness...");
            
            // Validate that all required fields are present
            AccountVerificationResult validationResult = validateAccountVerificationData(userQuery);
            
            if (!validationResult.isValid) {
                logger.warn("❌ Missing required fields for account verification: {}", validationResult.missingFields);
                return generateValidationErrorPrompt(validationResult);
            }
            
            logger.info("✅ All required account verification data found, proceeding with verification");
            promptBuilder.testAccountBlockedPromptVerify(userQuery);
        } else if (containsAccountBlockedKeywords(userQuery) || userQuery.contains("How to check account blocked?")) {
            logger.info("🔒 Account blocked keywords detected, using standard prompt");
            promptBuilder.workflowPromptWithMetadata(medataData, userQuery, "account-blocked");
//            promptBuilder.simpleTestAccountBlockedPrompt(userQuery);
        } else {
            // Fallback to blocked check prompt
            logger.info("⚠️ Using account blocked check prompt as fallback");
            promptBuilder.testAccountHaveBlockedPromptCheck(userQuery);
        }
        
        String prompt = promptBuilder.build();
        try {
            return callOpenAIAPI(prompt, "");
        } catch (IOException e) {
            logger.error("❌ Error handling account blocked: {}", e.getMessage(), e);
            return "❌ **Error Handling Account Blocked**\n\nI encountered an error while handling your account blocked request: "
                    + e.getMessage() + "\n\nPlease try again or contact PPC Bank support.";
        }
    }

    /**
     * Detect the language of input text
     */
    public String detectLanguage(String text) {
        try {
            String prompt = String.format("""
                Detect the language of this text and return only the language code (e.g., 'en', 'km', 'zh', 'ja', 'ko', 'th', 'vi', 'fr', 'de', 'es'):
                
                Text: "%s"
                
                Return only the 2-letter language code, nothing else.
                """, text.replace("\"", "\\\""));
            
            String response = callOpenAIAPIForTranslation(prompt);
            return response.trim().toLowerCase();
            
        } catch (Exception e) {
            logger.warn("Language detection failed, defaulting to English: {}", e.getMessage());
            return "en"; // Default to English if detection fails
        }
    }
    
    /**
     * Translate text to target language
     */
    public String translateText(String text, String targetLanguage) {
        try {
            if (targetLanguage == null || targetLanguage.isEmpty() || targetLanguage.equals("en")) {
                return text; // No translation needed for English
            }
            
            String languageName = getLanguageName(targetLanguage);
            String prompt = String.format("""
                Translate the following text to %s. Maintain the original formatting, structure, and any special characters like emojis or markdown:
                
                Text to translate:
                %s
                
                Translation:
                """, languageName, text);
            
            return callOpenAIAPIForTranslation(prompt);
            
        } catch (Exception e) {
            logger.error("Translation failed for language '{}': {}", targetLanguage, e.getMessage());
            return text; // Return original text if translation fails
        }
    }
    
    /**
     * Get full language name from language code
     */
    private String getLanguageName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "km" -> "Khmer (Cambodian)";
            case "zh" -> "Chinese (Simplified)";
            case "zh-tw" -> "Chinese (Traditional)";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "th" -> "Thai";
            case "vi" -> "Vietnamese";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "ru" -> "Russian";
            case "ar" -> "Arabic";
            case "hi" -> "Hindi";
            case "id" -> "Indonesian";
            case "ms" -> "Malay";
            case "tl" -> "Filipino";
            case "my" -> "Burmese";
            case "lo" -> "Lao";
            default -> "English";
        };
    }
    
    /**
     * Dedicated OpenAI API call for translation (without additional system messages)
     */
    private String callOpenAIAPIForTranslation(String prompt) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> messageUser = new HashMap<>();
        messageUser.put("role", "user");
        messageUser.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(messageUser);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIModel);
        requestBodyMap.put("messages", messages);
        requestBodyMap.put("temperature", 0.3); // Lower temperature for more consistent translations

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBodyJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + openAIApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                logger.error("OpenAI Translation API error: {} - {}", response.code(), responseBody);
                throw new IOException("Translation API error: " + response.code());
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText().trim();
                }
            }

            throw new IOException("No translation content found in API response");
        }
    }

    /**
     * Translate user query to English for better database matching
     */
    public String translateQueryToEnglish(String userQuery, String detectedLanguage) {
        try {
            // If already English, return as-is
            if (detectedLanguage == null || detectedLanguage.equals("en")) {
                return userQuery;
            }
            
            logger.info("🔄 Translating query from '{}' to English for database search", detectedLanguage);
            
            String prompt = String.format("""
                Translate this banking/financial query from %s to English. 
                Keep banking terms accurate and maintain the original intent.
                Focus on preserving keywords that would help find relevant banking information.
                
                Original query in %s:
                %s
                
                English translation:
                """, getLanguageName(detectedLanguage), getLanguageName(detectedLanguage), userQuery);
            
            String englishQuery = callOpenAIAPIForTranslation(prompt);
            logger.info("✅ Query translated to English: '{}'", englishQuery);
            return englishQuery;
            
        } catch (Exception e) {
            logger.warn("⚠️ Failed to translate query to English, using original: {}", e.getMessage());
            return userQuery; // Fallback to original query
        }
    }
    
    /**
     * Enhanced query processing with translation pipeline:
     * 1. Detect user language
     * 2. Translate query to English for database search
     * 3. Process with English query to find data
     * 4. Translate AI response back to user's language
     */
    public String processUserQueryWithTranslationPipeline(String userQuery, String sessionId, String targetLanguage) {
        Instant startTime = Instant.now();

        try {
            logger.info("🌐 Starting translation pipeline for query: '{}' (Session: {}, Target: {})", userQuery, sessionId, targetLanguage);

            // Step 1: Validate input
            if (userQuery == null || userQuery.trim().isEmpty()) {
                String emptyMessage = "💬 **No Message Received** - Please ask your question about PPC Bank services.";
                return translateToTargetLanguage(emptyMessage, targetLanguage);
            }

            // Step 2: Language Detection
            String detectedLanguage = detectLanguage(userQuery);
            logger.info("🔍 Detected input language: {}", detectedLanguage);
            
            // Step 3: Determine response language
            String responseLanguage = determineResponseLanguage(targetLanguage, detectedLanguage);
            logger.info("📝 Response will be in: {}", responseLanguage);

            // Step 4: Translate query to English for database search (if needed)
            String englishQuery = translateQueryToEnglish(userQuery, detectedLanguage);
            logger.info("🔄 Processing with English query: '{}'", englishQuery);

            String normalizedQuery = normalizeQuery(englishQuery);

            // Step 5: Quick greet shortcut
            if (isGreeting(normalizedQuery)) {
                logger.debug("⚡ Greeting detected, fast reply.");
                String greetingMessage = """
                        👋 **Hello! Welcome to PPC Bank!**

                        I can assist with:
                        • 🏦 Services and products
                        • 💳 Account info and requirements
                        • 🔍 Transaction status
                        • 📍 Branch info and contact
                        • ❓ General banking questions

                        How may I help you today?
                        """;
                
                return translateToTargetLanguage(greetingMessage, responseLanguage);
            }

            // Step 6: Get workflow data using English query for better matching
            var workflowData = workflowRepository.findAllActiveWorkflowsByTitle(englishQuery);
            
            // Step 7: Check if it's a transaction inquiry (use original query for data extraction)
            if (transactionAIService.isTransactionInquiry(englishQuery) || containsTransactionKeywords(englishQuery)) {
                logger.info("🔁 Transaction inquiry detected");
                String response = handleTransactionInquiry(workflowData, userQuery, englishQuery, sessionId);
                return translateToTargetLanguage(response, responseLanguage);
            }
            System.err.println("english: " + englishQuery);
            // Step 8: Check if the query is about account blocked (use original query for data extraction)
            if (containsAccountBlockedKeywords(englishQuery)) {
                logger.info("🔒 Account blocked detected");
                String response = handleAccountBlocked(workflowData, englishQuery, sessionId);
                return translateToTargetLanguage(response, responseLanguage);
            }

            // Step 9: Detect query intent using English query
            String queryIntent = jsonResponseService.detectQueryIntent(englishQuery);

            // Step 10: Perform database query using English query
            logger.info("🎯 Performing database query with English translation...");
            
            // Check and log current search mode
            boolean isDbOnlyMode = searchConfigService.isDbOnlyMode();
            logger.info("🔍 Current search mode: {} ({})", 
                isDbOnlyMode ? "Database Only" : "Hybrid Search",
                isDbOnlyMode ? "Internal data only" : "Internal + External sources");
            
            List<TitleMatchResult> matches = titleMatchingService.matchTitles(englishQuery, 5);

            if (!matches.isEmpty()) {
                logger.info("✅ {} title match(es) found using English query", matches.size());

                // Step 11: Create clean JSON context from matched data
                String jsonContext = jsonResponseService.createCleanAIResponse(matches, englishQuery);

                // Step 12: Store context for future use
                conversationContexts.put(sessionId, new ConversationContext(jsonContext, Instant.now()));

                // Step 13: Build prompt with English query and context
                PromptBuilder builder = new PromptBuilder(englishQuery);
                builder.addSmartJsonPrompt(jsonContext, englishQuery, queryIntent);
                builder.addConditionalTable(true, "Requirements");
                builder.addConditionalTable(true, "Amounts & Fees");
                builder.addConditionalTable(true, "Interest Rates");
                addRelevantTableTypes(builder, queryIntent, englishQuery, jsonContext);
                String prompt = builder.build();

                // Step 14: Get AI response in English first
                String englishResponse = callOpenAIAPI(prompt, "en"); // Force English response
                logger.info("✅ English AI response generated in {}ms", Duration.between(startTime, Instant.now()).toMillis());

                // Step 15: Translate response to user's language
                String finalResponse = translateToTargetLanguage(englishResponse, responseLanguage);

                Duration totalTime = Duration.between(startTime, Instant.now());
                logger.info("🌐 Translation pipeline completed successfully in {}ms with {} matches", 
                           totalTime.toMillis(), matches.size());

                return finalResponse;
            }

            // Step 16: No title match found
            logger.warn("⚠️ No title matches found for English query: '{}'", englishQuery);
            String noMatchMessage = "❓ I'm sorry, I couldn't find any PPC Bank information related to your question. Please try rephrasing it or contact PPC Bank directly.";
            
            return translateToTargetLanguage(noMatchMessage, responseLanguage);

        } catch (Exception e) {
            logger.error("💥 Error in translation pipeline: {}", e.getMessage(), e);

            // Handle timeout errors
            if (e instanceof java.net.SocketTimeoutException ||
                    (e.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.warn("⏰ AI API timeout detected");
                String timeoutResponse = generateTimeoutResponse(userQuery);
                return translateToTargetLanguage(timeoutResponse, targetLanguage);
            }

            String errorResponse = generateErrorResponse(e, userQuery);
            return translateToTargetLanguage(errorResponse, targetLanguage);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determine the appropriate response language based on target and detected languages
     */
    private String determineResponseLanguage(String targetLanguage, String detectedLanguage) {
        // If target language is explicitly specified and not auto-detect
        if (targetLanguage != null && !targetLanguage.isEmpty() && 
            !targetLanguage.equalsIgnoreCase("auto") && !targetLanguage.equalsIgnoreCase("detect")) {
            return targetLanguage;
        }
        
        // If auto-detect mode, use detected language (fallback to English)
        return detectedLanguage != null ? detectedLanguage : "en";
    }

    /**
     * Helper method to translate text to target language (with fallback handling)
     */
    private String translateToTargetLanguage(String text, String targetLanguage) {
        try {
            if (targetLanguage == null || targetLanguage.equals("en")) {
                return text; // No translation needed
            }
            
            logger.info("🌐 Translating response to: {}", targetLanguage);
            return translateText(text, targetLanguage);
            
        } catch (Exception e) {
            logger.warn("⚠️ Translation failed, returning original text: {}", e.getMessage());
            return text; // Fallback to original text
        }
    }

    
}
