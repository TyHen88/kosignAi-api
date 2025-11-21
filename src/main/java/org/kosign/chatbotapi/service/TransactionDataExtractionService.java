package org.kosign.chatbotapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting transaction data (hash, amount, currency) from user
 * queries
 */
@Service
public class TransactionDataExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionDataExtractionService.class);

    // Pre-compiled regex patterns for better performance
    private static final Pattern HASH_PATTERN = Pattern.compile(
            "(?:\\*\\*)?(?:external\\s+transaction\\s+reference)(?:\\*\\*)?[:\\s]+([a-zA-Z0-9]{6,12})|" +
                    "(?:bakong\\s+(?:hash|id)\\s*[#:]?)\\s*([a-zA-Z0-9]{6,12})|" +
                    "(?:transaction\\s+hash[/]?id|transaction\\s+(?:hash|id))[:\\s#]+([a-zA-Z0-9]{6,12})|" +
                    "(?:external\\s+reference|reference\\s+id|ref(?:erence)?|hash|id)[:\\s#]+([a-zA-Z0-9]{6,12})|" +
                    "(?:my\\s+)?(?:transaction\\s+)?(?:hash|id)[:\\s#]+([a-zA-Z0-9]{6,12})|" +
                    "\\b(?=[a-zA-Z0-9]{8}\\b)(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]{8}\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:\\d+\\.\\s*)?\\*\\*\\s*(?:amount|original\\s+amount)\\s*\\*\\*[:\\s]*(-?[0-9]+(?:\\.[0-9]+)?)|" +
                    "(?:amount|original\\s+amount|sum|paid|send|sent|transfer)[:\\s]*(-?[0-9]+(?:\\.[0-9]+)?)|" +
                    "\\$\\s*(-?[0-9]+(?:\\.[0-9]+)?)|" +
                    "(-?[0-9]+(?:\\.[0-9]+)?)\\s*(?:USD|KHR|dollars?|riels?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
            "\\b(USD|KHR)\\b|\\b(dollars?)\\b|\\b(riels?)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MARKDOWN_PATTERN = Pattern.compile(
            "\\*\\*[^*]+\\*\\*[:\\s]+([a-zA-Z0-9]{6,12})\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern FALLBACK_HASH_PATTERN = Pattern.compile(
            "\\b([a-zA-Z0-9]{8})\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> COMMON_WORDS = Set.of(
            "error", "amount", "currency", "transaction", "payment", "transfer",
            "from", "account", "bank", "date", "time", "seller", "original",
            "reference", "extracted", "details", "here", "are", "the");

    /**
     * Extract transaction data from user query
     */
    public TransactionData extractTransactionData(String userQuery) {
        TransactionData data = new TransactionData();
        logger.debug("Input text for extraction: {}", userQuery);

        extractHash(userQuery, data);
        extractAmount(userQuery, data);
        extractCurrency(userQuery, data);

        // Additional pattern to extract structured data if missing
        if (data.hash == null || data.amount == null || data.currency == null) {
            extractStructuredData(userQuery, data);
        }

        logger.info("Final extraction - Hash: {}, Amount: {}, Currency: {}",
                data.hash, data.amount, data.currency);
        return data;
    }

    private void extractHash(String userQuery, TransactionData data) {
        Matcher hashMatcher = HASH_PATTERN.matcher(userQuery);
        String bestHash = null;
        int bestPriority = 999;

        while (hashMatcher.find()) {
            for (int i = 1; i <= hashMatcher.groupCount(); i++) {
                String group = hashMatcher.group(i);
                if (group != null && !group.trim().isEmpty() && !isCommonWord(group.trim())) {
                    String candidate = group.trim();
                    int priority = getPriorityForHashGroup(i, candidate);

                    if (candidate.length() == 8) {
                        priority -= 10; // Higher priority for 8-char hashes
                    }

                    if (priority < bestPriority) {
                        bestHash = candidate;
                        bestPriority = priority;
                    }
                }
            }
        }

        if (bestHash != null) {
            data.hash = bestHash;
        } else {
            // Try markdown pattern
            Matcher markdownMatcher = MARKDOWN_PATTERN.matcher(userQuery);
            if (markdownMatcher.find()) {
                String candidate = markdownMatcher.group(1);
                if (!isCommonWord(candidate)) {
                    data.hash = candidate;
                }
            }

            // Try fallback pattern
            if (data.hash == null) {
                Matcher fallbackMatcher = FALLBACK_HASH_PATTERN.matcher(userQuery);
                while (fallbackMatcher.find()) {
                    String candidate = fallbackMatcher.group(1);
                    if (!isCommonWord(candidate) && candidate.matches(".*[a-zA-Z].*")
                            && candidate.matches(".*[0-9].*")) {
                        data.hash = candidate;
                        break;
                    }
                }
            }
        }
    }

    private void extractAmount(String userQuery, TransactionData data) {
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(userQuery);
        if (amountMatcher.find()) {
            for (int i = 1; i <= amountMatcher.groupCount(); i++) {
                String amount = amountMatcher.group(i);
                if (amount != null && !amount.trim().isEmpty()) {
                    data.amount = amount.trim().replaceFirst("^-", "");
                    break;
                }
            }
        }
    }

    private void extractCurrency(String userQuery, TransactionData data) {
        Matcher currencyMatcher = CURRENCY_PATTERN.matcher(userQuery);
        if (currencyMatcher.find()) {
            String currency = currencyMatcher.group().toLowerCase();
            if (currency.equals("usd") || currency.contains("dollar")) {
                data.currency = "USD";
            } else if (currency.equals("khr") || currency.contains("riel")) {
                data.currency = "KHR";
            } else {
                data.currency = currencyMatcher.group().toUpperCase();
            }
        }
    }

    private void extractStructuredData(String userQuery, TransactionData data) {
        String[] parts = userQuery.split("[,\\n]");

        for (String part : parts) {
            part = part.trim();

            if (data.hash == null && (part.toLowerCase().contains("hash")
                    || part.toLowerCase().contains("id")
                    || part.toLowerCase().contains("reference"))) {
                Matcher matcher = HASH_PATTERN.matcher(part);
                String bestHash = null;
                while (matcher.find()) {
                    String candidate = matcher.group(1);
                    if (candidate != null && !isCommonWord(candidate.trim())) {
                        if (candidate.length() == 8) {
                            bestHash = candidate;
                            break;
                        } else if (bestHash == null) {
                            bestHash = candidate;
                        }
                    }
                }
                if (bestHash != null) {
                    data.hash = bestHash;
                }
            }

            if (data.amount == null && part.toLowerCase().contains("amount")) {
                Matcher matcher = AMOUNT_PATTERN.matcher(part);
                if (matcher.find()) {
                    String amount = matcher.group(1).trim();
                    if (amount.startsWith("-")) {
                        amount = amount.substring(1);
                    }
                    data.amount = amount;
                }
            }

            if (data.currency == null && part.toLowerCase().contains("currency")) {
                Matcher matcher = CURRENCY_PATTERN.matcher(part);
                if (matcher.find()) {
                    String currency = matcher.group(1).toLowerCase();
                    if (currency.equals("usd") || currency.contains("dollar")) {
                        data.currency = "USD";
                    } else if (currency.equals("khr") || currency.contains("riel")) {
                        data.currency = "KHR";
                    } else {
                        data.currency = matcher.group(1).toUpperCase();
                    }
                }
            }
        }
    }

    private boolean isCommonWord(String word) {
        return COMMON_WORDS.contains(word.toLowerCase());
    }

    private int getPriorityForHashGroup(int groupNumber, String candidate) {
        return switch (groupNumber) {
            case 1 -> 1; // External Transaction Reference
            case 2 -> 2; // Bakong hash
            case 3 -> 3; // Transaction Hash/ID variations
            case 4 -> 4; // Common labels
            case 5 -> 5; // "my transaction hash" patterns
            case 6 -> 6; // 8-character standalone
            default -> 7;
        };
    }

    /**
     * Helper class to hold transaction data
     */
    public static class TransactionData {
        public String hash;
        public String amount;
        public String currency;

        public boolean isComplete() {
            return hash != null && amount != null && currency != null;
        }

        public boolean hasPartialData() {
            return hash != null || amount != null || currency != null;
        }
    }
}
