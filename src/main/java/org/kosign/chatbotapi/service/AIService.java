package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.kosign.chatbotapi.model.ConversationContext;
import org.kosign.chatbotapi.model.TitleMatchResult;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.kosign.chatbotapi.service.TransactionDataExtractionService.TransactionData;
import org.kosign.chatbotapi.utilAI.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

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
    private TransactionAIService transactionAIService;

    @Autowired
    private TransactionDataExtractionService transactionDataExtractionService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private ResponseFormatterService responseFormatterService;

    @Autowired
    private WorkflowMessageService workflowMessageService;

    @Autowired
    private AccountVerificationService accountVerificationService;

    @Autowired
    private TitleMatchingService titleMatchingService;

    @Autowired
    private JsonResponseService jsonResponseService;

    @Autowired
    private ContextStorageService contextStorageService;

    @Autowired
    private SearchConfigService searchConfigService;

    @Autowired
    private WorkflowRepository workflowRepository;

    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();

    // Optimized HTTP client with connection pooling and better timeouts
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 2 minutes for AI processing
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)) // Connection pooling
            .retryOnConnectionFailure(true) // Retry on connection failures
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // main method that processes the user query
    public String processUserQuery(String userQuery, String targetLanguage) {
        return processUserQueryWithSession(userQuery, "default-session", targetLanguage);
    }

    /**
     * Enhanced method with session context support, intelligent title matching, and
     * real-time translation
     */
    public String processUserQueryWithSession(String userQuery, String sessionId, String targetLanguage) {
        Instant startTime = Instant.now();

        try {
            logger.info("🔍 Processing user query: '{}' (Session: {}, Target Language: {})", userQuery, sessionId,
                    targetLanguage);

            // Step 1: Validate input
            if (userQuery == null || userQuery.trim().isEmpty()) {
                String emptyMessage = "💬 **No Message Received** - Please ask your question about PPC Bank services.";
                // Translate empty message if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() &&
                        !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    return translationService.translateText(emptyMessage, targetLanguage);
                }
                return emptyMessage;
            }

            // Step 2: Auto-detect input language if no target language specified
            String detectedLanguage = null;
            if (targetLanguage == null || targetLanguage.isEmpty()) {
                detectedLanguage = translationService.detectLanguage(userQuery);
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
                    return translationService.translateText(greetingMessage, targetLanguage);
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
                    response = translationService.translateText(response, targetLanguage);
                }

                logger.info("✅ Transaction response ready");
                return response;
            }

            // // Step 5: Check if the query is about account blocked
            // if (containsAccountBlockedKeywords(userQuery)) {
            // logger.info("🔒 Account blocked detected");
            // String response = handleAccountBlocked(workflowData, englishQuery,
            // sessionId);
            //
            // // Translate account blocked response if needed
            // if (targetLanguage != null && !targetLanguage.isEmpty() &&
            // !targetLanguage.equalsIgnoreCase("en") &&
            // !targetLanguage.equalsIgnoreCase("english")) {
            // response = translateText(response, targetLanguage);
            // }
            //
            // logger.info("✅ Account blocked response ready");
            // return response;
            // }

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
                return translationService.translateText(noMatchMessage, targetLanguage);
            }
            return noMatchMessage;

        } catch (Exception e) {
            logger.error("💥 Error while processing user query: {}", e.getMessage(), e);

            // Special handling for timeout errors
            if (e instanceof java.net.SocketTimeoutException ||
                    (e.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.warn("⏰ AI API timeout detected for query: '{}'", userQuery);
                String timeoutResponse = responseFormatterService.generateTimeoutResponse(userQuery);

                // Translate timeout response if needed
                if (targetLanguage != null && !targetLanguage.isEmpty() &&
                        !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                    try {
                        return translationService.translateText(timeoutResponse, targetLanguage);
                    } catch (Exception te) {
                        logger.warn("Failed to translate timeout response: {}", te.getMessage());
                        return timeoutResponse; // Return untranslated if translation fails
                    }
                }
                return timeoutResponse;
            }

            String errorResponse = responseFormatterService.generateErrorResponse(e, userQuery);

            // Translate error response if needed
            if (targetLanguage != null && !targetLanguage.isEmpty() &&
                    !targetLanguage.equalsIgnoreCase("en") && !targetLanguage.equalsIgnoreCase("english")) {
                try {
                    return translationService.translateText(errorResponse, targetLanguage);
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

            // Use AI vision for text extraction
            String extractedText = imageProcessingService.extractTextFromImage(image);

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
    public String callOpenAIAPI(String prompt, String targetLanguage) throws IOException {
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
                        return translationService.translateText(aiResponse, targetLanguage);
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
     * 
     * @param medataData   Workflow metadata
     * @param userQuery    The original user query (for data extraction)
     * @param englishQuery The English translated query (for AI processing)
     * @param sessionId    Session identifier
     */
    private String handleTransactionInquiry(Object medataData, String userQuery, String englishQuery,
            String sessionId) {
        logger.info("Handling transaction inquiry - Original: '{}', English: '{}'", userQuery, englishQuery);
        try {
            // Step 1: Check if user is asking "how to check transaction?"
            if (isAskingHowToCheckTransaction(userQuery) || isAskingHowToCheckTransaction(englishQuery)) {
                String inputTypeGuide = workflowMessageService.extractWorkflowInputType(medataData);
                if (inputTypeGuide != null) {
                    transactionAIService.setInputType(inputTypeGuide);
                }
                logger.info("🔍 User asking 'how to check transaction' - showing guide");
                return transactionAIService.getTransactionDetailsPrompt();
            }

            // Step 2: Extract transaction data from the ORIGINAL user query to preserve
            // exact values
            TransactionData extractedData = transactionDataExtractionService.extractTransactionData(userQuery);
            System.err.println("extractedData: " + extractedData.hash + " | " + extractedData.amount + " | "
                    + extractedData.currency);

            if (extractedData.isComplete()) {
                // Step 3: All data is available, proceed with transaction check
                logger.info("✅ Complete transaction data found, proceeding with transaction check");

                // Check if workflow data exists and extract custom message
                String customMessage = workflowMessageService.extractWorkflowMessage(medataData);

                if (customMessage != null && !customMessage.trim().isEmpty()) {
                    logger.info("🔧 Using custom workflow message");
                    // Set custom message to TransactionAIService
                    transactionAIService.setCustomMessage(customMessage);
                } else {
                    logger.info("📝 No custom workflow message found, will use default message");
                    // Clear any previous custom message to use default
                    transactionAIService.clearCustomMessage();
                }

                // Proceed with transaction check
                BakongTransactionResponse response = transactionAIService.checkTransactionStatus(
                        extractedData.hash,
                        extractedData.amount,
                        extractedData.currency);
                return responseFormatterService.formatTransactionResponse(response, transactionAIService);
            } else {
                // Missing data, request more information
                logger.info("⚠️ Incomplete transaction data, requesting details");
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

                // Fallback to transaction details prompt
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
     * Check if user is asking "how to check transaction"
     */
    private boolean isAskingHowToCheckTransaction(String query) {
        if (query == null)
            return false;

        String lowerQuery = query.toLowerCase().trim();
        return lowerQuery.contains("how to check transaction") ||
                lowerQuery.contains("how do i check transaction") ||
                lowerQuery.contains("how can i check transaction") ||
                lowerQuery.equals("how to check transaction?") ||
                lowerQuery.contains("guide to check transaction") ||
                lowerQuery.contains("steps to check transaction");
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

            // Extract text from image with transaction-focused prompt
            String extractedText = imageProcessingService.extractTransactionDataFromImage(image);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return "❌ **No Text Found**\n\nI couldn't extract any readable text from your receipt image. Please ensure the image is clear and try again.";
            }

            logger.info("📄 Extracted text from receipt: {}", extractedText);

            // Check if user is requesting transaction verification
            if (isTransactionInquiry(extractedText) || containsTransactionKeywords(extractedText)) {
                logger.info("🔍 Transaction details detected in image, attempting verification...");

                // Try to extract transaction details from the image text
                TransactionData transactionData = transactionDataExtractionService
                        .extractTransactionData(extractedText);

                if (transactionData.isComplete()) {
                    // All data found, proceed with verification
                    logger.info("✅ Complete transaction data extracted from image");
                    BakongTransactionResponse response = transactionAIService.checkTransactionStatus(
                            transactionData.hash,
                            transactionData.amount,
                            transactionData.currency);
                    return responseFormatterService.formatTransactionResponse(response, transactionAIService);
                } else if (transactionData.hasPartialData()) {
                    // Partial data found, request missing details
                    return responseFormatterService.generatePartialDataResponse(transactionData, extractedText);
                } else {
                    // No transaction data found, provide helpful guidance
                    return responseFormatterService.generateTransactionGuidanceResponse(extractedText);
                }
            } else {
                // No transaction keywords found, just return extracted text with helpful
                // message
                return responseFormatterService.generateGeneralImageResponse(extractedText);
            }

        } catch (Exception e) {
            logger.error("❌ Error processing transaction receipt image: {}", e.getMessage(), e);
            return "❌ **Error Processing Image**\n\nI encountered an error while processing your receipt image: "
                    + e.getMessage() + "\n\nPlease try again or contact PPC Bank support.";
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

    private boolean containsAccountBlockedKeywords(String text) {
        return accountVerificationService.containsAccountBlockedKeywords(text);
    }

    private boolean containsAccountVerificationData(String text) {
        return accountVerificationService.containsAccountVerificationData(text);
    }

    private String handleAccountBlocked(Object medataData, String userQuery, String sessionId) {
        PromptBuilder promptBuilder = new PromptBuilder(userQuery);
        System.err.println("Account blocked::" + userQuery);
        // Check if user provided structured account verification data
        if (accountVerificationService.containsAccountVerificationData(userQuery)) {
            logger.info("🔍 Account verification data detected, validating completeness...");

            // Validate that all required fields are present
            AccountVerificationService.AccountVerificationResult validationResult = accountVerificationService
                    .validateAccountVerificationData(userQuery);

            if (!validationResult.isValid) {
                logger.warn("❌ Missing required fields for account verification: {}", validationResult.missingFields);
                return accountVerificationService.generateValidationErrorPrompt(validationResult);
            }

            logger.info("✅ All required account verification data found, proceeding with verification");
            promptBuilder.testAccountBlockedPromptVerify(userQuery);
        } else if (containsAccountBlockedKeywords(userQuery) || userQuery.contains("How to check account blocked?")) {
            logger.info("🔒 Account blocked keywords detected, using standard prompt");
            promptBuilder.workflowPromptWithMetadata(medataData, userQuery, "account-blocked");
            // promptBuilder.simpleTestAccountBlockedPrompt(userQuery);
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

    public String detectLanguage(String text) {
        return translationService.detectLanguage(text);
    }

    public String translateText(String text, String targetLanguage) {
        return translationService.translateText(text, targetLanguage);
    }

    public String translateQueryToEnglish(String userQuery, String detectedLanguage) {
        return translationService.translateQueryToEnglish(userQuery, detectedLanguage);
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
            logger.info("🌐 Starting translation pipeline for query: '{}' (Session: {}, Target: {})", userQuery,
                    sessionId, targetLanguage);

            // Step 1: Validate input
            if (userQuery == null || userQuery.trim().isEmpty()) {
                String emptyMessage = "💬 **No Message Received** - Please ask your question about PPC Bank services.";
                return translationService.translateToTargetLanguage(emptyMessage, targetLanguage);
            }

            // Step 2: Language Detection
            String detectedLanguage = translationService.detectLanguage(userQuery);
            logger.info("🔍 Detected input language: {}", detectedLanguage);

            // Step 3: Determine response language
            String responseLanguage = translationService.determineResponseLanguage(targetLanguage, detectedLanguage);
            logger.info("📝 Response will be in: {}", responseLanguage);

            // Step 4: Translate query to English for database search (if needed)
            String englishQuery = translationService.translateQueryToEnglish(userQuery, detectedLanguage);
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

                return translationService.translateToTargetLanguage(greetingMessage, responseLanguage);
            }

            // Step 6: Get workflow data using English query for better matching
            var workflowData = workflowRepository.findAllActiveWorkflowsByTitle(englishQuery);

            // Step 7: Check if it's a transaction inquiry (use original query for data
            // extraction)
            if (transactionAIService.isTransactionInquiry(englishQuery) || containsTransactionKeywords(englishQuery)) {
                logger.info("🔁 Transaction inquiry detected");
                String response = handleTransactionInquiry(workflowData, userQuery, englishQuery, sessionId);
                return translationService.translateToTargetLanguage(response, responseLanguage);
            }

            // // Step 8: Check if the query is about account blocked (use original query
            // for data extraction)
            // if (containsAccountBlockedKeywords(englishQuery)) {
            // logger.info("🔒 Account blocked detected");
            // String response = handleAccountBlocked(workflowData, englishQuery,
            // sessionId);
            // return translateToTargetLanguage(response, responseLanguage);
            // }

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
                logger.info("✅ English AI response generated in {}ms",
                        Duration.between(startTime, Instant.now()).toMillis());

                // Step 15: Translate response to user's language
                String finalResponse = translationService.translateToTargetLanguage(englishResponse, responseLanguage);

                Duration totalTime = Duration.between(startTime, Instant.now());
                logger.info("🌐 Translation pipeline completed successfully in {}ms with {} matches",
                        totalTime.toMillis(), matches.size());

                return finalResponse;
            }

            // Step 16: No title match found
            logger.warn("⚠️ No title matches found for English query: '{}'", englishQuery);
            String noMatchMessage = "❓ I'm sorry, I couldn't find any PPC Bank information related to your question. Please try rephrasing it or contact PPC Bank directly.";

            return translationService.translateToTargetLanguage(noMatchMessage, responseLanguage);

        } catch (Exception e) {
            logger.error("💥 Error in translation pipeline: {}", e.getMessage(), e);

            // Handle timeout errors
            if (e instanceof java.net.SocketTimeoutException ||
                    (e.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.warn("⏰ AI API timeout detected");
                String timeoutResponse = responseFormatterService.generateTimeoutResponse(userQuery);
                return translationService.translateToTargetLanguage(timeoutResponse, targetLanguage);
            }

            String errorResponse = responseFormatterService.generateErrorResponse(e, userQuery);
            return translationService.translateToTargetLanguage(errorResponse, targetLanguage);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
