// package org.kosign.chatbotapi.service;

// import org.kosign.chatbotapi.domains.PPCBank;
// import org.kosign.chatbotapi.model.ConversationContext;
// import org.kosign.chatbotapi.repository.PPCBankContentRepository;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.stereotype.Service;

// import java.util.*;
// import java.util.stream.Collectors;

// /**
//  * Service for handling search operations and relevance scoring
//  */
// @Service
// public class SearchService {
    
//     private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
//     @Autowired
//     private PPCBankContentRepository pageContentRepository;
    
//     @Autowired
//     private BankingDomainService bankingDomainService;
    
//     // Simple cache for search results within the same request
//     private final Map<String, List<PPCBank>> searchCache = new HashMap<>();
    
//     /**
//      * Clears the search cache for a new request
//      */
//     public void clearCache() {
//         searchCache.clear();
//     }
    
//     /**
//      * Performs intelligent search with domain awareness
//      */
//     public List<PPCBank> performIntelligentSearch(List<String> keywords, String originalQuery) {
//         Set<PPCBank> allResults = new LinkedHashSet<>();
//         String primaryDomain = bankingDomainService.detectBankingDomain(originalQuery);
        
//         logger.debug("Performing intelligent search with domain: {}, keywords: {}", primaryDomain, keywords);

//         // 1. Exact phrase search first (with caching)
//         String exactCacheKey = "exact:" + originalQuery;
//         List<PPCBank> exactResults = searchCache.computeIfAbsent(exactCacheKey, 
//             k -> pageContentRepository.findByMultipleKeywordsCombined(originalQuery));
//         allResults.addAll(exactResults);
//         logger.debug("Exact search found {} results", exactResults.size());

//         // 2. Domain-specific enhanced search
//         if (!keywords.isEmpty()) {
//             // Create domain-focused search terms
//             List<String> searchTerms = bankingDomainService.buildDomainSearchTerms(keywords, primaryDomain);
            
//             // Combine keywords with OR logic for PostgreSQL full-text search
//             String combinedKeywords = String.join(" | ", searchTerms);
            
//             // Single database call with domain context (with caching)
//             String combinedCacheKey = "domain:" + primaryDomain + ":" + combinedKeywords;
//             List<PPCBank> keywordResults = searchCache.computeIfAbsent(combinedCacheKey,
//                 k -> pageContentRepository.findByMultipleKeywordsCombined(combinedKeywords));
//             allResults.addAll(keywordResults);
//             logger.debug("Domain-specific search found {} results", keywordResults.size());
//         }

//         // 3. Fallback search if insufficient results
//         if (allResults.size() < 3 && keywords.size() >= 2) {
//             String keyword1 = keywords.size() > 0 ? keywords.get(0) : null;
//             String keyword2 = keywords.size() > 1 ? keywords.get(1) : null;
//             String keyword3 = keywords.size() > 2 ? keywords.get(2) : null;
            
//             String multiCacheKey = "multi:" + keyword1 + ":" + keyword2 + ":" + keyword3;
//             List<PPCBank> multiResults = searchCache.computeIfAbsent(multiCacheKey,
//                 k -> pageContentRepository.findByMultipleKeywords(keyword1, keyword2, keyword3));
//             allResults.addAll(multiResults);
//             logger.debug("Fallback search found {} additional results", multiResults.size());
//         }

//         // Return top results with domain relevance priority
//         return prioritizeResultsByDomain(allResults, primaryDomain, originalQuery);
//     }
    
//     /**
//      * Performs contextual search leveraging previous conversation context
//      */
//     public List<PPCBank> performContextualSearch(List<String> keywords, String query, ConversationContext context) {
//         // Combine current keywords with previous context
//         Set<String> enhancedKeywords = new LinkedHashSet<>(keywords);
//         enhancedKeywords.addAll(context.getLastKeywords().stream().limit(2).collect(Collectors.toList()));
        
//         // Perform focused search with enhanced keywords
//         String combinedKeywords = String.join(" | ", enhancedKeywords.stream().limit(4).collect(Collectors.toList()));
//         String cacheKey = "contextual:" + context.getLastDomain() + ":" + combinedKeywords;
        
//         return searchCache.computeIfAbsent(cacheKey,
//             k -> pageContentRepository.findByMultipleKeywordsCombined(combinedKeywords));
//     }
    
//     /**
//      * Performs broader search when no specific results are found
//      */
//     public List<PPCBank> performBroaderSearch(String query) {
//         // If no specific results, try to find any banking-related content
//         List<String> generalTerms = Arrays.asList("bank", "service", "account", "payment", "loan", "card");

//         for (String term : generalTerms) {
//             List<PPCBank> results = pageContentRepository.findByTitleOrContentContainingIgnoreCase(term);
//             if (!results.isEmpty()) {
//                 return results.stream().limit(5).collect(Collectors.toList());
//             }
//         }

