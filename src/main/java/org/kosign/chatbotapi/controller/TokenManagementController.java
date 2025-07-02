// package org.kosign.chatbotapi.controller;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
// import org.kosign.chatbotapi.service.bakong.BakongTokenService;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.HashMap;
// import java.util.Map;

// /**
//  * Management controller for Bakong token operations (for admin/monitoring purposes)
//  */
// @RestController
// @RequestMapping("/v1/admin/tokens")
// @RequiredArgsConstructor
// @Slf4j
// @ConditionalOnProperty(value = "bakong.token.management.enabled", havingValue = "true", matchIfMissing = true)
// public class TokenManagementController extends ChatAIRestController {

//     private final BakongTokenService bakongTokenService;

    
//     @GetMapping("/health")
//     public ResponseEntity<?> checkTokenHealth() {
//         try {
//             boolean hasValidToken = bakongTokenService.hasValidToken("transaction_service", "hentyna11@gmail.com");
            
//             Map<String, Object> response = new HashMap<>();
//             response.put("success", true);
//             response.put("hasValidToken", hasValidToken);
//             response.put("service", "transaction_service");
//             response.put("timestamp", System.currentTimeMillis());
            
//             return ok(response);
            
//         } catch (Exception e) {
//             log.error("Failed to check token health: {}", e.getMessage(), e);
            
//             Map<String, Object> errorResponse = new HashMap<>();
//             errorResponse.put("success", false);
//             errorResponse.put("error", "Failed to check token health");
//             errorResponse.put("message", e.getMessage());
            
//             return ResponseEntity.badRequest().body(errorResponse);
//         }
//     }

//     /**
//      * Manually trigger token cleanup
//      */
//     @PostMapping("/cleanup")
//     public ResponseEntity<?> cleanupTokens() {
//         try {
//             log.info("Manual token cleanup triggered");
            
//             int cleanedCount = bakongTokenService.cleanupExpiredTokens();
            
//             Map<String, Object> response = new HashMap<>();
//             response.put("success", true);
//             response.put("cleanedCount", cleanedCount);
//             response.put("message", "Token cleanup completed successfully");
//             response.put("timestamp", System.currentTimeMillis());
            
//             return ok(response);
            
//         } catch (Exception e) {
//             log.error("Failed to cleanup tokens: {}", e.getMessage(), e);
            
//             Map<String, Object> errorResponse = new HashMap<>();
//             errorResponse.put("success", false);
//             errorResponse.put("error", "Failed to cleanup tokens");
//             errorResponse.put("message", e.getMessage());
            
//             return ResponseEntity.badRequest().body(errorResponse);
//         }
//     }

//     /**
//      * Manually invalidate tokens for transaction service
//      */
//     @PostMapping("/invalidate")
//     public ResponseEntity<?> invalidateTokens() {
//         try {
//             log.info("Manual token invalidation triggered for transaction service");
            
//             bakongTokenService.invalidateTokens("transaction_service", "hentyna11@gmail.com");
            
//             Map<String, Object> response = new HashMap<>();
//             response.put("success", true);
//             response.put("message", "Tokens invalidated successfully");
//             response.put("service", "transaction_service");
//             response.put("timestamp", System.currentTimeMillis());
            
//             return ok(response);
            
//         } catch (Exception e) {
//             log.error("Failed to invalidate tokens: {}", e.getMessage(), e);
            
//             Map<String, Object> errorResponse = new HashMap<>();
//             errorResponse.put("success", false);
//             errorResponse.put("error", "Failed to invalidate tokens");
//             errorResponse.put("message", e.getMessage());
            
//             return ResponseEntity.badRequest().body(errorResponse);
//         }
//     }

    
// } 