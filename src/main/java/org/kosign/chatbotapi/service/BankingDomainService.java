package org.kosign.chatbotapi.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for banking domain detection and keyword management
 */
@Service
public class BankingDomainService {
    
    // Enhanced banking-related keywords and synonyms with domain intelligence
    private final Map<String, List<String>> bankingKeywords = createBankingKeywordsMap();
    
    private Map<String, List<String>> createBankingKeywordsMap() {
        Map<String, List<String>> keywords = new HashMap<>();
        keywords.put("loans", Arrays.asList("loan", "credit", "lending", "borrow", "mortgage", "financing", "car loan", "personal loan", "business loan", "home loan", "auto loan", "vehicle loan", "installment", "emi", "interest rate", "collateral", "guarantor"));
        keywords.put("accounts", Arrays.asList("account", "savings", "checking", "deposit", "current account", "fixed deposit", "time deposit", "fd", "saving account", "current", "account opening", "minimum balance"));
        keywords.put("cards", Arrays.asList("card", "credit card", "debit card", "atm card", "visa", "mastercard", "card application", "card limit", "card fees", "annual fee", "cashback", "rewards"));
        keywords.put("payments", Arrays.asList("payment", "pay", "bill", "bills", "transfer", "remittance", "send money", "bill payment", "utility payment", "online payment", "mobile payment", "wire transfer", "fund transfer"));
        keywords.put("forex", Arrays.asList("foreign exchange", "currency", "exchange rate", "usd", "dollar", "euro", "yen", "foreign currency", "currency exchange", "fx rate", "international transfer"));
        keywords.put("mobile_banking", Arrays.asList("mobile banking", "app", "online banking", "digital", "internet banking", "mobile app", "online", "digital banking", "e-banking", "net banking"));
        keywords.put("services", Arrays.asList("service", "banking service", "financial service", "product", "facility", "feature", "benefit", "offering"));
        keywords.put("rates_fees", Arrays.asList("rate", "interest", "fees", "charges", "pricing", "cost", "commission", "penalty", "tariff", "schedule"));
        keywords.put("branches_atm", Arrays.asList("branch", "location", "atm", "office", "address", "nearest branch", "atm location", "contact", "phone", "hours"));
        keywords.put("business", Arrays.asList("corporate", "commercial", "enterprise", "company", "sme", "business account", "corporate account", "trade finance", "business loan"));
        keywords.put("investment", Arrays.asList("investment", "mutual fund", "insurance", "life insurance", "general insurance", "term deposit", "wealth management", "portfolio"));
        keywords.put("security", Arrays.asList("security", "safe", "vault", "locker", "safety deposit box", "secure", "protection", "fraud", "scam"));
        return keywords;
    }
    
    /**
     * Detects the primary banking domain from the user query
     * This helps prioritize search within specific banking categories
     */
    public String detectBankingDomain(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Priority order for domain detection
        String[] domainOrder = {"loans", "accounts", "cards", "payments", "forex", "mobile_banking", 
                               "investment", "business", "security", "branches_atm", "rates_fees", "services"};
        
        for (String domain : domainOrder) {
            List<String> keywords = bankingKeywords.get(domain);
            if (keywords != null) {
                for (String keyword : keywords) {
                    if (lowerQuery.contains(keyword.toLowerCase())) {
                        return domain;
                    }
                }
            }
        }
        
        return null; // No specific domain detected
    }
    
