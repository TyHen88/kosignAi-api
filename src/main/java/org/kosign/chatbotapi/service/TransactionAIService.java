package org.kosign.chatbotapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionRequest;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.kosign.chatbotapi.service.bakong.BakongTransactionService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Simplified service for transaction checking
 * Returns raw transaction data for API formatting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAIService {
    private String messages = "";
    private String inputType = "";

    public void setCustomMessage(String customMessage) {
        this.messages = customMessage != null ? customMessage : "";
    }

    public String getCustomMessage() {
        return this.messages;
    }

    public String getInputType() {
        return this.inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public void clearCustomMessage() {
        this.messages = "";
    }

    private final BakongTransactionService bakongTransactionService;

    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^(USD|KHR)$");

    private static final String[] TRANSACTION_KEYWORDS = {
            "transaction error", "payment failed", "check transaction", "transaction status",
            "payment issue", "money not received", "transfer problem", "payment stuck",
            "transaction pending", "payment not working", "failed payment", "transaction failed",
            "money missing", "payment not completed", "transfer not received", "bakong error",
            "payment gateway", "transaction hash", "payment timeout", "money lost",
            "verify transaction", "payment verification", "transaction receipt", "payment receipt",
            "bakong hash", "payment confirmation", "transfer status", "transaction inquiry",
            "external transaction reference", "payment error", "transfer failed"
    };

    @Cacheable(value = "transaction-inquiry-cache", key = "#userMessage.hashCode()")
    public boolean isTransactionInquiry(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }
        String message = userMessage.toLowerCase().trim();
        return java.util.Arrays.stream(TRANSACTION_KEYWORDS)
                .anyMatch(message::contains);
    }

    public TransactionValidationResult validateTransactionInput(String hash, String amountStr, String currency) {
        TransactionValidationResult result = new TransactionValidationResult();
        validateHash(hash, result);
        validateAmount(amountStr, result);
        validateCurrency(currency, result);
        return result;
    }

    private void validateHash(String hash, TransactionValidationResult result) {
        if (hash == null || hash.trim().isEmpty()) {
            result.addError("Transaction hash is required");
        } else {
            String cleanHash = hash.trim();
            if (cleanHash.length() < 4 || cleanHash.length() > 64) {
                result.addError("Transaction hash must be between 4 and 64 characters");
            } else if (!HASH_PATTERN.matcher(cleanHash).matches()) {
                result.addError("Transaction hash must contain only letters and numbers");
            }
        }
    }

    private void validateAmount(String amountStr, TransactionValidationResult result) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            result.addError("Amount is required");
        } else {
            String cleanAmount = amountStr.trim();
            if (!AMOUNT_PATTERN.matcher(cleanAmount).matches()) {
                result.addError("Invalid amount format");
            } else {
                try {
                    double amount = Double.parseDouble(cleanAmount);
                    if (amount <= 0 || amount > 1000000) {
                        result.addError("Amount must be between 0 and 1,000,000");
                    }
                } catch (NumberFormatException e) {
                    result.addError("Invalid amount format");
                }
            }
        }
    }

    private void validateCurrency(String currency, TransactionValidationResult result) {
        if (currency == null || currency.trim().isEmpty()) {
            result.addError("Currency is required");
        } else {
            String cleanCurrency = currency.trim().toUpperCase();
            if (!CURRENCY_PATTERN.matcher(cleanCurrency).matches()) {
                result.addError("Currency must be USD or KHR");
            }
        }
    }

    public BakongTransactionResponse checkTransactionStatus(String hash, String amountStr, String currency) {
        return checkTransactionStatus(hash, amountStr, currency, null);
    }

    public BakongTransactionResponse checkTransactionStatus(String hash, String amountStr, String currency,
            String workflowMessage) {
        if (workflowMessage != null && !workflowMessage.trim().isEmpty()) {
            setCustomMessage(workflowMessage);
        }

        log.info("Checking transaction status for hash: {}", hash);

        if (bakongTransactionService == null) {
            log.error("BakongTransactionService is not available");
            throw new IllegalStateException("Transaction service is not available");
        }

        TransactionValidationResult validation = validateTransactionInput(hash, amountStr, currency);
        if (!validation.isValid()) {
            log.warn("Validation failed: {}", validation.getErrors());
            throw new IllegalArgumentException(String.join(", ", validation.getErrors()));
        }

        try {
            Number amount = Double.parseDouble(amountStr.trim());
            BakongTransactionRequest request = BakongTransactionRequest.builder()
                    .hash(hash.trim())
                    .amount(amount)
                    .currency(currency.trim().toUpperCase())
                    .build();

            return bakongTransactionService.checkTransactionStatus(request);

        } catch (Exception e) {
            log.error("Error checking transaction status: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String getTransactionDetailsPrompt() {
        String inputTypeValue = getInputType();

        return switch (inputTypeValue) {
            case "both" -> """
                    To verify your transaction, please:
                    - Upload your payment slip, OR
                    - Enter transaction details manually:
                      • Transaction Hash (8-64 characters)
                      • Amount (e.g., 50 or 25.75)
                      • Currency (USD or KHR)
                    """;
            case "image_payment_slip" -> "Please upload your payment slip to verify the transaction.";
            case "text_hash_payment" -> """
                    Please provide:
                    • Transaction Hash (8-64 characters, e.g., c250339a)
                    • Amount (e.g., 50 or 25.75)
                    • Currency (USD or KHR)
                    """;
            default -> "Please provide transaction details to check status.";
        };
    }

    public static class TransactionValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }
    }
}