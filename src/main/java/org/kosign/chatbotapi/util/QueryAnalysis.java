package org.kosign.chatbotapi.util;

/**
 * Utility class for analyzing user queries and determining response structure needs
 */
public class QueryAnalysis {
    private boolean needsDocumentTable;
    private boolean needsAmountTable;
    private boolean needsInterestTable;

    public QueryAnalysis(boolean needsDocumentTable, boolean needsAmountTable, boolean needsInterestTable) {
        this.needsDocumentTable = needsDocumentTable;
        this.needsAmountTable = needsAmountTable;
        this.needsInterestTable = needsInterestTable;
    }

    public boolean needsDocumentTable() {
        return needsDocumentTable;
    }

    public boolean needsAmountTable() {
        return needsAmountTable;
    }

    public boolean needsInterestTable() {
        return needsInterestTable;
    }
    
    /**
     * Analyzes a query to determine what type of structured response is needed
     */
    public static QueryAnalysis analyze(String query) {
        String lower = query.toLowerCase();
        boolean docTable = lower.matches(".*(document|requirement|passport|certificate|valid|bring|need).*");
        boolean amountTable = lower.matches(".*(amount|fee|cost|minimum|maximum|limit|charge|price).*");
        boolean interestTable = lower.matches(".*(interest|rate|loan|credit|deposit|saving).*");
        return new QueryAnalysis(docTable, amountTable, interestTable);
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

    public static boolean containsInterestQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("interest") || lowerQuery.contains("rate") ||
                lowerQuery.contains("loan") || lowerQuery.contains("credit") ||
                lowerQuery.contains("deposit") || lowerQuery.contains("saving");
    }
}