//         // Last resort: get recent pages
//         return pageContentRepository.findRecentPages(PageRequest.of(0, 5)).getContent();
//     }
    
//     /**
//      * Prioritizes search results based on domain relevance and content quality
//      */
//     private List<PPCBank> prioritizeResultsByDomain(Set<PPCBank> results, String primaryDomain, String originalQuery) {
//         if (results.isEmpty()) return new ArrayList<>();
        
//         // Score results based on relevance
//         Map<PPCBank, Double> relevanceScores = new HashMap<>();
//         String queryLower = originalQuery.toLowerCase();
        
//         for (PPCBank page : results) {
//             double score = calculateRelevanceScore(page, primaryDomain, queryLower);
//             relevanceScores.put(page, score);
//         }
        
//         // Sort by relevance score and return top results
//         return results.stream()
//                 .sorted((p1, p2) -> Double.compare(
//                         relevanceScores.getOrDefault(p2, 0.0),
//                         relevanceScores.getOrDefault(p1, 0.0)))
//                 .limit(8)
//                 .collect(Collectors.toList());
//     }
    
//     /**
//      * Calculates relevance score for a page based on domain and content quality
//      */
//     private double calculateRelevanceScore(PPCBank page, String primaryDomain, String queryLower) {
//         double score = 0.0;
        
//         if (page.getTitle() != null) {
//             String titleLower = page.getTitle().toLowerCase();
//             // Higher weight for title matches
//             if (titleLower.contains(queryLower)) score += 10.0;
            
//             // Domain-specific title boost
//             if (primaryDomain != null) {
//                 List<String> domainKeywords = bankingDomainService.getDomainKeywords(primaryDomain);
//                 if (domainKeywords != null) {
//                     for (String keyword : domainKeywords.stream().limit(3).collect(Collectors.toList())) {
//                         if (titleLower.contains(keyword.toLowerCase())) {
//                             score += 5.0;
//                         }
//                     }
//                 }
//             }
//         }
        
//         if (page.getContent() != null) {
//             String contentLower = page.getContent().toLowerCase();
//             // Medium weight for content matches
//             if (contentLower.contains(queryLower)) score += 3.0;
            
//             // Count keyword density
//             String[] queryWords = queryLower.split("\\s+");
//             for (String word : queryWords) {
//                 if (word.length() > 2) {
//                     long count = Arrays.stream(contentLower.split("\\s+"))
//                             .filter(w -> w.contains(word))
//                             .count();
//                     score += count * 0.5;
//                 }
//             }
//         }
        
//         // Boost for pages with successful status
//         if (page.getStatus() != null && page.getStatus() == 200) {
//             score += 2.0;
//         }
        
//         // Recency boost (newer content is more relevant)
//         if (page.getUpdatedAt() != null) {
//             long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(
//                 page.getUpdatedAt().toLocalDate(), 
//                 java.time.LocalDate.now()
//             );
//             if (daysSinceUpdate < 30) score += 1.0;
//         }
        
//         return score;
//     }
    
//     /**
//      * Extracts domain-relevant snippet from content
//      */
//     public String extractDomainRelevantSnippet(String content, String query, String primaryDomain) {
//         if (content == null)
//             return "No content available.";

//         // Try to find the most relevant part of the content
//         String[] queryWords = query.toLowerCase().split("\\s+");
//         String lowerContent = content.toLowerCase();
        
//         // Add domain-specific keywords to search criteria
//         Set<String> searchTerms = new HashSet<>(Arrays.asList(queryWords));
//         if (primaryDomain != null) {
//             List<String> domainKeywords = bankingDomainService.getDomainKeywords(primaryDomain);
//             if (domainKeywords != null) {
//                 // Add top domain keywords to improve relevance
//                 searchTerms.addAll(domainKeywords.stream()
//                     .limit(3)
//                     .map(String::toLowerCase)
//                     .collect(Collectors.toList()));
//             }
//         }

//         int bestStart = 0;
//         int maxMatches = 0;

//         // Find the section with most relevant matches
//         for (int i = 0; i < content.length() - 400; i += 100) {
//             int end = Math.min(i + 400, content.length());
//             String snippet = lowerContent.substring(i, end);

//             int matches = 0;
//             for (String term : searchTerms) {
//                 if (term.length() > 2 && snippet.contains(term)) {
//                     // Give higher weight to exact query word matches
//                     if (Arrays.asList(queryWords).contains(term)) {
//                         matches += 2;
//                     } else {
//                         matches += 1;
//                     }
//                 }
//             }

//             if (matches > maxMatches) {
//                 maxMatches = matches;
//                 bestStart = i;
//             }
//         }

//         int end = Math.min(bestStart + 500, content.length());
//         String snippet = content.substring(bestStart, end);

//         if (end < content.length()) {
//             snippet += "...";
//         }
//         if (bestStart > 0) {
//             snippet = "..." + snippet;
//         }

//         return snippet;
//     }
// } 