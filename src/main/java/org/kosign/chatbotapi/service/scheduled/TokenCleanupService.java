// package org.kosign.chatbotapi.service.scheduled;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.kosign.chatbotapi.service.bakong.BakongTokenService;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// /**
//  * Scheduled service for cleaning up expired tokens automatically
//  */
// @Service
// @Slf4j
// @RequiredArgsConstructor
// @ConditionalOnProperty(value = "bakong.token.cleanup.enabled", havingValue = "true", matchIfMissing = true)
// public class TokenCleanupService {

//     private final BakongTokenService bakongTokenService;

//     /**
//      * Clean up expired tokens every 6 hours
//      */
//     @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
//     public void cleanupExpiredTokens() {
//         try {
//             log.info("🧹 Starting scheduled token cleanup...");
            
//             int cleanedCount = bakongTokenService.cleanupExpiredTokens();
            
//             if (cleanedCount > 0) {
//                 log.info("✅ Token cleanup completed: {} tokens processed", cleanedCount);
//             } else {
//                 log.debug("ℹ️ Token cleanup completed: no expired tokens found");
//             }
            
//         } catch (Exception e) {
//             log.error("💥 Error during token cleanup: {}", e.getMessage(), e);
//         }
//     }

//     /**
//      * Log token health check every hour for monitoring
//      */
//     @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
//     public void logTokenHealth() {
//         try {
//             boolean hasValidToken = bakongTokenService.hasValidToken(
//                 "transaction_service", 
//                 "hentyna11@gmail.com"
//             );
            
//             if (hasValidToken) {
//                 log.info("📊 Token Health: ✅ Valid token available");
//             } else {
//                 log.info("📊 Token Health: ⚠️ No valid token found - will renew on next API call");
//             }
            
//         } catch (Exception e) {
//             log.warn("⚠️ Failed to check token health: {}", e.getMessage());
//         }
//     }
// } 