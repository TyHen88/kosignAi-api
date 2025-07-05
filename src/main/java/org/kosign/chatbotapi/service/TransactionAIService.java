package org.kosign.chatbotapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionRequest;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.kosign.chatbotapi.service.bakong.BakongTransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Optimized service for AI integration with transaction checking
 * Features enhanced performance, caching, and user-friendly messaging
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionAIService {
    
    private final BakongTransactionService bakongTransactionService;
    @Value("${ai.chat.system-prompt}")
    private String systemPrompt;
    // Optimized patterns with compiled regex for better performance
    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^(USD|KHR)$");
    
    // Enhanced transaction inquiry keywords for better detection
    private static final String[] TRANSACTION_KEYWORDS = {
        "transaction error", "payment failed", "check transaction", "transaction status",
        "payment issue", "money not received", "transfer problem", "payment stuck",
        "transaction pending", "payment not working", "failed payment", "transaction failed",
        "money missing", "payment not completed", "transfer not received", "bakong error",
        "payment gateway", "transaction hash", "payment timeout", "money lost"
    };

    // Performance metrics
    private final AtomicLong validationCount = new AtomicLong(0);
    private final AtomicLong successfulChecks = new AtomicLong(0);
    private final AtomicLong failedChecks = new AtomicLong(0);
    
    /**
     * Enhanced transaction inquiry detection with caching
     */
    @Cacheable(value = "transaction-inquiry-cache", key = "#userMessage.hashCode()")
    public boolean isTransactionInquiry(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return false;
        }
        
        String message = userMessage.toLowerCase().trim();
        
        // Fast check using parallel stream for better performance
        return java.util.Arrays.stream(TRANSACTION_KEYWORDS)
                .parallel()
                .anyMatch(message::contains);
    }
    
    /**
     * Optimized transaction input validation with detailed error reporting
     */
    public TransactionValidationResult validateTransactionInput(String hash, String amountStr, String currency) {
        long validationId = validationCount.incrementAndGet();
        log.debug("🔍 Starting validation #{}: hash={}, amount={}, currency={}", validationId, hash, amountStr, currency);
        
        TransactionValidationResult result = new TransactionValidationResult();
        Instant startTime = Instant.now();
        
        // Parallel validation for better performance
        CompletableFuture<Void> hashValidation = CompletableFuture.runAsync(() -> validateHash(hash, result));
        CompletableFuture<Void> amountValidation = CompletableFuture.runAsync(() -> validateAmount(amountStr, result));
        CompletableFuture<Void> currencyValidation = CompletableFuture.runAsync(() -> validateCurrency(currency, result));
        
        // Wait for all validations to complete
        CompletableFuture.allOf(hashValidation, amountValidation, currencyValidation).join();
        
        Duration validationTime = Duration.between(startTime, Instant.now());
        log.debug("⚡ Validation #{} completed in {} ms with {} errors", 
            validationId, validationTime.toMillis(), result.getErrors().size());
        
        return result;
    }

    private void validateHash(String hash, TransactionValidationResult result) {
        if (hash == null || hash.trim().isEmpty()) {
            result.addError("🔍 **Transaction Hash Required** - Please provide your transaction hash to check the status.");
        } else {
            String cleanHash = hash.trim();
            if (cleanHash.length() < 4) {
                result.addError("🔍 **Invalid Hash Length** - Transaction hash is too short. Please check and try again.");
            } else if (cleanHash.length() > 64) {
                result.addError("🔍 **Invalid Hash Length** - Transaction hash is too long. Please verify your hash.");
            } else if (!HASH_PATTERN.matcher(cleanHash).matches()) {
                result.addError("🔍 **Invalid Hash Format** - Transaction hash must contain only letters and numbers (no spaces or special characters).");
            }
        }
    }

    private void validateAmount(String amountStr, TransactionValidationResult result) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            result.addError("💰 **Amount Required** - Please specify the transaction amount to verify.");
        } else {
            String cleanAmount = amountStr.trim();
            if (!AMOUNT_PATTERN.matcher(cleanAmount).matches()) {
                result.addError("💰 **Invalid Amount Format** - Please enter a valid number (e.g., 50 or 50.25).");
            } else {
                try {
                    double amount = Double.parseDouble(cleanAmount);
                    if (amount <= 0) {
                        result.addError("💰 **Invalid Amount** - Amount must be greater than 0.");
                    } else if (amount > 1000000) {
                        result.addError("💰 **Amount Too Large** - Please verify the transaction amount (seems unusually high).");
                    }
                } catch (NumberFormatException e) {
                    result.addError("💰 **Invalid Amount** - Please enter a valid numeric amount.");
                }
            }
        }
    }

    private void validateCurrency(String currency, TransactionValidationResult result) {
        if (currency == null || currency.trim().isEmpty()) {
            result.addError("💱 **Currency Required** - Please specify the currency (USD or KHR).");
        } else {
            String cleanCurrency = currency.trim().toUpperCase();
            if (!CURRENCY_PATTERN.matcher(cleanCurrency).matches()) {
                result.addError("💱 **Invalid Currency** - Only USD and KHR are supported. Please check your currency.");
            }
        }
    }
    
    /**
     * Enhanced transaction status checking with comprehensive error handling
     */
    public String checkTransactionStatus(String hash, String amountStr, String currency) {
        Instant startTime = Instant.now();
        log.info("🔍 AI requesting transaction check for hash: {}", hash);
        
        try {
            // Service availability check
            if (bakongTransactionService == null) {
                log.error("💥 BakongTransactionService is not properly injected!");
                failedChecks.incrementAndGet();
                return formatServiceError("🔧 **Service Unavailable** - Transaction checking service is currently not available. Please try again later or contact support.");
            }
            
            log.debug("✅ Using transaction service: {}", bakongTransactionService.getClass().getSimpleName());
            
            // Enhanced input validation
            TransactionValidationResult validation = validateTransactionInput(hash, amountStr, currency);
            if (!validation.isValid()) {
                log.warn("❌ Validation failed for transaction input: {}", validation.getErrors());
                failedChecks.incrementAndGet();
                return formatValidationError(validation);
            }
            
            log.debug("✅ Input validation passed, creating API request...");
            
            // Parse and create optimized request
            Number amount = Double.parseDouble(amountStr.trim());
            BakongTransactionRequest request = BakongTransactionRequest.builder()
                    .hash(hash.trim())
                    .amount(amount)
                    .currency(currency.trim().toUpperCase())
                    .build();
            
            log.info("🌐 Executing transaction check with request: {}", request);
            
            // Call the service with timing
            BakongTransactionResponse response = bakongTransactionService.checkTransactionStatus(request);
            
            Duration checkDuration = Duration.between(startTime, Instant.now());
            log.info("⚡ Transaction check completed in {} ms", checkDuration.toMillis());
            
            // Track success and format response
            successfulChecks.incrementAndGet();
            return formatTransactionResponse(response, checkDuration);
            
        } catch (Exception e) {
            Duration errorDuration = Duration.between(startTime, Instant.now());
            log.error("💥 Error checking transaction status after {} ms: {}", errorDuration.toMillis(), e.getMessage(), e);
            failedChecks.incrementAndGet();
            
            return formatServiceError(generateErrorMessage(e));
        }
    }

    private String generateErrorMessage(Exception e) {
        String message = e.getMessage();
        
        if (message != null) {
            if (message.contains("Authentication")) {
                return "🔐 **Authentication Issue** - We're having trouble verifying your transaction due to authentication problems. Please try again in a few minutes.";
            } else if (message.contains("timeout") || message.contains("Timeout")) {
                return "⏱️ **Request Timeout** - The transaction check is taking longer than expected. Please try again.";
            } else if (message.contains("network") || message.contains("Network")) {
                return "🌐 **Network Issue** - We're experiencing connectivity problems. Please check your connection and try again.";
            } else if (message.contains("Not Found") || message.contains("404")) {
                return "🔍 **Transaction Not Found** - No transaction found with the provided details. Please verify your information.";
            }
        }
        
        return "🔄 **Service Issue** - We're experiencing temporary difficulties checking transactions. Please try again in a few minutes or contact support.";
    }
    
    /**
     * Enhanced transaction details prompt with better guidance
     */
    public String getTransactionDetailsPrompt() {
        return """
            🔍 **Transaction Status Check**
            
            To help you check your transaction status, I need the following information:
            
            📋 **Required Details:**
            
            1. 🏷️ **Transaction Hash**
               - Usually 8-64 characters
               - Contains only letters and numbers
               - Example: `c250339a` or `abc123def456`
            
            2. 💰 **Transaction Amount**
               - The exact amount you sent/received
               - Example: `50` or `25.75`
            
            3. 💱 **Currency**
               - Must be either `USD` or `KHR`
            
            
            💡 **Example:**
            ```
            Hash: c250339a
            Amount: 50
            Currency: USD
            ```
            
            ℹ️ Once you provide this information, I'll check your transaction status immediately.
            """;
    }

    private String formatValidationError(TransactionValidationResult validation) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ **Input Validation Required**\n\n");
        sb.append("Please check the following:\n\n");
        
        for (String error : validation.getErrors()) {
            sb.append("• ").append(error).append("\n");
        }
        
        sb.append("\n💡 **Need Help?**\n");
        sb.append("• Transaction hash: Usually found in your payment confirmation\n");
        sb.append("• Amount: Enter the exact amount (e.g., 50 or 25.75)\n");
        sb.append("• Currency: Use USD or KHR only\n\n");
        sb.append("Please provide the corrected information and I'll check your transaction immediately.");
        
        return sb.toString();
    }
    
    private String formatTransactionResponse(BakongTransactionResponse response, Duration checkDuration) {
        if (response == null) {
            return "❌ **Transaction Not Found**\n\n" +
                   "The transaction could not be found in the system. Please verify your details and try again.";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Enhanced status determination with more specific icons
        TransactionStatus status = determineTransactionStatus(response);
        
        sb.append(status.getIcon()).append(" **Transaction Status: ").append(status.getDisplayText()).append("**\n\n");
        
        // Comprehensive transaction details with better formatting
        sb.append("📋 **Transaction Details:**\n");
        // sb.append("```\n");
        // sb.append("Hash:        ").append(response.getHash()).append("\n");
        
        if (response.getFromAccountId() != null) {
            sb.append("From:        ").append(maskAccountId(response.getFromAccountId())).append("\n");
        }
        if (response.getToAccountId() != null) {
            sb.append("To:          ").append(maskAccountId(response.getToAccountId())).append("\n");
        }
        
        sb.append("Amount:      ").append(formatAmount(response.getAmount(), response.getCurrency())).append("\n");
        
        if (response.getDescription() != null && !response.getDescription().trim().isEmpty()) {
            sb.append("Description: ").append(response.getDescription()).append("\n");
        }
        
        if (response.getTrackingStatus() != null) {
            sb.append("Status:      ").append(response.getTrackingStatus()).append("\n");
        }
        
        if (response.getCreatedDateMs() != null) {
            sb.append("Created:     ").append(formatTimestamp(response.getCreatedDateMs())).append("\n");
        }
        
        if (response.getAcknowledgedDateMs() != null) {
            sb.append("Confirmed:   ").append(formatTimestamp(response.getAcknowledgedDateMs())).append("\n");
        }
        
        if (response.getReceiverBank() != null) {
            sb.append("Bank:        ").append(response.getReceiverBank()).append("\n");
        }
        
        // sb.append("```\n\n");
        
        // Status-specific guidance with enhanced messaging
        sb.append(status.getGuidanceMessage());
        
        // Performance information
        sb.append(String.format("\n\n⚡ *Checked in %d ms*", checkDuration.toMillis()));
        
        return sb.toString();
    }

    private TransactionStatus determineTransactionStatus(BakongTransactionResponse response) {
        // Check if we have confirmation data (acknowledgedDateMs indicates successful completion)
        if (response.getAcknowledgedDateMs() != null && response.getAcknowledgedDateMs() > 0) {
            log.info("✅ Transaction determined as SUCCESSFUL based on acknowledgedDateMs: {}", response.getAcknowledgedDateMs());
            return TransactionStatus.SUCCESSFUL;
        }
        
        // Check tracking status if available
        if (response.getTrackingStatus() == null) {
            log.warn("⚠️ No tracking status available, checking other indicators");
            // If we have creation date but no acknowledged date, might be pending
            if (response.getCreatedDateMs() != null && response.getCreatedDateMs() > 0) {
                return TransactionStatus.PENDING;
            }
            return TransactionStatus.UNKNOWN;
        }
        
        String status = response.getTrackingStatus().toUpperCase().trim();
        log.info("🔍 Analyzing tracking status: '{}'", status);
        
        // Check for successful statuses
        if (status.contains("SUCCESS") || status.contains("COMPLETED") || status.contains("CONFIRMED") 
            || status.contains("ACKNOWLEDGED") || status.equals("200") || status.equals("OK")
            || status.contains("SETTLED") || status.contains("CLEARED") || status.contains("DONE")) {
            return TransactionStatus.SUCCESSFUL;
        } 
        
        // Check for failed statuses
        if (status.contains("FAILED") || status.contains("ERROR") || status.contains("REJECTED") 
            || status.contains("MISMATCH") || status.contains("DECLINED") || status.contains("INVALID")
            || status.startsWith("4") || status.startsWith("5")) { // HTTP error codes
            return TransactionStatus.FAILED;
        } 
        
        // Check for pending statuses
        if (status.contains("PENDING") || status.contains("PROCESSING") || status.contains("WAITING")
            || status.contains("IN_PROGRESS") || status.contains("SUBMITTED") || status.contains("QUEUED")) {
            return TransactionStatus.PENDING;
        }
        
        // For numeric status codes, treat 200-299 as success
        try {
            int statusCode = Integer.parseInt(status);
            if (statusCode >= 200 && statusCode < 300) {
                log.info("✅ Treating HTTP status {} as SUCCESSFUL", statusCode);
                return TransactionStatus.SUCCESSFUL;
            } else if (statusCode >= 400) {
                return TransactionStatus.FAILED;
            }
        } catch (NumberFormatException e) {
            // Not a numeric status, continue with other checks
        }
        
        log.warn("❓ Unknown tracking status '{}', defaulting to UNKNOWN", status);
        return TransactionStatus.UNKNOWN;
    }

    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return accountId;
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }

    private String formatAmount(Number amount, String currency) {
        if (amount == null) return "N/A";
        if (currency == null) currency = "USD";
        
        return String.format("%,.2f %s", amount.doubleValue(), currency);
    }
    
    private String formatServiceError(String message) {
        return "🔄 **Service Issue**\n\n" + message + 
               "\n\nIf the problem persists, please contact PPC Bank support for assistance.";
    }
    
    private String formatTimestamp(Float timestampMs) {
        if (timestampMs == null) return "N/A";
        
        try {
            long timestamp = timestampMs.longValue();
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), 
                ZoneId.systemDefault()
            );
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"));
        } catch (Exception e) {
            log.warn("Failed to format timestamp: {}", timestampMs);
            return "Invalid date";
        }
    }

    // Performance monitoring
    public String getServiceMetrics() {
        long total = successfulChecks.get() + failedChecks.get();
        double successRate = total > 0 ? (double) successfulChecks.get() / total * 100 : 0;
        
        return String.format(
            "Transaction AI Metrics: validations=%d, checks=%d, successes=%d, success_rate=%.1f%%",
            validationCount.get(), total, successfulChecks.get(), successRate
        );
    }

    // Enhanced validation result class
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
        
        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
        
        public int getErrorCount() {
            return errors.size();
        }
    }

    // Enhanced transaction status enum
    private enum TransactionStatus {
        SUCCESSFUL("✅", "SUCCESSFUL", 
            "🎉 **Great News!** Your transaction has been completed successfully.\n\n" +
            "💡 **What this means:**\n" +
            "• The money has been transferred successfully\n" +
            "• The transaction is finalized and cannot be reversed\n" +
            "• You can expect the funds to be available in the recipient's account"),
            
        FAILED("❌", "FAILED", 
            "⚠️ **Transaction Failed** - Your transaction could not be completed.\n\n" +
            "🔧 **Next Steps:**\n" +
            "• Verify your account balance and transaction details\n" +
            "• Check if there are any account restrictions\n" +
            "• Contact PPC Bank support if you need assistance\n" +
            "• You may need to initiate a new transaction"),
            
        PENDING("⏳", "PENDING", 
            "⏳ **Transaction In Progress** - Your transaction is being processed.\n\n" +
            "⏱️ **What to expect:**\n" +
            "• Processing typically takes a few minutes to several hours\n" +
            "• You'll receive a notification once it's complete\n" +
            "• No action is required from your side\n" +
            "• Contact support if it remains pending for more than 24 hours"),
            
        UNKNOWN("❓", "UNKNOWN", 
            "❓ **Status Unclear** - We couldn't determine the exact transaction status.\n\n" +
            "🔍 **Recommended Actions:**\n" +
            "• Wait a few minutes and check again\n" +
            "• Verify your transaction details are correct\n" +
            "• Contact PPC Bank support for manual verification\n" +
            "• Keep your transaction hash for reference");

        private final String icon;
        private final String displayText;
        private final String guidanceMessage;

        TransactionStatus(String icon, String displayText, String guidanceMessage) {
            this.icon = icon;
            this.displayText = displayText;
            this.guidanceMessage = guidanceMessage;
        }

        public String getIcon() { return icon; }
        public String getDisplayText() { return displayText; }
        public String getGuidanceMessage() { return guidanceMessage; }
    }
} 