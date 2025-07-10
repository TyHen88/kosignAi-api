package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.domains.Workflow;
import org.kosign.chatbotapi.model.TitleMatchResult;
import org.kosign.chatbotapi.repository.PPCBankContentRepository;
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for intelligent title matching using fuzzy matching and semantic analysis
 */
@Service
public class TitleMatchingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TitleMatchingService.class);
    
    @Autowired
    private PPCBankContentRepository pageContentRepository;

    private WorkflowRepository workflowRepository;
    
    @Autowired
    private BankingDomainService bankingDomainService;
    
    @Autowired
    private SearchConfigService searchConfigService;
    
    // Predefined title patterns for better matching
    private static final Map<String, List<String>> TITLE_PATTERNS = new HashMap<>();
    
    static {
        // Banking product patterns
        TITLE_PATTERNS.put("CARD", Arrays.asList("card", "visa", "credit", "debit", "prestige", "business"));
        TITLE_PATTERNS.put("LOAN", Arrays.asList("loan", "credit", "financing", "annatean", "rohas", "home", "car", "songkhoem"));
        TITLE_PATTERNS.put("ACCOUNT", Arrays.asList("account", "savings", "current", "deposit", "fixed", "vip", "junior"));
        TITLE_PATTERNS.put("BANKING", Arrays.asList("banking", "mobile", "online", "digital", "e-banking", "smartbiz"));
        TITLE_PATTERNS.put("PAYMENT", Arrays.asList("payment", "transfer", "bill", "khqr", "gateway", "sms"));
        TITLE_PATTERNS.put("CORPORATE", Arrays.asList("corporate", "business", "employment", "officer", "manager", "job"));
        TITLE_PATTERNS.put("NEWS", Arrays.asList("news", "event", "announcement", "campaign", "promotion", "discount"));
        TITLE_PATTERNS.put("SUPPORT", Arrays.asList("support", "contact", "branch", "help", "complaint", "information"));
    }
    
    /**
     * Performs intelligent title matching with configuration-based search mode
     */
    public List<TitleMatchResult> matchTitles(String userQuery, int maxResults) {
        logger.debug("Matching titles for query: {}", userQuery);
        
        // Check search configuration
        boolean isDbOnly = searchConfigService.isDbOnlyMode();
        logger.debug("🔍 Search mode: {}", isDbOnly ? "Database Only" : "Hybrid Search");
        
        if (isDbOnly) {
            // Database-only search
            return performDatabaseSearch(userQuery, maxResults);
        } else {
            // Hybrid search (database + external)
            return performHybridSearch(userQuery, maxResults);
        }
    }

    public List<TitleMatchResult> workflowMatchContents(String userQuery, int maxResults) {
        logger.debug("Matching workflows for query: {}", userQuery);
        List<Workflow> allWorkflows = workflowRepository.findAll();
        return allWorkflows.stream().map(workflow -> TitleMatchResult.builder().workflow(workflow).build()).collect(Collectors.toList());
    }
    
    /**
     * Calculates comprehensive match score for a title
     */
    private double calculateTitleMatchScore(PPCBank page, String normalizedQuery, List<String> queryKeywords) {
        String title = page.getTitle();
        String normalizedTitle = normalizeText(title);
        
        double score = 0.0;
        
        // 1. Exact phrase match (highest weight)
        if (normalizedTitle.contains(normalizedQuery)) {
            score += 100.0;
        }
        
        // 2. Fuzzy string similarity
        double fuzzySimilarity = calculateFuzzySimilarity(normalizedTitle, normalizedQuery);
        score += fuzzySimilarity * 80.0;
        
        // 3. Keyword matching with weights
        List<String> titleWords = Arrays.asList(normalizedTitle.split("\\s+"));
        for (String keyword : queryKeywords) {
            if (titleWords.contains(keyword)) {
                score += 30.0; // Direct keyword match
            } else {
                // Partial keyword match
                for (String titleWord : titleWords) {
                    if (titleWord.contains(keyword) || keyword.contains(titleWord)) {
                        score += 15.0;
                        break;
                    }
                }
            }
        }
        
        // 4. Semantic pattern matching
        score += calculateSemanticScore(title, normalizedQuery);
        
        // 5. Domain relevance boost
        String detectedDomain = bankingDomainService.detectBankingDomain(normalizedQuery);
        if (detectedDomain != null) {
            score += calculateDomainRelevanceScore(title, detectedDomain);
        }
        
        // 6. Title quality and recency boost
        if (page.getStatus() != null && page.getStatus() == 200) {
            score += 5.0;
        }
        
        if (page.getUpdatedAt() != null) {
            long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(
                page.getUpdatedAt().toLocalDate(), 
                java.time.LocalDate.now()
            );
            if (daysSinceUpdate < 30) {
                score += 3.0;
            }
        }
        
        return score;
    }
    
    /**
     * Calculates fuzzy similarity between two strings using Levenshtein distance
     */
    private double calculateFuzzySimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int editDistance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - (double) editDistance / maxLen;
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calculates semantic score based on predefined patterns
     */
    private double calculateSemanticScore(String title, String query) {
        double score = 0.0;
        String lowerTitle = title.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : TITLE_PATTERNS.entrySet()) {
            List<String> patterns = entry.getValue();
            
            boolean titleMatches = patterns.stream().anyMatch(lowerTitle::contains);
            boolean queryMatches = patterns.stream().anyMatch(lowerQuery::contains);
            
            if (titleMatches && queryMatches) {
                score += 20.0;
            }
        }
        
        return score;
    }
    
    /**
     * Calculates domain relevance score
     */
    private double calculateDomainRelevanceScore(String title, String domain) {
        List<String> domainKeywords = bankingDomainService.getDomainKeywords(domain);
        if (domainKeywords == null || domainKeywords.isEmpty()) {
            return 0.0;
        }
        
        String lowerTitle = title.toLowerCase();
        double score = 0.0;
        
        for (String keyword : domainKeywords) {
            if (lowerTitle.contains(keyword.toLowerCase())) {
                score += 10.0;
            }
        }
        
        return score;
    }
    
    /**
     * Extracts and normalizes keywords from user query
     */
    private List<String> extractQueryKeywords(String query) {
        // Remove common stop words and normalize
        String normalized = normalizeText(query);
        List<String> words = Arrays.asList(normalized.split("\\s+"));
        
        // Filter out stop words and short words
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "can", "what", "how", "where", "when", "why", "who");
        
        return words.stream()
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word))
            .collect(Collectors.toList());
    }
    
    /**
     * Normalizes text for better matching
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        
        return text.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Finds which keywords matched in the title
     */
    private List<String> findMatchedKeywords(String title, List<String> queryKeywords) {
        String normalizedTitle = normalizeText(title);
        List<String> titleWords = Arrays.asList(normalizedTitle.split("\\s+"));
        
        return queryKeywords.stream()
            .filter(keyword -> titleWords.contains(keyword) || 
                titleWords.stream().anyMatch(word -> word.contains(keyword) || keyword.contains(word)))
            .collect(Collectors.toList());
    }
    
    /**
     * Determines the type of match found
     */
    private String determineMatchType(String title, String query, List<String> queryKeywords) {
        String normalizedTitle = normalizeText(title);
        
        if (normalizedTitle.contains(query)) {
            return "EXACT_PHRASE";
        }
        
        List<String> titleWords = Arrays.asList(normalizedTitle.split("\\s+"));
        long exactMatches = queryKeywords.stream()
            .filter(titleWords::contains)
            .count();
        
        if (exactMatches >= queryKeywords.size() * 0.8) {
            return "HIGH_KEYWORD";
        } else if (exactMatches >= queryKeywords.size() * 0.5) {
            return "MEDIUM_KEYWORD";
        } else {
            return "FUZZY_MATCH";
        }
    }

    /**
     * Performs database-only search
     */
    private List<TitleMatchResult> performDatabaseSearch(String userQuery, int maxResults) {
        logger.info("🗄️ Performing database-only search for: {}", userQuery);
        
        // Get all available titles from database
        List<PPCBank> allPages = pageContentRepository.findByMultipleKeywordsCombined(userQuery);
        
        // Extract and normalize user query keywords
        List<String> queryKeywords = extractQueryKeywords(userQuery);
        String normalizedQuery = normalizeText(userQuery);
        
        // Calculate match scores for each title
        List<TitleMatchResult> matches = new ArrayList<>();
        
        for (PPCBank page : allPages) {
            if (page.getTitle() == null || page.getTitle().trim().isEmpty()) {
                continue;
            }
            
            double matchScore = calculateTitleMatchScore(page, normalizedQuery, queryKeywords);
            
            if (matchScore > 0.1) { // Minimum threshold for relevance
                matches.add(TitleMatchResult.builder()
                    .page(page)
                    .matchScore(matchScore)
                    .matchedKeywords(findMatchedKeywords(page.getTitle(), queryKeywords))
                    .matchType(determineMatchType(page.getTitle(), normalizedQuery, queryKeywords))
                    .build());
            }
        }
        
        // Sort by match score and return top results
        return matches.stream()
            .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * Performs hybrid search (database + external sources)
     */
    private List<TitleMatchResult> performHybridSearch(String userQuery, int maxResults) {
        logger.info("🌐 Performing hybrid search (internal + external) for: {}", userQuery);
        
        // 1. First get database results
        List<TitleMatchResult> databaseResults = performDatabaseSearch(userQuery, maxResults);
        logger.info("📊 Database search returned {} results", databaseResults.size());
        
        // 2. Prepare for external search integration
        List<TitleMatchResult> hybridResults = new ArrayList<>(databaseResults);
        
        // TODO: Implement external search logic here
        // Example future implementation:
        // if (databaseResults.size() < maxResults) {
        //     List<TitleMatchResult> externalResults = performExternalSearch(userQuery, maxResults - databaseResults.size());
        //     hybridResults.addAll(externalResults);
        //     logger.info("🌐 External search returned {} additional results", externalResults.size());
        // }
        
        logger.warn("⚠️ External search not yet implemented - currently returning {} database results only", hybridResults.size());
        
        // Future implementation could include:
        // - Web search APIs (Google, Bing)
        // - External banking knowledge bases
        // - Third-party financial data sources
        // - Banking regulatory websites
        // - FAQ databases from other banks
        
        return hybridResults;
    }
    
    /**
     * Placeholder for future external search implementation
     * This method will be implemented when external search sources are available
     */
    private List<TitleMatchResult> performExternalSearch(String userQuery, int maxResults) {
        logger.debug("🔮 External search called for: {} (max: {})", userQuery, maxResults);
        
        // TODO: Implement external search logic
        // This could include:
        // 1. Web search API calls
        // 2. External banking database queries  
        // 3. Third-party knowledge base searches
        // 4. Banking regulation lookup
        
        return new ArrayList<>(); // Return empty list for now
    }

} 