package org.kosign.chatbotapi.model;

import lombok.Data;
import org.kosign.chatbotapi.domains.PPCBank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation context to maintain state across queries
 */
@Data
public class ConversationContext {
    private String lastDomain;
    private List<String> lastKeywords;
    private List<PPCBank> lastResults;
    private long lastAccessTime;
    private int searchCount;
    private String jsonContext;
    private Instant createdAt;
    
    public ConversationContext(String domain, List<String> keywords, List<PPCBank> results) {
        this.lastDomain = domain;
        this.lastKeywords = new ArrayList<>(keywords);
        this.lastResults = new ArrayList<>(results);
        this.lastAccessTime = System.currentTimeMillis();
        this.searchCount = 0;
        this.createdAt = Instant.now();
    }
    
    // New constructor for JSON context
    public ConversationContext(String jsonContext, Instant createdAt) {
        this.jsonContext = jsonContext;
        this.createdAt = createdAt;
        this.lastAccessTime = System.currentTimeMillis();
        this.searchCount = 0;
    }
    
    public boolean isExpired(long maxAgeMs) {
        return (System.currentTimeMillis() - lastAccessTime) > maxAgeMs;
    }
    
    public void updateAccess() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public void incrementSearchCount() {
        this.searchCount++;
    }
    
    // Getters
    public String getLastDomain() { 
        return lastDomain; 
    }
    
    public List<String> getLastKeywords() { 
        return lastKeywords; 
    }
    
    public List<PPCBank> getLastResults() { 
        return lastResults; 
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public int getSearchCount() {
        return searchCount;
    }
    
    public String getJsonContext() {
        return jsonContext;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
} 