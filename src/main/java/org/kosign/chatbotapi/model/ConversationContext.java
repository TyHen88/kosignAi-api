package org.kosign.chatbotapi.model;

import lombok.Data;
import org.kosign.chatbotapi.domains.PPCBank;

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
    
    public ConversationContext(String domain, List<String> keywords, List<PPCBank> results) {
        this.lastDomain = domain;
        this.lastKeywords = new ArrayList<>(keywords);
        this.lastResults = new ArrayList<>(results);
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public boolean isExpired(long maxAgeMs) {
        return (System.currentTimeMillis() - lastAccessTime) > maxAgeMs;
    }
    
    public void updateAccess() {
        this.lastAccessTime = System.currentTimeMillis();
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
} 