package org.kosign.chatbotapi.model;

import lombok.Builder;
import lombok.Data;
import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.domains.Workflow;

import java.util.List;

/**
 * Represents the result of title matching with scoring and metadata
 */
@Data
@Builder
public class TitleMatchResult {
    
    /**
     * The matched page from the database
     */
    private PPCBank page;

    private Workflow workflow;
    
    /**
     * The confidence score of the match (0.0 to 100.0+)
     */
    private double matchScore;
    
    /**
     * List of keywords from the user query that matched this title
     */
    private List<String> matchedKeywords;
    
    /**
     * Type of match found (EXACT_PHRASE, HIGH_KEYWORD, MEDIUM_KEYWORD, FUZZY_MATCH)
     */
    private String matchType;
    
    /**
     * Convenience method to get the title
     */
    public String getTitle() {
        return page != null ? page.getTitle() : null;
    }
    
    /**
     * Convenience method to get the content
     */
    public String getContent() {
        return page != null ? page.getContent() : null;
    }
    
    /**
     * Convenience method to get the URL
     */
    public String getUrl() {
        return page != null ? page.getUrl() : null;
    }
    
    /**
     * Convenience method to get the JSON content
     */
    public String getContentJson() {
        return page != null ? page.getContentJson() : null;
    }
    
    /**
     * Checks if this is a high-confidence match
     */
    public boolean isHighConfidence() {
        return matchScore >= 70.0;
    }
    
    /**
     * Checks if this is a medium-confidence match
     */
    public boolean isMediumConfidence() {
        return matchScore >= 40.0 && matchScore < 70.0;
    }
    
    /**
     * Gets a human-readable confidence level
     */
    public String getConfidenceLevel() {
        if (matchScore >= 90.0) return "VERY_HIGH";
        if (matchScore >= 70.0) return "HIGH";
        if (matchScore >= 40.0) return "MEDIUM";
        if (matchScore >= 20.0) return "LOW";
        return "VERY_LOW";
    }
    
    /**
     * Gets a summary of the match for logging/debugging
     */
    public String getMatchSummary() {
        return String.format("Title: %s, Score: %.2f, Type: %s, Keywords: %s", 
            getTitle(), matchScore, matchType, matchedKeywords);
    }
} 