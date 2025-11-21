package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Service for processing images - OCR and text extraction
 */
@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    @Value("${ai.provider}")
    private AiProvider activeProvider;

    @Value("${ai.openai.api-key}")
    private String openAIApiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private enum AiProvider {
        OPENAI, GEMINI, ANTHROPIC, DEEPSEEK
    }

    public String extractTextFromImage(MultipartFile image) throws IOException {
        logger.info("🔍 Extracting text from image using AI vision: {}", image.getOriginalFilename());

        validateImageFile(image);
        String extractedText = extractTextFromImageUsingAI(image);

        if (extractedText == null || extractedText.trim().isEmpty()) {
            logger.warn("⚠️ No text extracted from image: {}", image.getOriginalFilename());
            return "No readable text found in the image.";
        }

        logger.info("✅ Successfully extracted {} characters from image", extractedText.length());
        return extractedText.trim();
    }

    public String extractTransactionDataFromImage(MultipartFile imageFile) throws IOException {
        try {
            String base64Image = encodeImageToBase64(imageFile);

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

    private String extractTextFromImageUsingAI(MultipartFile imageFile) throws IOException {
        try {
            String base64Image = encodeImageToBase64(imageFile);

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

    private String extractTextUsingOpenAIVision(String base64Image) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> textContent = Map.of("type", "text", "text",
                "Extract all readable text from this image. Return only the text content, no descriptions or formatting.");

        Map<String, Object> imageContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageContent));

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(userMessage),
                "max_tokens", 1000);

        return callOpenAIVisionAPI(url, requestBody);
    }

    private String extractTransactionTextUsingOpenAIVision(String base64Image) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";

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

        return callOpenAIVisionAPI(url, requestBody);
    }

    private String callOpenAIVisionAPI(String url, Map<String, Object> requestBody) throws IOException {
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

    private String encodeImageToBase64(MultipartFile imageFile) throws IOException {
        try {
            byte[] imageBytes = imageFile.getBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to encode image to base64: {}", e.getMessage());
            throw new IOException("Failed to process image file");
        }
    }

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
}
