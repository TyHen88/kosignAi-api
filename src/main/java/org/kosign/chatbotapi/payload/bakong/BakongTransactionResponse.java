package org.kosign.chatbotapi.payload.bakong;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BakongTransactionResponse {
    private Integer responseCode;
    private String responseMessage;
    private String errorCode;
    private TransactionData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionData {
        private String hash;
        private String fromAccountId;
        private String toAccountId;
        private String currency;
        private Number amount;
        private String description;
        private Float createdDateMs;
        private Float acknowledgedDateMs;
        private String trackingStatus;
        private String receiverBank;
        private String receiverBankAccount;
        private String instructionRef;
        private String externalRef;
    }
    
    // Helper methods for backward compatibility and easy access
    public String getHash() {
        return data != null ? data.getHash() : null;
    }
    
    public String getFromAccountId() {
        return data != null ? data.getFromAccountId() : null;
    }
    
    public String getToAccountId() {
        return data != null ? data.getToAccountId() : null;
    }
    
    public String getCurrency() {
        return data != null ? data.getCurrency() : null;
    }
    
    public Number getAmount() {
        return data != null ? data.getAmount() : null;
    }
    
    public String getDescription() {
        return data != null ? data.getDescription() : null;
    }
    
    public Float getCreatedDateMs() {
        return data != null ? data.getCreatedDateMs() : null;
    }
    
    public Float getAcknowledgedDateMs() {
        return data != null ? data.getAcknowledgedDateMs() : null;
    }
    
    public String getTrackingStatus() {
        return data != null ? data.getTrackingStatus() : null;
    }
    
    public String getReceiverBank() {
        return data != null ? data.getReceiverBank() : null;
    }
    
    public String getReceiverBankAccount() {
        return data != null ? data.getReceiverBankAccount() : null;
    }
}
