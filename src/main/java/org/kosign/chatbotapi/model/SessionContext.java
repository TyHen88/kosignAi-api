package org.kosign.chatbotapi.model;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Session context to store query context and JSON data for reuse
 */
@Data
@Builder
public class SessionContext {
    private String sessionId;
    private String lastQuery;
    private String normalizedLastQuery;
    private String lastQueryIntent;
    private List<TitleMatchResult> lastMatchResults;
    private String lastJsonContext;
    private JsonNode lastStructuredData;
    private LocalDateTime lastAccessTime;
    private LocalDateTime createdTime;
    private int queryCount;
    private Map<String, Object> metadata;

    // Static method to create a new session context
    public static SessionContext create(String sessionId) {
        return SessionContext.builder()
                .sessionId(sessionId)
                .createdTime(LocalDateTime.now())
                .lastAccessTime(LocalDateTime.now())
                .queryCount(0)
                .metadata(new ConcurrentHashMap<>())
                .build();
    }

    // Update context with new query data
    public void updateContext(String query, String normalizedQuery, String queryIntent, 
                             List<TitleMatchResult> matchResults, String jsonContext) {
        this.lastQuery = query;
        this.normalizedLastQuery = normalizedQuery;
        this.lastQueryIntent = queryIntent;
        this.lastMatchResults = matchResults;
        this.lastJsonContext = jsonContext;
        this.lastAccessTime = LocalDateTime.now();
        this.queryCount++;
    }

    // Check if context is expired (older than specified minutes)
    public boolean isExpired(int maxAgeMinutes) {
        return lastAccessTime.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }

    // Check if context is relevant to current query
    public boolean isRelevantTo(String currentQuery, String currentQueryIntent) {
        if (lastQuery == null || lastQueryIntent == null) {
            return false;
        }
        
        // Check if query intent is the same
        if (currentQueryIntent.equals(lastQueryIntent)) {
            return true;
        }
        
        // Check if queries have similar keywords
        String[] lastWords = normalizedLastQuery != null ? normalizedLastQuery.split("\\s+") : new String[0];
        String[] currentWords = currentQuery.split("\\s+");
        
        int commonWords = 0;
        for (String currentWord : currentWords) {
            for (String lastWord : lastWords) {
                if (currentWord.length() > 3 && lastWord.length() > 3 && 
                    (currentWord.contains(lastWord) || lastWord.contains(currentWord))) {
                    commonWords++;
                    break;
                }
            }
        }
        
        // Consider relevant if at least 50% of words are similar and both queries have substantial words
        return currentWords.length > 1 && lastWords.length > 1 && 
               (double) commonWords / Math.min(currentWords.length, lastWords.length) >= 0.5;
    }

    // Check if there are high-confidence matches available
    public boolean hasHighConfidenceMatches() {
        return lastMatchResults != null && !lastMatchResults.isEmpty() && 
               lastMatchResults.get(0).isHighConfidence();
    }

    // Update access time
    public void touch() {
        this.lastAccessTime = LocalDateTime.now();
    }

    // Add metadata
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new ConcurrentHashMap<>();
        }
        metadata.put(key, value);
    }

    // Get metadata
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
} 