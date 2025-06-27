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
            "forex", Arrays.asList("foreign exchange", "currency", "exchange rate", "usd", "dollar"));

    // main method that processes the user query
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
                        relevanceScore.getOrDefault(p1, 0)))
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
        if (content == null)
            return "No content available.";

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

    private String generateIntelligentResponse(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        String basePrompt = buildBasePrompt(userQuery);
        String enhancedPrompt = noSpecificData ? basePrompt + buildGeneralResponseInstructions()
                : basePrompt + buildContextualResponseInstructions(context);

        return callGeminiAPI(enhancedPrompt);
    }

    private String buildBasePrompt(String userQuery) {
        return String.format("""
                You are PPC Bank's AI assistant. User asks: "%s"

                RESPONSE STRUCTURE:
                🎯 **Direct Answer** - Answer the specific question immediately
                📋 **Details** - Key information in table format when applicable
                💡 **Important Notes** - Critical considerations or warnings
                🔗 **Next Steps** - Clear action items
                ❓ **Related Questions** - 2-3 relevant follow-up questions

                TONE: Professional, warm, helpful
                FORMAT: Use tables for requirements/amounts/documents
                """, userQuery);
    }

    private String buildGeneralResponseInstructions() {
        return """

                INSTRUCTIONS:
                - Provide general banking guidance since no specific PPC Bank data is available
                - Use banking best practices and common industry standards
                - Include disclaimer: "For specific PPC Bank requirements, please visit a branch or check our website"
                - Structure information clearly with tables where helpful
                - Focus on practical, actionable advice
                """;
    }

    private String buildContextualResponseInstructions(String context) {
        return String.format("""

                PPC BANK INFORMATION:
                %s

                INSTRUCTIONS:
                - Use the provided PPC Bank information as your primary source
                - Answer directly and specifically based on this data
                - If information is incomplete, note what might be missing
                - Create tables for document requirements, fees, or limits
                - Suggest contacting PPC Bank for any unclear details
                """, context);
    }

    // Alternative: Single optimized method approach
    private String generateIntelligentResponseOptimized(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        StringBuilder prompt = new StringBuilder();

        // Core prompt - always included
        prompt.append(String.format("""
                You are PPC Bank's helpful AI assistant. User question: "%s"

                RESPONSE FORMAT:
                🎯 **Answer**: [Direct response to the question]
                """, userQuery));

        // Add specific sections based on query type
        if (containsDocumentQuery(userQuery)) {
            prompt.append("""
                    📋 **Requirements**:
                    | Document | Validity | Notes |
                    |----------|----------|-------|
                    | [Doc 1] | [Period] | [Details] |

                    """);
        }

        if (containsAmountQuery(userQuery)) {
            prompt.append("""
                    💰 **Amounts & Fees**:
                    | Type | Amount | Fee | Limit |
                    |------|--------|-----|-------|
                    | [Type] | [Amount] | [Fee] | [Limit] |

                    """);
        }

        // Always include these sections
        prompt.append("""
                💡 **Important**: [Key considerations]
                🔗 **Next Steps**: [What to do next]
                ❓ **You might ask**: [2-3 follow-up questions]

                """);

        // Context-specific instructions
        if (noSpecificData) {
            prompt.append("""
                    GUIDELINES:
                    - Provide general banking guidance (no specific PPC Bank data available)
                    - Include: "Contact PPC Bank directly for specific requirements"
                    - Use industry-standard practices and common requirements
                    """);
        } else {
            prompt.append(String.format("""
                    PPC BANK DATA:
                    %s

                    GUIDELINES:
                    - Use provided PPC Bank information as primary source
                    - Be specific and accurate based on this data
                    - If data seems incomplete, mention what might be missing
                    """, context));
        }

        return callGeminiAPI(prompt.toString());
    }

    // Helper methods for query analysis
    private boolean containsDocumentQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("document") || lowerQuery.contains("requirement") ||
                lowerQuery.contains("passport") || lowerQuery.contains("certificate") ||
                lowerQuery.contains("need") || lowerQuery.contains("bring");
    }

    private boolean containsAmountQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("amount") || lowerQuery.contains("fee") ||
                lowerQuery.contains("cost") || lowerQuery.contains("minimum") ||
                lowerQuery.contains("maximum") || lowerQuery.contains("limit") ||
                lowerQuery.contains("charge");
    }

    // Enhanced version with dynamic prompt building
    private String generateSmartResponse(String userQuery, String context, boolean noSpecificData)
            throws IOException {

        PromptBuilder builder = new PromptBuilder(userQuery);

        // Analyze query and add appropriate sections
        QueryAnalysis analysis = analyzeQuery(userQuery);

        builder.addDirectAnswerSection()
                .addConditionalTable(analysis.needsDocumentTable(), "Requirements")
                .addConditionalTable(analysis.needsAmountTable(), "Amounts & Fees")
                .addImportantNotes()
                .addNextSteps()
                .addFollowUpQuestions();

        if (noSpecificData) {
            builder.addGeneralBankingInstructions();
        } else {
            builder.addContextualInstructions(context);
        }

        return callGeminiAPI(builder.build());
    }

    private static class PromptBuilder {
        private StringBuilder prompt;
        private String userQuery;

        public PromptBuilder(String userQuery) {
            this.userQuery = userQuery;
            this.prompt = new StringBuilder();
            prompt.append(String.format("You are PPC Bank's AI assistant. User asks: \"%s\"\n\n", userQuery));
        }

        public PromptBuilder addDirectAnswerSection() {
            prompt.append("🎯 **Direct Answer**: [Answer the question immediately]\n\n");
            return this;
        }

        public PromptBuilder addConditionalTable(boolean condition, String type) {
            if (condition) {
                if ("Requirements".equals(type)) {
                    prompt.append("""
                            📋 **Requirements**:
                            | Document | Validity | Purpose |
                            |----------|----------|---------|
                            | [Item] | [Period] | [Reason] |

                            """);
                } else if ("Amounts & Fees".equals(type)) {
                    prompt.append("""
                            💰 **Amounts & Fees**:
                            | Type | Amount | Fee |
                            |------|--------|-----|
                            | [Type] | [Amount] | [Fee] |

                            """);
                }
            }
            return this;
        }

        public PromptBuilder addImportantNotes() {
            prompt.append("💡 **Important**: [Critical considerations]\n");
            return this;
        }

        public PromptBuilder addNextSteps() {
            prompt.append("🔗 **Next Steps**: [Clear actions to take]\n");
            return this;
        }

        public PromptBuilder addFollowUpQuestions() {
            prompt.append("❓ **Related Questions**: [2-3 relevant follow-ups]\n\n");
            return this;
        }

        public PromptBuilder addGeneralBankingInstructions() {
            prompt.append("""
                    INSTRUCTIONS:
                    - Provide helpful general banking guidance
                    - Include: "Contact PPC Bank for specific requirements"
                    - Use professional, warm tone
                    """);
            return this;
        }

        public PromptBuilder addContextualInstructions(String context) {
            prompt.append(String.format("""
                    PPC BANK INFO: %s

                    INSTRUCTIONS:
                    - Use provided information as primary source
                    - Be specific and accurate
                    - Professional, helpful tone
                    """, context));
            return this;
        }

        public String build() {
            return prompt.toString();
        }
    }

    private static class QueryAnalysis {
        private boolean needsDocumentTable;
        private boolean needsAmountTable;

        public QueryAnalysis(boolean needsDocumentTable, boolean needsAmountTable) {
            this.needsDocumentTable = needsDocumentTable;
            this.needsAmountTable = needsAmountTable;
        }

        public boolean needsDocumentTable() {
            return needsDocumentTable;
        }

        public boolean needsAmountTable() {
            return needsAmountTable;
        }
    }

    private QueryAnalysis analyzeQuery(String query) {
        String lower = query.toLowerCase();
        boolean docTable = lower.matches(".*(document|requirement|passport|certificate|valid|bring|need).*");
        boolean amountTable = lower.matches(".*(amount|fee|cost|minimum|maximum|limit|charge|price).*");
        return new QueryAnalysis(docTable, amountTable);
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

            stats.append(
                    "\n💡 **Tip:** Ask me about PPC Bank services, accounts, loans, payments, or any banking topic!");

            return stats.toString();
        } catch (Exception e) {
            logger.error("Error getting database stats: {}", e.getMessage(), e);
            return "❌ Unable to retrieve database statistics at the moment.";
        }
    }
}
