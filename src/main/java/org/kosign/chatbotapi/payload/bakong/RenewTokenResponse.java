package org.kosign.chatbotapi.payload.bakong;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewTokenResponse {
    private Integer responseCode;
    private String responseMessage;
    private String errorCode;
    private TokenData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenData {
        private String token;
    }
    
    // Helper method to get the access token
    public String getAccessToken() {
        return data != null ? data.getToken() : null;
    }
    
    // Keep for backward compatibility
    public String getRefreshToken() {
        return null; // Not provided by this API
    }
}