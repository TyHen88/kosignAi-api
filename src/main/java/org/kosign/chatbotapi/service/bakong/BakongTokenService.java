// package org.kosign.chatbotapi.service.bakong;

// import org.kosign.chatbotapi.model.BakongToken;
// import java.util.Optional;

// /**
//  * Simplified service for managing Bakong API tokens
//  */
// public interface BakongTokenService {
    
//     /**
//      * Get a valid access token, renewing if necessary
//      * @param serviceName the service name (usually "transaction_service")
//      * @param email the email for token renewal
//      * @return valid access token
//      */
//     String getValidAccessToken(String serviceName, String email);
    
//     /**
//      * Store a new token, replacing any existing one
//      * @param serviceName the service name
//      * @param email the email
//      * @param accessToken the access token
//      * @param expiryHours hours until expiry
//      * @return saved token entity
//      */
//     BakongToken storeToken(String serviceName, String email, String accessToken, int expiryHours);
    
//     /**
//      * Check if a valid token exists
//      * @param serviceName the service name
//      * @param email the email
//      * @return true if valid token exists
//      */
//     boolean hasValidToken(String serviceName, String email);
    
//     /**
//      * Find existing valid token
//      * @param serviceName the service name
//      * @param email the email
//      * @return optional token if found and valid
//      */
//     Optional<BakongToken> findValidToken(String serviceName, String email);
    
//     /**
//      * Invalidate (delete) tokens for service and email
//      * @param serviceName the service name
//      * @param email the email
//      */
//     void invalidateTokens(String serviceName, String email);
    
//     /**
//      * Clean up expired tokens
//      * @return number of tokens cleaned up
//      */
//     int cleanupExpiredTokens();
// } 