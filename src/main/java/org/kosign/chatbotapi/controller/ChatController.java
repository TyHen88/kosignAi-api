package org.kosign.chatbotapi.controller;

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
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private AIService aiService;

    @PostMapping(value = "/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processQuery(
            @RequestParam("query") String userQuery,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Query cannot be empty"));
            }
            String extractedText = aiService.extractTextFromImage(image); 
            String fullPrompt = userQuery;
            if (image != null) {
                fullPrompt = userQuery + "\n\nExtracted from image:\n" + extractedText;
            }
            logger.info("Received chat query: {}", fullPrompt);

            String response = aiService.processUserQuery(fullPrompt);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("query", userQuery);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing chat query: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    createErrorResponse("Internal server error: " + e.getMessage())
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