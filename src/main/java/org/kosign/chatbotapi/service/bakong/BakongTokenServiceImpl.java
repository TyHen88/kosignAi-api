// package org.kosign.chatbotapi.service.bakong;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.kosign.chatbotapi.components.common.api.StatusCode;
// import org.kosign.chatbotapi.exception.BusinessException;
// import org.kosign.chatbotapi.model.BakongToken;
// import org.kosign.chatbotapi.payload.bakong.RenewTokenRequest;
// import org.kosign.chatbotapi.payload.bakong.RenewTokenResponse;
// import org.kosign.chatbotapi.repository.BakongTokenRepository;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.Optional;

// /**
//  * Simplified implementation of BakongTokenService with database persistence and token renewal integration
//  */
// @Service
// @Slf4j
// @RequiredArgsConstructor
// public class BakongTokenServiceImpl implements BakongTokenService {

//     private final BakongTokenRepository bakongTokenRepository;
//     private final TokenRenewalService tokenRenewalService;
    
//     // Constants
//     private static final String SERVICE_NAME_TRANSACTION = "transaction_service";
//     private static final int DEFAULT_BUFFER_MINUTES = 5;
//     private static final int DEFAULT_TOKEN_EXPIRY_HOURS = 1;
    
//     @Override
//     @Cacheable(value = "bakong-tokens", key = "#serviceName + ':' + #email")
//     public String getValidAccessToken(String serviceName, String email) {
//         log.debug("🔑 Requesting valid access token for service: {}, email: {}", serviceName, email);
        
//         // First, check if we have a valid token in the database
//         Optional<BakongToken> existingToken = findValidTokenWithBuffer(serviceName, email);
//         if (existingToken.isPresent()) {
//             BakongToken token = existingToken.get();
//             log.info("✅ Using existing valid token for {} (expires: {})", serviceName, token.getExpiresAt());
//             return token.getAccessToken();
//         }
        
//         // No valid token found, need to renew
//         log.info("🔄 No valid token found, renewing for service: {}, email: {}", serviceName, email);
//         return renewAndStoreToken(serviceName, email);
//     }
    
//     @Override
//     @Transactional
//     public BakongToken storeToken(String serviceName, String email, String accessToken, int expiryHours) {
//         log.info("💾 Storing new token for service: {}, email: {}, expires in: {} hours", 
//             serviceName, email, expiryHours);
        
//         try {
//             // Delete any existing token for this service/email combination
//             int deletedCount = bakongTokenRepository.deleteByServiceAndEmail(serviceName, email);
//             if (deletedCount > 0) {
//                 log.debug("🗑️ Deleted {} existing tokens for {}/{}", deletedCount, serviceName, email);
//             }
            
//             // Create new token entity
//             BakongToken newToken = BakongToken.builder()
//                 .serviceName(serviceName)
//                 .email(email)
//                 .accessToken(accessToken)
//                 .expiresAt(LocalDateTime.now().plusHours(expiryHours))
//                 .build();
            
//             // Save to database
//             BakongToken savedToken = bakongTokenRepository.save(newToken);
            
//             // Clear cache to ensure fresh data
//             clearCache(serviceName, email);
            
//             log.info("✅ Successfully stored token with ID: {} (expires: {})", 
//                 savedToken.getId(), savedToken.getExpiresAt());
            
//             return savedToken;
            
//         } catch (Exception e) {
//             log.error("💥 Failed to store token for {}/{}: {}", serviceName, email, e.getMessage(), e);
//             throw new BusinessException(StatusCode.BAD_REQUEST, 
//                 "🔐 **Token Storage Error** - Failed to securely store authentication token. Please try again.");
//         }
//     }
    
//     @Override
//     public boolean hasValidToken(String serviceName, String email) {
//         LocalDateTime bufferTime = LocalDateTime.now().plusMinutes(DEFAULT_BUFFER_MINUTES);
//         return bakongTokenRepository.hasValidToken(serviceName, email, bufferTime);
//     }
    
//     @Override
//     public Optional<BakongToken> findValidToken(String serviceName, String email) {
//         return findValidTokenWithBuffer(serviceName, email);
//     }
    
//     @Override
//     @Transactional
//     @CacheEvict(value = "bakong-tokens", key = "#serviceName + ':' + #email")
//     public void invalidateTokens(String serviceName, String email) {
//         log.info("🗑️ Invalidating all tokens for service: {}, email: {}", serviceName, email);
        
//         int deletedCount = bakongTokenRepository.deleteByServiceAndEmail(serviceName, email);
        
//         log.info("✅ Invalidated {} tokens for {}/{}", deletedCount, serviceName, email);
//     }
    
//     @Override
//     @Transactional
//     public int cleanupExpiredTokens() {
//         log.info("🧹 Starting cleanup of expired tokens...");
        
//         int deletedCount = bakongTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        
//         log.info("✅ Cleanup complete: deleted {} expired tokens", deletedCount);
        
//         return deletedCount;
//     }
    
//     // === Private Helper Methods ===
    
//     private String renewAndStoreToken(String serviceName, String email) {
//         try {
//             log.info("🔄 Renewing token for service: {}, email: {}", serviceName, email);
            
//             // Create renewal request
//             RenewTokenRequest renewRequest = new RenewTokenRequest();
//             renewRequest.setEmail(email);
            
//             // Call the renewal service
//             RenewTokenResponse response = tokenRenewalService.renewToken(renewRequest);
            
//             if (response == null || response.getAccessToken() == null) {
//                 throw new BusinessException(StatusCode.AUTHENTICATION_FAILED,
//                     "🔐 **Token Renewal Failed** - Unable to obtain new authentication token.");
//             }
            
//             String newToken = response.getAccessToken();
            
//             // Store the new token in database
//             storeToken(serviceName, email, newToken, DEFAULT_TOKEN_EXPIRY_HOURS);
            
//             log.info("✅ Successfully renewed and stored token for {}/{}", serviceName, email);
            
//             return newToken;
            
//         } catch (BusinessException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("💥 Failed to renew token for {}/{}: {}", serviceName, email, e.getMessage(), e);
//             throw new BusinessException(StatusCode.AUTHENTICATION_FAILED,
//                 "🔐 **Token Renewal Error** - Failed to renew authentication token. Please try again.");
//         }
//     }
    
//     private Optional<BakongToken> findValidTokenWithBuffer(String serviceName, String email) {
//         LocalDateTime bufferTime = LocalDateTime.now().plusMinutes(DEFAULT_BUFFER_MINUTES);
//         return bakongTokenRepository.findValidTokenWithBuffer(serviceName, email, bufferTime);
//     }
    
//     @CacheEvict(value = "bakong-tokens", key = "#serviceName + ':' + #email")
//     private void clearCache(String serviceName, String email) {
//         // This method signature ensures Spring cache eviction
//         log.debug("🗑️ Cleared cache for {}/{}", serviceName, email);
//     }
// } 