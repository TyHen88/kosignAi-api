package org.kosign.chatbotapi.util;

/**
 * Utility class for analyzing user queries and determining response structure needs
 */
public class QueryAnalysis {
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
    
    /**
     * Analyzes a query to determine what type of structured response is needed
     */
    public static QueryAnalysis analyze(String query) {
        String lower = query.toLowerCase();
        boolean docTable = lower.matches(".*(document|requirement|passport|certificate|valid|bring|need).*");
        boolean amountTable = lower.matches(".*(amount|fee|cost|minimum|maximum|limit|charge|price).*");
        return new QueryAnalysis(docTable, amountTable);
    }
    
    /**
     * Helper methods for query analysis
     */
    public static boolean containsDocumentQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("document") || lowerQuery.contains("requirement") ||
                lowerQuery.contains("passport") || lowerQuery.contains("certificate") ||
                lowerQuery.contains("need") || lowerQuery.contains("bring");
    }

    public static boolean containsAmountQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("amount") || lowerQuery.contains("fee") ||
                lowerQuery.contains("cost") || lowerQuery.contains("minimum") ||
                lowerQuery.contains("maximum") || lowerQuery.contains("limit") ||
                lowerQuery.contains("charge");
    }
} 