    /**
     * Extracts smart keywords with domain intelligence
     */
    public List<String> extractSmartKeywords(String query) {
        Set<String> keywords = new LinkedHashSet<>(); // Use LinkedHashSet to preserve order and remove duplicates
        
        // First, detect the primary banking domain
        String primaryDomain = detectBankingDomain(query);

        // Split query into words
        String[] words = query.split("\\s+");

        // Add original words (filtering out very common words)
        Set<String> stopWords = Set.of("the", "is", "are", "was", "were", "a", "an", "and", "or", "but",
                "in", "on", "at", "to", "for", "of", "with", "by", "from", "about", "into", "through",
                "during", "before", "after", "above", "below", "up", "down", "out", "off", "over",
                "under", "again", "further", "then", "once", "what", "how", "when", "where", "why",
                "tell", "me", "you", "i", "can", "could", "would", "should", "will", "do", "does", "did");

        for (String word : words) {
            if (word.length() > 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        // Add domain-specific keywords with priority
        if (primaryDomain != null) {
            List<String> domainKeywords = bankingKeywords.get(primaryDomain);
            if (domainKeywords != null) {
                // Add the primary domain keywords first (higher priority)
                keywords.addAll(domainKeywords.stream().limit(3).collect(Collectors.toList()));
            }
        }

        // Add banking-related synonyms (but limit expansion to avoid over-matching)
        for (Map.Entry<String, List<String>> entry : bankingKeywords.entrySet()) {
            // Skip the primary domain as we already added it
            if (entry.getKey().equals(primaryDomain)) continue;
            
            for (String synonym : entry.getValue()) {
                if (query.contains(synonym)) {
                    keywords.add(entry.getKey()); // Add main category
                    // Add only 1 most relevant synonym instead of all
                    keywords.addAll(entry.getValue().stream().limit(1).collect(Collectors.toList()));
                    break; // Stop after first match to avoid over-expansion
                }
            }
        }

        // Add phrase-based keywords (limit to essential terms)
        if (query.contains("bill payment") || query.contains("pay bill")) {
            keywords.addAll(Arrays.asList("payment", "bill", "transfer"));
        }
        if (query.contains("exchange rate") || query.contains("currency")) {
            keywords.addAll(Arrays.asList("forex", "exchange", "rate"));
        }
        if (query.contains("mobile banking") || query.contains("app")) {
            keywords.addAll(Arrays.asList("mobile", "app", "banking"));
        }

        // Return limited set of most relevant keywords (max 6 to reduce DB calls)
        return keywords.stream().limit(6).collect(Collectors.toList());
    }
    
    /**
     * Builds domain-specific search terms with relevance weighting
     */
    public List<String> buildDomainSearchTerms(List<String> keywords, String primaryDomain) {
        List<String> searchTerms = new ArrayList<>(keywords);
        
        // Add domain-specific terms if primary domain is detected
        if (primaryDomain != null) {
            List<String> domainTerms = bankingKeywords.get(primaryDomain);
            if (domainTerms != null) {
                // Add top 2 domain-specific terms to boost relevance
                searchTerms.addAll(0, domainTerms.stream().limit(2).collect(Collectors.toList()));
            }
        }
        
        // Remove duplicates while preserving order
        return searchTerms.stream().distinct().limit(5).collect(Collectors.toList());
    }
    
    /**
     * Formats domain name for display
     */
    public String formatDomainName(String domain) {
        switch (domain) {
            case "loans": return "Loan Products & Services";
            case "accounts": return "Account Services";
            case "cards": return "Card Services";
            case "payments": return "Payment Services";
            case "forex": return "Foreign Exchange";
            case "mobile_banking": return "Mobile & Digital Banking";
            case "investment": return "Investment & Insurance";
            case "business": return "Business Banking";
            case "security": return "Security Services";
            case "branches_atm": return "Branch & ATM Services";
            case "rates_fees": return "Rates & Fees";
            default: return "Banking Services";
        }
    }
    
    /**
     * Provides domain-specific guidance for better AI responses
     */
    public String getDomainSpecificGuidance(String domain) {
        StringBuilder guidance = new StringBuilder();
        guidance.append("💡 **Important Notes:**\n");
        
        switch (domain) {
            case "loans":
                guidance.append("- Loan eligibility may vary based on income, credit history, and collateral\n");
                guidance.append("- Interest rates are subject to change and may differ based on loan type\n");
                guidance.append("- Processing fees and documentation requirements apply\n");
                break;
            case "accounts":
                guidance.append("- Account opening requires valid identification and minimum deposit\n");
                guidance.append("- Monthly maintenance fees may apply based on account type\n");
                guidance.append("- Interest rates and features vary by account category\n");
                break;
            case "cards":
                guidance.append("- Card approval depends on income verification and credit assessment\n");
                guidance.append("- Annual fees, interest rates, and credit limits vary by card type\n");
                guidance.append("- Rewards and benefits are subject to terms and conditions\n");
                break;
            case "payments":
                guidance.append("- Transaction limits and fees may apply based on payment type\n");
                guidance.append("- International transfers require additional documentation\n");
                guidance.append("- Processing times vary by destination and payment method\n");
                break;
            case "forex":
                guidance.append("- Exchange rates fluctuate throughout the day\n");
                guidance.append("- Spread margins apply for buying and selling currencies\n");
                guidance.append("- Large transactions may require advance notice\n");
                break;
            default:
                guidance.append("- Terms and conditions apply to all banking services\n");
                guidance.append("- Fees and charges are subject to change\n");
                guidance.append("- Contact PPC Bank for the most current information\n");
        }
        
        guidance.append("\n");
        return guidance.toString();
    }
    
    /**
     * Gets banking keywords for a specific domain
     */
    public List<String> getDomainKeywords(String domain) {
        return bankingKeywords.getOrDefault(domain, new ArrayList<>());
    }
} 