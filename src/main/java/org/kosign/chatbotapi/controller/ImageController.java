//package org.kosign.chatbotapi.controller;
//
//import org.kosign.chatbotapi.service.AIService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*") // Allow CORS for frontend integration
//public class ImageController {
//
//    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
//
//    @Autowired
//    private AIService aiService;
//
//    /**
//     * Upload image, extract text via OCR, and query database
//     */
//    @PostMapping("/image/query")
//    public ResponseEntity<Map<String, Object>> processImageQuery(
//            @RequestParam("image") MultipartFile imageFile,
//            @RequestParam(value = "sessionId", defaultValue = "default-session") String sessionId) {
//
//        try {
//            logger.info("Received image query request - File: {}, Session: {}",
//                       imageFile.getOriginalFilename(), sessionId);
//
//            if (imageFile.isEmpty()) {
//                return ResponseEntity.badRequest().body(createErrorResponse("No image file provided"));
//            }
//
//            // Process image with OCR and query database
//            String response = aiService.processImageQuery(imageFile, sessionId);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("success", true);
//            result.put("filename", imageFile.getOriginalFilename());
//            result.put("fileSize", imageFile.getSize());
//            result.put("contentType", imageFile.getContentType());
//            result.put("sessionId", sessionId);
//            result.put("response", response);
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error processing image query: {}", e.getMessage(), e);
//            return ResponseEntity.internalServerError().body(
//                    createErrorResponse("Error processing image: " + e.getMessage())
//            );
//        }
//    }
//
//    /**
//     * Get image processing capabilities and status
//     */
//    @GetMapping("/image/status")
//    public ResponseEntity<Map<String, Object>> getImageProcessingStatus() {
//        try {
//            Map<String, Object> status = new HashMap<>();
//            status.put("success", true);
//            status.put("ocrEnabled", true);
//            status.put("supportedFormats", new String[]{"JPEG", "PNG", "BMP", "WebP"});
//            status.put("maxFileSize", "20MB");
//            status.put("aiProviders", new String[]{"OpenAI Vision", "Gemini Vision", "Claude Vision"});
//            status.put("features", new String[]{
//                "Text extraction from images",
//                "Automatic database query",
//                "Multi-language support",
//                "Session-based context"
//            });
//
//            return ResponseEntity.ok(status);
//
//        } catch (Exception e) {
//            logger.error("Error getting image processing status: {}", e.getMessage(), e);
//            return ResponseEntity.internalServerError().body(
//                    createErrorResponse("Error getting status: " + e.getMessage())
//            );
//        }
//    }
//
//    /**
//     * Health check for image processing service
//     */
//    @GetMapping("/image/health")
//    public ResponseEntity<Map<String, Object>> healthCheck() {
//        Map<String, Object> health = new HashMap<>();
//        health.put("status", "UP");
//        health.put("service", "Image OCR Processing");
//        health.put("timestamp", System.currentTimeMillis());
//
//        return ResponseEntity.ok(health);
//    }
//
//    /**
//     * Create standardized error response
//     */
//    private Map<String, Object> createErrorResponse(String message) {
//        Map<String, Object> error = new HashMap<>();
//        error.put("success", false);
//        error.put("error", message);
//        error.put("timestamp", System.currentTimeMillis());
//        return error;
//    }
//}