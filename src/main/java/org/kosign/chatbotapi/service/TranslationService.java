package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for language detection and translation
 */
@Service
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    @Value("${ai.openai.api-key}")
    private String openAIApiKey;

    @Value("${ai.openai.model}")
    private String openAIModel;

    private final Map<String, String> translationCache = new ConcurrentHashMap<>();
    private final Map<String, String> languageDetectionCache = new ConcurrentHashMap<>();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable(value = "language-detection-cache", key = "#text.hashCode()")
    public String detectLanguage(String text) {
        String cached = languageDetectionCache.get(text);
        if (cached != null) {
            return cached;
        }

        try {
            String prompt = String.format(
                    """
                            Detect the language of this text and return only the language code (e.g., 'en', 'km', 'zh', 'ja', 'ko', 'th', 'vi', 'fr', 'de', 'es'):

                            Text: "%s"

                            Return only the 2-letter language code, nothing else.
                            """,
                    text.replace("\"", "\\\""));

            String response = callOpenAIAPIForTranslation(prompt);
            String languageCode = response.trim().toLowerCase();

            if (languageDetectionCache.size() < 1000) {
                languageDetectionCache.put(text, languageCode);
            }

            return languageCode;
        } catch (Exception e) {
            logger.warn("Language detection failed, defaulting to English: {}", e.getMessage());
            return "en";
        }
    }

    @Cacheable(value = "translation-cache", key = "#text.hashCode() + '_' + #targetLanguage")
    public String translateText(String text, String targetLanguage) {
        try {
            if (targetLanguage == null || targetLanguage.isEmpty() || targetLanguage.equals("en")) {
                return text;
            }

            String cacheKey = text.hashCode() + "_" + targetLanguage;
            String cached = translationCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            String languageName = getLanguageName(targetLanguage);
            String prompt = String.format(
                    """
                            Translate the following text to %s. Maintain the original formatting, structure, and any special characters like emojis or markdown:

                            Text to translate:
                            %s

                            Translation:
                            """,
                    languageName, text);

            String translated = callOpenAIAPIForTranslation(prompt);

            if (translationCache.size() < 2000) {
                translationCache.put(cacheKey, translated);
            }

            return translated;
        } catch (Exception e) {
            logger.error("Translation failed for language '{}': {}", targetLanguage, e.getMessage());
            return text;
        }
    }

    public String translateQueryToEnglish(String userQuery, String detectedLanguage) {
        try {
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
            return userQuery;
        }
    }

    public String determineResponseLanguage(String targetLanguage, String detectedLanguage) {
        if (targetLanguage != null && !targetLanguage.isEmpty() &&
                !targetLanguage.equalsIgnoreCase("auto") && !targetLanguage.equalsIgnoreCase("detect")) {
            return targetLanguage;
        }
        return detectedLanguage != null ? detectedLanguage : "en";
    }

    public String translateToTargetLanguage(String text, String targetLanguage) {
        try {
            if (targetLanguage == null || targetLanguage.equals("en")) {
                return text;
            }

            logger.info("🌐 Translating response to: {}", targetLanguage);
            return translateText(text, targetLanguage);
        } catch (Exception e) {
            logger.warn("⚠️ Translation failed, returning original text: {}", e.getMessage());
            return text;
        }
    }

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
        requestBodyMap.put("temperature", 0.3);

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
}
