package org.kosign.chatbotapi.controller;

import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
@RequiredArgsConstructor
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private AIService aiService;


    @PostMapping(value = "/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processQuery(
            @RequestParam("query") String userQuery,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "targetLanguage", defaultValue = "auto") String targetLanguage,
            @RequestParam(value = "useTranslationPipeline", defaultValue = "true") boolean useTranslationPipeline
    ) {
        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Query cannot be empty"));
            }

            // Handle language detection/conversion
            String processedTargetLanguage = processTargetLanguage(targetLanguage);
            logger.info("Processing query with target language: {} (processed: {}, pipeline: {})", 
                       targetLanguage, processedTargetLanguage, useTranslationPipeline);

            String response;
            if (image != null) {
                // Check if this is a transaction-related query
                String lowerQuery = userQuery.toLowerCase();
                if (lowerQuery.contains("transaction") || lowerQuery.contains("receipt") ||
                    lowerQuery.contains("payment") || lowerQuery.contains("verify") ||
                    lowerQuery.contains("check")) {

                    logger.info("Processing transaction receipt image with query: {}", userQuery);
                    response = aiService.processTransactionReceiptImage(image, "web-session");

                    // Apply translation if needed
                    if (processedTargetLanguage != null && !processedTargetLanguage.equals("en")) {
                        response = aiService.translateText(response, processedTargetLanguage);
                    }
                } else {
                    // General image processing
                    String extractedText = aiService.extractTextFromImage(image);
                    String fullPrompt = userQuery + "\n\nExtracted from image:\n" + extractedText;
                    logger.info("Processing general image query: {}", userQuery);

                    // Use translation pipeline for image queries too
                    if (useTranslationPipeline) {
                        response = aiService.processUserQueryWithTranslationPipeline(fullPrompt, "web-session", processedTargetLanguage);
                    } else {
                        response = aiService.processUserQuery(fullPrompt, processedTargetLanguage);
                    }
                }
            } else {
                // Text-only query - choose processing method
                logger.info("Processing text query: {}", userQuery);

                if (useTranslationPipeline) {
                    // Use enhanced translation pipeline for better database matching
                    response = aiService.processUserQueryWithTranslationPipeline(userQuery, "web-session", processedTargetLanguage);
                } else {
                    // Use original method
                    response = aiService.processUserQuery(userQuery, processedTargetLanguage);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("query", userQuery);
            result.put("response", response);
            result.put("targetLanguage", targetLanguage);
            result.put("processedLanguage", processedTargetLanguage);
            result.put("useTranslationPipeline", useTranslationPipeline);
            result.put("hasImage", image != null);
            if (image != null) {
                result.put("filename", image.getOriginalFilename());
                result.put("fileSize", image.getSize());
            }
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing chat query: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Internal server error: " + e.getMessage())
            );
        }
    }

    /**
     * Dedicated endpoint for language detection
     */
    @PostMapping("/language/detect")
    public ResponseEntity<Map<String, Object>> detectLanguage(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Text is required"));
            }

            String detectedLanguage = aiService.detectLanguage(text);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("text", text);
            result.put("detectedLanguage", detectedLanguage);
            result.put("languageName", getLanguageDisplayName(detectedLanguage));
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error detecting language: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Language detection error: " + e.getMessage())
            );
        }
    }

    /**
     * Dedicated endpoint for text translation
     */
    @PostMapping("/language/translate")
    public ResponseEntity<Map<String, Object>> translateText(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String targetLanguage = request.get("targetLanguage");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Text is required"));
            }

            if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Target language is required"));
            }

            String processedTargetLanguage = processTargetLanguage(targetLanguage);
            String translatedText = aiService.translateText(text, processedTargetLanguage);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("originalText", text);
            result.put("translatedText", translatedText);
            result.put("targetLanguage", targetLanguage);
            result.put("processedLanguage", processedTargetLanguage);
            result.put("languageName", getLanguageDisplayName(processedTargetLanguage));
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error translating text: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Translation error: " + e.getMessage())
            );
        }
    }

    /**
     * Get supported languages
     */
    @GetMapping("/language/supported")
    public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
        try {
            Map<String, String> languages = new HashMap<>();
            languages.put("en", "English");
            languages.put("km", "Khmer (Cambodian)");
            languages.put("zh", "Chinese (Simplified)");
            languages.put("zh-tw", "Chinese (Traditional)");
            languages.put("ja", "Japanese");
            languages.put("ko", "Korean");
            languages.put("th", "Thai");
            languages.put("vi", "Vietnamese");
            languages.put("fr", "French");
            languages.put("de", "German");
            languages.put("es", "Spanish");
            languages.put("it", "Italian");
            languages.put("pt", "Portuguese");
            languages.put("ru", "Russian");
            languages.put("ar", "Arabic");
            languages.put("hi", "Hindi");
            languages.put("id", "Indonesian");
            languages.put("ms", "Malay");
            languages.put("tl", "Filipino");
            languages.put("my", "Burmese");
            languages.put("lo", "Lao");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("languages", languages);
            result.put("autoDetect", true);
            result.put("defaultLanguage", "en");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error getting supported languages: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Error getting languages: " + e.getMessage())
            );
        }
    }

    /**
     * Process target language parameter
     */
    private String processTargetLanguage(String targetLanguage) {
        if (targetLanguage == null || targetLanguage.trim().isEmpty() ||
            targetLanguage.equalsIgnoreCase("auto") || targetLanguage.equalsIgnoreCase("detect")) {
            return null; // Auto-detect mode
        }

        // Convert common language names to codes
        return switch (targetLanguage.toLowerCase()) {
            case "english" -> "en";
            case "khmer", "cambodian" -> "km";
            case "chinese", "mandarin" -> "zh";
            case "japanese" -> "ja";
            case "korean" -> "ko";
            case "thai" -> "th";
            case "vietnamese" -> "vi";
            case "french" -> "fr";
            case "german" -> "de";
            case "spanish" -> "es";
            case "italian" -> "it";
            case "portuguese" -> "pt";
            case "russian" -> "ru";
            case "arabic" -> "ar";
            case "hindi" -> "hi";
            case "indonesian" -> "id";
            case "malay" -> "ms";
            case "filipino" -> "tl";
            case "burmese" -> "my";
            case "lao" -> "lo";
            default -> targetLanguage.toLowerCase(); // Return as-is if already a code
        };
    }

    /**
     * Get display name for language code
     */
    private String getLanguageDisplayName(String languageCode) {
        if (languageCode == null) return "Auto-detect";

        return switch (languageCode.toLowerCase()) {
            case "en" -> "English";
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
            default -> languageCode;
        };
    }

    /**
     * Dedicated endpoint for transaction receipt verification
     */
    @PostMapping(value = "/transaction/verify-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> verifyTransactionReceipt(
            @RequestParam("receipt") MultipartFile receiptImage,
            @RequestParam(value = "sessionId", defaultValue = "default-session") String sessionId
    ) {
        try {
            logger.info("Received transaction receipt verification request - File: {}, Session: {}",
                       receiptImage.getOriginalFilename(), sessionId);

            if (receiptImage.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No receipt image provided"));
            }

            // Process the transaction receipt
            String response = aiService.processTransactionReceiptImage(receiptImage, sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("filename", receiptImage.getOriginalFilename());
            result.put("fileSize", receiptImage.getSize());
            result.put("contentType", receiptImage.getContentType());
            result.put("sessionId", sessionId);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error verifying transaction receipt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Error processing receipt: " + e.getMessage())
            );
        }
    }

    /**
     * Get transaction verification capabilities and status
     */
    @GetMapping("/transaction/capabilities")
    public ResponseEntity<Map<String, Object>> getTransactionCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("success", true);
            capabilities.put("receiptVerification", true);
            capabilities.put("supportedFormats", new String[]{"JPEG", "PNG", "BMP", "WebP"});
            capabilities.put("maxFileSize", "10MB");
            capabilities.put("extractedData", new String[]{
                "Transaction Hash/ID",
                "Transaction Amount",
                "Currency (USD/KHR)",
                "Transaction Status",
                "Bank Information",
                "Date & Time"
            });
            capabilities.put("verificationFeatures", new String[]{
                "Automatic data extraction",
                "Real-time transaction verification",
                "Bakong API integration",
                "Intelligent error detection",
                "Multi-format receipt support"
            });

            return ResponseEntity.ok(capabilities);

        } catch (Exception e) {
            logger.error("Error getting transaction capabilities: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Error getting capabilities: " + e.getMessage())
            );
        }
    }

    /**
     * Dedicated endpoint using the enhanced translation pipeline
     */
    @PostMapping(value = "/query/with-translation-pipeline", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processQueryWithTranslationPipeline(
            @RequestParam("query") String userQuery,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "targetLanguage", defaultValue = "auto") String targetLanguage,
            @RequestParam(value = "sessionId", defaultValue = "web-session") String sessionId
    ) {
        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Query cannot be empty"));
            }

            String processedTargetLanguage = processTargetLanguage(targetLanguage);
            logger.info("Processing query with translation pipeline - Target: {} (processed: {})",
                       targetLanguage, processedTargetLanguage);

            String response;
            long startTime = System.currentTimeMillis();

            if (image != null) {
                // Extract text and combine with query
                String extractedText = aiService.extractTextFromImage(image);
                String fullPrompt = userQuery + "\n\nExtracted from image:\n" + extractedText;
                logger.info("Processing image query with translation pipeline");

                response = aiService.processUserQueryWithTranslationPipeline(fullPrompt, sessionId, processedTargetLanguage);
            } else {
                // Text-only query using translation pipeline
                logger.info("Processing text query with translation pipeline");
                response = aiService.processUserQueryWithTranslationPipeline(userQuery, sessionId, processedTargetLanguage);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("query", userQuery);
            result.put("response", response);
            result.put("targetLanguage", targetLanguage);
            result.put("processedLanguage", processedTargetLanguage);
            result.put("sessionId", sessionId);
            result.put("processingTimeMs", processingTime);
            result.put("translationPipeline", true);
            result.put("hasImage", image != null);
            if (image != null) {
                result.put("filename", image.getOriginalFilename());
                result.put("fileSize", image.getSize());
            }
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing query with translation pipeline: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Translation pipeline error: " + e.getMessage())
            );
        }
    }

    /**
     * Get translation pipeline capabilities and information
     */
    @GetMapping("/translation/capabilities")
    public ResponseEntity<Map<String, Object>> getTranslationCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("success", true);
            capabilities.put("translationPipeline", true);
            capabilities.put("autoLanguageDetection", true);
            capabilities.put("databaseQueryInEnglish", true);
            capabilities.put("responseTranslation", true);

            Map<String, String> pipelineSteps = new HashMap<>();
            pipelineSteps.put("step1", "Detect input language");
            pipelineSteps.put("step2", "Translate query to English");
            pipelineSteps.put("step3", "Search database with English query");
            pipelineSteps.put("step4", "Generate AI response in English");
            pipelineSteps.put("step5", "Translate response to user's language");
            capabilities.put("pipelineSteps", pipelineSteps);

            capabilities.put("supportedLanguages", 21);
            capabilities.put("fallbackHandling", true);
            capabilities.put("preserveFormatting", true);
            capabilities.put("bankingTerms", "Optimized for banking/financial queries");

            Map<String, String> benefits = new HashMap<>();
            benefits.put("accuracy", "Better database matching with English queries");
            benefits.put("consistency", "Standardized search regardless of input language");
            benefits.put("coverage", "Improved search results for non-English queries");
            benefits.put("userExperience", "Natural language responses in user's preferred language");
            capabilities.put("benefits", benefits);

            capabilities.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(capabilities);

        } catch (Exception e) {
            logger.error("Error getting translation capabilities: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Error getting capabilities: " + e.getMessage())
            );
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Chat service is running");
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}