package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.kosign.chatbotapi.domains.PageContent;
import org.kosign.chatbotapi.repository.PageContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.google.api-key}")
    private String googleApiKey;

    @Value("${ai.google.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.chat.system-prompt}")
    private String systemPrompt;

    @Autowired
    private PageContentRepository pageContentRepository;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Banking-related keywords and synonyms for better matching
    private final Map<String, List<String>> bankingKeywords = Map.of(
        "payments", Arrays.asList("payment", "pay", "bill", "bills", "transfer", "remittance", "send money"),
        "loans", Arrays.asList("loan", "credit", "lending", "borrow", "mortgage", "financing"),
        "accounts", Arrays.asList("account", "savings", "checking", "deposit", "current account"),
        "cards", Arrays.asList("card", "credit card", "debit card", "atm card", "visa", "mastercard"),
        "services", Arrays.asList("service", "banking service", "financial service", "product"),
        "rates", Arrays.asList("rate", "interest", "fees", "charges", "pricing"),
        "mobile", Arrays.asList("mobile banking", "app", "online banking", "digital", "internet banking"),
        "branches", Arrays.asList("branch", "location", "atm", "office", "address"),
        "business", Arrays.asList("corporate", "commercial", "enterprise", "company", "sme"),
        "forex", Arrays.asList("foreign exchange", "currency", "exchange rate", "usd", "dollar")
    );

    //main method that processes the user query
    public String processUserQuery(String userQuery) {
        try {
            logger.info("Processing user query: {}", userQuery);

            // Clean and normalize the query
            String normalizedQuery = normalizeQuery(userQuery);
            
            // Extract smart keywords with intent recognition
            List<String> keywords = extractSmartKeywords(normalizedQuery);
            logger.debug("Extracted smart keywords: {}", keywords);

            // Perform intelligent search
            List<PageContent> relevantPages = performIntelligentSearch(keywords, normalizedQuery);
            logger.debug("Found {} relevant pages", relevantPages.size());

            // If no specific results, try broader search
            if (relevantPages.isEmpty()) {
                relevantPages = performBroaderSearch(normalizedQuery);
                logger.debug("Broader search found {} pages", relevantPages.size());
            }

            // Build enhanced context
            String context = buildEnhancedContext(relevantPages, userQuery);

            // Generate intelligent AI response
            String aiResponse = generateIntelligentResponse(userQuery, context, relevantPages.isEmpty());

            logger.info("Successfully generated AI response for query: {}", userQuery);
            return aiResponse;

        } catch (Exception e) {
            logger.error("Error processing user query: {}", e.getMessage(), e);
            return "❌ I apologize, but I encountered an error while processing your request. Please try rephrasing your question or contact support.";
        }
    }

    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> extractSmartKeywords(String query) {
        Set<String> keywords = new HashSet<>();
        
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
        
        // Add banking-related synonyms
        for (Map.Entry<String, List<String>> entry : bankingKeywords.entrySet()) {
            for (String synonym : entry.getValue()) {
                if (query.contains(synonym)) {
                    keywords.add(entry.getKey());
                    keywords.addAll(entry.getValue());
                }
            }
        }

        // Add phrase-based keywords
        if (query.contains("bill payment") || query.contains("pay bill")) {
            keywords.addAll(Arrays.asList("payment", "bill", "pay", "transfer", "online"));
        }
        if (query.contains("exchange rate") || query.contains("currency")) {
            keywords.addAll(Arrays.asList("forex", "exchange", "rate", "currency", "usd"));
        }
        if (query.contains("mobile banking") || query.contains("app")) {
            keywords.addAll(Arrays.asList("mobile", "app", "online", "digital", "banking"));
        }

        return new ArrayList<>(keywords);
    }

    private List<PageContent> performIntelligentSearch(List<String> keywords, String originalQuery) {
        Set<PageContent> allResults = new LinkedHashSet<>();
        
        // 1. Exact phrase search first
        List<PageContent> exactResults = pageContentRepository.findByContentContainingIgnoreCase(originalQuery);
        allResults.addAll(exactResults);
        
        // 2. Multi-keyword search
        if (keywords.size() >= 2) {
            String keyword1 = keywords.size() > 0 ? keywords.get(0) : null;
            String keyword2 = keywords.size() > 1 ? keywords.get(1) : null;
            String keyword3 = keywords.size() > 2 ? keywords.get(2) : null;
            List<PageContent> multiResults = pageContentRepository.findByMultipleKeywords(keyword1, keyword2, keyword3);
            allResults.addAll(multiResults);
        }
        
        // 3. Individual keyword search with relevance scoring
        Map<PageContent, Integer> relevanceScore = new HashMap<>();
        
        for (String keyword : keywords) {
            List<PageContent> keywordResults = pageContentRepository.findByTitleOrContentContainingIgnoreCase(keyword);
            for (PageContent page : keywordResults) {
                relevanceScore.put(page, relevanceScore.getOrDefault(page, 0) + 1);
                allResults.add(page);
            }
        }
        
        // Sort by relevance score and return top results
        return allResults.stream()
                .sorted((p1, p2) -> Integer.compare(
                    relevanceScore.getOrDefault(p2, 0), 
                    relevanceScore.getOrDefault(p1, 0)
                ))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<PageContent> performBroaderSearch(String query) {
        // If no specific results, try to find any banking-related content
        List<String> generalTerms = Arrays.asList("bank", "service", "account", "payment", "loan", "card");
        
        for (String term : generalTerms) {
            List<PageContent> results = pageContentRepository.findByTitleOrContentContainingIgnoreCase(term);
            if (!results.isEmpty()) {
                return results.stream().limit(5).collect(Collectors.toList());
            }
        }
        
        // Last resort: get recent pages
        return pageContentRepository.findRecentPages(PageRequest.of(0, 5)).getContent();
    }

    private String buildEnhancedContext(List<PageContent> pages, String userQuery) {
        if (pages.isEmpty()) {
            return "While I don't have specific information about your exact query, I can help you with general banking information from PPC Bank.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Based on information from PPC Bank's website, here's what I found:\n\n");

        for (int i = 0; i < Math.min(pages.size(), 5); i++) {
            PageContent page = pages.get(i);
            context.append(String.format("**Source %d: %s**\n", i + 1, 
                page.getTitle() != null ? page.getTitle() : "PPC Bank Information"));
            
            // Extract most relevant content snippets
            String content = extractRelevantSnippet(page.getContent(), userQuery);
            context.append(String.format("Content: %s\n", content));
            context.append(String.format("URL: %s\n\n", page.getUrl()));
        }

        return context.toString();
    }

    private String extractRelevantSnippet(String content, String query) {
        if (content == null) return "No content available.";
        
        // Try to find the most relevant part of the content
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerContent = content.toLowerCase();
        
        int bestStart = 0;
        int maxMatches = 0;
        
        // Find the section with most query word matches
        for (int i = 0; i < content.length() - 400; i += 100) {
            int end = Math.min(i + 400, content.length());
            String snippet = lowerContent.substring(i, end);
            
            int matches = 0;
            for (String word : queryWords) {
                if (word.length() > 2 && snippet.contains(word)) {
                    matches++;
                }
            }
            
            if (matches > maxMatches) {
                maxMatches = matches;
                bestStart = i;
            }
        }
        
        int end = Math.min(bestStart + 500, content.length());
        String snippet = content.substring(bestStart, end);
        
        if (end < content.length()) {
            snippet += "...";
        }
        
        return snippet;
    }

    private String generateIntelligentResponse(String userQuery, String context, boolean noSpecificData) throws IOException {
        String enhancedPrompt;
        
        if (noSpecificData) {
            enhancedPrompt = String.format("""
                You are a helpful and knowledgeable banking assistant for PPC Bank. The user asked: "%s"
                
                While I don't have specific information about this exact topic in my current database, 
                please provide a helpful, general response about this banking topic. Be informative and professional.
                
                If appropriate, suggest that the user:
                1. Visit the main PPC Bank website for the most current information
                2. Contact PPC Bank directly for specific details
                3. Visit a branch for personalized assistance
                
                Make your response warm, helpful, and informative even without specific data.
                """, userQuery);
        } else {
            enhancedPrompt = String.format("""
                You are a helpful and knowledgeable banking assistant for PPC Bank. 
                
                User's question: "%s"
                
                %s
                
                Instructions:
                1. Provide a comprehensive, helpful answer based on the information above
                2. Be conversational and friendly
                3. If the information seems incomplete, acknowledge it and suggest where to get more details
                4. Format your response nicely with bullet points or sections if appropriate
                5. Always end with an offer to help with anything else
                6. Be proactive - if they ask about one service, mention related services they might be interested in
                """, userQuery, context);
        }

        return callGeminiAPI(enhancedPrompt);
    }

    private String callGeminiAPI(String prompt) throws IOException {
        // Escape the prompt properly for JSON
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 1500,
                    "topP": 0.8,
                    "topK": 40
                }
            }
            """, escapedPrompt);

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                model, googleApiKey);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.get("application/json"), requestBody))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AI API error: " + response.code() + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            JsonNode candidates = jsonResponse.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }
            
            return "I apologize, but I'm having trouble generating a response right now. Please try again or contact PPC Bank directly for assistance.";
        }
    }

    public String getDatabaseStats() {
        try {
            Long totalPages = pageContentRepository.getTotalPageCount();
            List<PageContent> recentPages = pageContentRepository.findRecentPages(PageRequest.of(0, 5)).getContent();

            StringBuilder stats = new StringBuilder();
            stats.append(String.format("📊 **PPC Bank Information Database**\n\n"));
            stats.append(String.format("📄 **Total pages indexed:** %d\n", totalPages));
            stats.append(String.format("🕒 **Recently updated pages:** %d\n\n", recentPages.size()));

            if (!recentPages.isEmpty()) {
                stats.append("**Recent content:**\n");
                for (PageContent page : recentPages) {
                    stats.append(String.format("• %s\n", 
                        page.getTitle() != null ? page.getTitle() : page.getUrl()));
                }
            }

            stats.append("\n💡 **Tip:** Ask me about PPC Bank services, accounts, loans, payments, or any banking topic!");

            return stats.toString();
        } catch (Exception e) {
            logger.error("Error getting database stats: {}", e.getMessage(), e);
            return "❌ Unable to retrieve database statistics at the moment.";
        }
    }
}

