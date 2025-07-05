package org.kosign.chatbotapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kosign.chatbotapi.model.TitleMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for creating structured JSON responses from database results
 */
@Service
public class JsonResponseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonResponseService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Creates a structured JSON response from title match results
     */
    public String createStructuredResponse(List<TitleMatchResult> matchResults, String userQuery) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            
            // Add metadata
            response.put("userQuery", userQuery);
            response.put("timestamp", System.currentTimeMillis());
            response.put("totalResults", matchResults.size());
            
            // Add match summary
            ObjectNode matchSummary = objectMapper.createObjectNode();
            if (!matchResults.isEmpty()) {
                TitleMatchResult bestMatch = matchResults.get(0);
                matchSummary.put("bestMatchScore", bestMatch.getMatchScore());
                matchSummary.put("bestMatchType", bestMatch.getMatchType());
                matchSummary.put("confidenceLevel", bestMatch.getConfidenceLevel());
                matchSummary.put("hasHighConfidenceMatch", bestMatch.isHighConfidence());
            }
            response.set("matchSummary", matchSummary);
            
            // Add detailed results
            ArrayNode results = objectMapper.createArrayNode();
            for (TitleMatchResult match : matchResults) {
                ObjectNode resultNode = createResultNode(match);
                results.add(resultNode);
            }
            response.set("results", results);
            
            return objectMapper.writeValueAsString(response);
            
        } catch (JsonProcessingException e) {
            logger.error("Error creating JSON response", e);
            return createErrorResponse("Failed to create structured response", userQuery);
        }
    }
    
    /**
     * Creates a result node for a single match
     */
    private ObjectNode createResultNode(TitleMatchResult match) {
        ObjectNode resultNode = objectMapper.createObjectNode();
        
        // Basic match information
        resultNode.put("title", match.getTitle());
        resultNode.put("url", match.getUrl());
        resultNode.put("matchScore", match.getMatchScore());
        resultNode.put("matchType", match.getMatchType());
        resultNode.put("confidenceLevel", match.getConfidenceLevel());
        
        // Matched keywords
        ArrayNode keywordsArray = objectMapper.createArrayNode();
        if (match.getMatchedKeywords() != null) {
            match.getMatchedKeywords().forEach(keywordsArray::add);
        }
        resultNode.set("matchedKeywords", keywordsArray);
        
        // Content processing
        if (match.getContentJson() != null && !match.getContentJson().trim().isEmpty()) {
            try {
                JsonNode contentJson = objectMapper.readTree(match.getContentJson());
                resultNode.set("structuredContent", processStructuredContent(contentJson));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse JSON content for title: {}", match.getTitle());
                resultNode.put("rawContent", extractTextContent(match.getContent()));
            }
        } else {
            resultNode.put("rawContent", extractTextContent(match.getContent()));
        }
        
        // Page metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        if (match.getPage() != null) {
            metadata.put("pageId", match.getPage().getId());
            metadata.put("status", match.getPage().getStatus());
            metadata.put("depth", match.getPage().getDepth());
            if (match.getPage().getUpdatedAt() != null) {
                metadata.put("lastUpdated", match.getPage().getUpdatedAt().toString());
            }
        }
        resultNode.set("metadata", metadata);
        
        return resultNode;
    }
    
    /**
     * Processes structured JSON content for better AI understanding
     */
    private ObjectNode processStructuredContent(JsonNode contentJson) {
        ObjectNode processed = objectMapper.createObjectNode();
        
        // Extract title
        if (contentJson.has("title")) {
            processed.put("title", contentJson.get("title").asText());
        }
        
        // Process sections
        if (contentJson.has("sections") && contentJson.get("sections").isArray()) {
            ArrayNode sectionsArray = objectMapper.createArrayNode();
            for (JsonNode section : contentJson.get("sections")) {
                ObjectNode sectionNode = objectMapper.createObjectNode();
                
                if (section.has("heading")) {
                    sectionNode.put("heading", section.get("heading").asText());
                }
                
                if (section.has("paragraphs") && section.get("paragraphs").isArray()) {
                    ArrayNode paragraphsArray = objectMapper.createArrayNode();
                    for (JsonNode paragraph : section.get("paragraphs")) {
                        paragraphsArray.add(paragraph.asText());
                    }
                    sectionNode.set("paragraphs", paragraphsArray);
                }
                
                if (section.has("list") && section.get("list").isArray()) {
                    ArrayNode listArray = objectMapper.createArrayNode();
                    for (JsonNode listItem : section.get("list")) {
                        listArray.add(listItem.asText());
                    }
                    sectionNode.set("list", listArray);
                }
                
                sectionsArray.add(sectionNode);
            }
            processed.set("sections", sectionsArray);
        }
        
        // Process tables
        if (contentJson.has("tables") && contentJson.get("tables").isArray()) {
            ArrayNode tablesArray = objectMapper.createArrayNode();
            for (JsonNode table : contentJson.get("tables")) {
                ObjectNode tableNode = objectMapper.createObjectNode();
                
                // Check if headers exist and are not empty
                boolean hasHeaders = table.has("headers") && 
                                   table.get("headers").isArray() && 
                                   table.get("headers").size() > 0;
                
                if (table.has("rows") && table.get("rows").isArray()) {
                    JsonNode rows = table.get("rows");
                    
                    if (hasHeaders) {
                        // Use existing headers
                        ArrayNode headersArray = objectMapper.createArrayNode();
                        for (JsonNode header : table.get("headers")) {
                            headersArray.add(header.asText());
                        }
                        tableNode.set("headers", headersArray);
                        
                        // Add all rows as data
                        ArrayNode rowsArray = objectMapper.createArrayNode();
                        for (JsonNode row : rows) {
                            if (row.isArray()) {
                                ArrayNode rowArray = objectMapper.createArrayNode();
                                for (JsonNode cell : row) {
                                    rowArray.add(cell.asText());
                                }
                                rowsArray.add(rowArray);
                            }
                        }
                        tableNode.set("rows", rowsArray);
                    } else if (rows.size() > 0) {
                        // Headers are null/empty, use first row as headers
                        JsonNode firstRow = rows.get(0);
                        if (firstRow.isArray()) {
                            ArrayNode headersArray = objectMapper.createArrayNode();
                            for (JsonNode header : firstRow) {
                                headersArray.add(header.asText());
                            }
                            tableNode.set("headers", headersArray);
                            
                            // Add remaining rows as data (skip first row since it's headers)
                            ArrayNode rowsArray = objectMapper.createArrayNode();
                            for (int i = 1; i < rows.size(); i++) {
                                JsonNode row = rows.get(i);
                                if (row.isArray()) {
                                    ArrayNode rowArray = objectMapper.createArrayNode();
                                    for (JsonNode cell : row) {
                                        rowArray.add(cell.asText());
                                    }
                                    rowsArray.add(rowArray);
                                }
                            }
                            tableNode.set("rows", rowsArray);
                        }
                    }
                }
                
                // Only add table if it has meaningful data
                if (tableNode.has("headers") && tableNode.has("rows")) {
                    tablesArray.add(tableNode);
                }
            }
            processed.set("tables", tablesArray);
        }
        
        return processed;
    }
    
    /**
     * Extracts and cleans text content
     */
    private String extractTextContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "No content available";
        }
        
        // Clean up the content
        String cleaned = content
            .replaceAll("\\s+", " ")
            .trim();
        
        // Truncate if too long
        if (cleaned.length() > 1000) {
            cleaned = cleaned.substring(0, 1000) + "...";
        }
        
        return cleaned;
    }
    
    /**
     * Creates a simple JSON response for a single best match
     */
    public String createSimpleResponse(TitleMatchResult match, String userQuery) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            
            response.put("userQuery", userQuery);
            response.put("timestamp", System.currentTimeMillis());
            response.put("hasMatch", match != null);
            
            if (match != null) {
                response.set("result", createResultNode(match));
            } else {
                response.put("message", "No matching content found for the query");
            }
            
            return objectMapper.writeValueAsString(response);
            
        } catch (JsonProcessingException e) {
            logger.error("Error creating simple JSON response", e);
            return createErrorResponse("Failed to create response", userQuery);
        }
    }
    
    /**
     * Creates an error response
     */
    private String createErrorResponse(String error, String userQuery) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("userQuery", userQuery);
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", error);
            response.put("hasMatch", false);
            
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            logger.error("Error creating error response", e);
            return "{\"error\":\"System error occurred\",\"hasMatch\":false}";
        }
    }
    
    /**
     * Creates a response optimized for AI processing
     */
    public String createAIOptimizedResponse(List<TitleMatchResult> matchResults, String userQuery) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            
            // Add query context
            response.put("userQuery", userQuery);
            response.put("intent", detectQueryIntent(userQuery));
            response.put("hasRelevantData", !matchResults.isEmpty());
            
            // Add concise match information
            if (!matchResults.isEmpty()) {
                TitleMatchResult bestMatch = matchResults.get(0);
                
                ObjectNode primaryResult = objectMapper.createObjectNode();
                primaryResult.put("title", bestMatch.getTitle());
                primaryResult.put("confidence", bestMatch.getConfidenceLevel());
                primaryResult.put("matchType", bestMatch.getMatchType());
                
                // Add only the most relevant content
                if (bestMatch.getContentJson() != null) {
                    try {
                        JsonNode contentJson = objectMapper.readTree(bestMatch.getContentJson());
                        primaryResult.set("keyInformation", extractKeyInformation(contentJson, userQuery));
                    } catch (JsonProcessingException e) {
                        primaryResult.put("keyInformation", extractTextContent(bestMatch.getContent()));
                    }
                } else {
                    primaryResult.put("keyInformation", extractTextContent(bestMatch.getContent()));
                }
                
                response.set("primaryResult", primaryResult);
                
                // Add additional results if they're high confidence
                ArrayNode additionalResults = objectMapper.createArrayNode();
                for (int i = 1; i < Math.min(matchResults.size(), 3); i++) {
                    TitleMatchResult additionalMatch = matchResults.get(i);
                    if (additionalMatch.isHighConfidence() || additionalMatch.isMediumConfidence()) {
                        ObjectNode additionalNode = objectMapper.createObjectNode();
                        additionalNode.put("title", additionalMatch.getTitle());
                        additionalNode.put("confidence", additionalMatch.getConfidenceLevel());
                        additionalResults.add(additionalNode);
                    }
                }
                response.set("additionalResults", additionalResults);
            }
            
            return objectMapper.writeValueAsString(response);
            
        } catch (JsonProcessingException e) {
            logger.error("Error creating AI-optimized response", e);
            return createErrorResponse("Failed to create AI-optimized response", userQuery);
        }
    }
    
    /**
     * Detects the intent of the user query
     */
    public String detectQueryIntent(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("how") || lowerQuery.contains("process") || lowerQuery.contains("steps")) {
            return "HOW_TO";
        } else if (lowerQuery.contains("what") || lowerQuery.contains("definition") || lowerQuery.contains("explain")) {
            return "DEFINITION";
        } else if (lowerQuery.contains("requirement") || lowerQuery.contains("document") || lowerQuery.contains("need")) {
            return "REQUIREMENTS";
        } else if (lowerQuery.contains("fee") || lowerQuery.contains("cost") || lowerQuery.contains("price") || lowerQuery.contains("rate")) {
            return "PRICING";
        } else if (lowerQuery.contains("where") || lowerQuery.contains("contact") || lowerQuery.contains("branch")) {
            return "LOCATION";
        } else {
            return "GENERAL_INQUIRY";
        }
    }
    
    /**
     * Extracts key information relevant to the query
     */
    private JsonNode extractKeyInformation(JsonNode contentJson, String userQuery) {
        ObjectNode keyInfo = objectMapper.createObjectNode();
        
        // Extract title
        if (contentJson.has("title")) {
            keyInfo.put("title", contentJson.get("title").asText());
        }
        
        // Extract most relevant sections based on query
        if (contentJson.has("sections") && contentJson.get("sections").isArray()) {
            ArrayNode relevantSections = objectMapper.createArrayNode();
            String lowerQuery = userQuery.toLowerCase();
            
            for (JsonNode section : contentJson.get("sections")) {
                if (section.has("heading")) {
                    String heading = section.get("heading").asText().toLowerCase();
                    if (isRelevantSection(heading, lowerQuery)) {
                        ObjectNode sectionNode = objectMapper.createObjectNode();
                        sectionNode.put("heading", section.get("heading").asText());
                        
                        if (section.has("paragraphs") && section.get("paragraphs").isArray()) {
                            ArrayNode paragraphs = objectMapper.createArrayNode();
                            for (JsonNode paragraph : section.get("paragraphs")) {
                                paragraphs.add(paragraph.asText());
                            }
                            sectionNode.set("paragraphs", paragraphs);
                        }
                        
                        if (section.has("list") && section.get("list").isArray()) {
                            ArrayNode list = objectMapper.createArrayNode();
                            for (JsonNode listItem : section.get("list")) {
                                list.add(listItem.asText());
                            }
                            sectionNode.set("list", list);
                        }
                        
                        relevantSections.add(sectionNode);
                    }
                }
            }
            keyInfo.set("relevantSections", relevantSections);
        }
        
        // Extract relevant tables
        if (contentJson.has("tables") && contentJson.get("tables").isArray()) {
            ArrayNode tables = objectMapper.createArrayNode();
            for (JsonNode table : contentJson.get("tables")) {
                tables.add(table);
            }
            keyInfo.set("tables", tables);
        }
        
        return keyInfo;
    }
    
    /**
     * Checks if a section is relevant to the query
     */
    private boolean isRelevantSection(String heading, String query) {
        // Simple relevance check - can be enhanced with more sophisticated matching
        String[] queryWords = query.split("\\s+");
        for (String word : queryWords) {
            if (word.length() > 3 && heading.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates a clean, focused response optimized for AI processing
     */
    public String createCleanAIResponse(List<TitleMatchResult> matchResults, String userQuery) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            
            // Add basic query context
            response.put("userQuery", userQuery);
            response.put("queryIntent", detectQueryIntent(userQuery));
            
            if (!matchResults.isEmpty()) {
                TitleMatchResult bestMatch = matchResults.get(0);
                
                // Create clean, focused content
                ObjectNode cleanContent = objectMapper.createObjectNode();
                cleanContent.put("title", bestMatch.getTitle());
                cleanContent.put("url", bestMatch.getUrl());
                cleanContent.put("matchConfidence", bestMatch.getConfidenceLevel());
                
                // Extract and clean the most relevant content
                String relevantContent = extractRelevantContent(bestMatch, userQuery);
                cleanContent.put("relevantInformation", relevantContent);
                
                // Add structured content if available
                if (bestMatch.getContentJson() != null && !bestMatch.getContentJson().trim().isEmpty()) {
                    try {
                        JsonNode contentJson = objectMapper.readTree(bestMatch.getContentJson());
                        ObjectNode structuredInfo = extractStructuredInfo(contentJson, userQuery);
                        cleanContent.set("structuredData", structuredInfo);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse JSON content for title: {}", bestMatch.getTitle());
                    }
                }
                
                response.set("bankInformation", cleanContent);
                
                // Add additional matches only if they're high confidence
                if (matchResults.size() > 1) {
                    ArrayNode additionalInfo = objectMapper.createArrayNode();
                    for (int i = 1; i < Math.min(matchResults.size(), 2); i++) {
                        TitleMatchResult additionalMatch = matchResults.get(i);
                        if (additionalMatch.isHighConfidence()) {
                            ObjectNode additionalNode = objectMapper.createObjectNode();
                            additionalNode.put("title", additionalMatch.getTitle());
                            additionalNode.put("relevantInfo", extractRelevantContent(additionalMatch, userQuery));
                            additionalInfo.add(additionalNode);
                        }
                    }
                    if (additionalInfo.size() > 0) {
                        response.set("additionalInformation", additionalInfo);
                    }
                }
            }
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            
            // Check size and log warning if too large
            if (jsonResponse.length() > 6000) {
                logger.warn("⚠️ Large JSON response generated ({} chars) - may cause AI timeout", jsonResponse.length());
                // Could implement truncation logic here if needed
            }
            
            return jsonResponse;
            
        } catch (JsonProcessingException e) {
            logger.error("Error creating clean AI response", e);
            return createErrorResponse("Failed to create clean response", userQuery);
        }
    }
    
    /**
     * Extracts relevant content based on the user query
     */
    private String extractRelevantContent(TitleMatchResult match, String userQuery) {
        String content = match.getContent();
        if (content == null || content.trim().isEmpty()) {
            return "No specific content available";
        }
        
        // Clean and truncate content
        String cleaned = content
            .replaceAll("\\s+", " ")
            .trim();
        
        // Truncate if too long, but keep it meaningful
        if (cleaned.length() > 800) {
            cleaned = cleaned.substring(0, 800) + "...";
        }
        
        return cleaned;
    }
    
    /**
     * Extracts structured information in a simplified format
     */
    private ObjectNode extractStructuredInfo(JsonNode contentJson, String userQuery) {
        ObjectNode structuredInfo = objectMapper.createObjectNode();
        
        // Extract title
        if (contentJson.has("title")) {
            structuredInfo.put("pageTitle", contentJson.get("title").asText());
        }
        
        // Extract key sections
        if (contentJson.has("sections") && contentJson.get("sections").isArray()) {
            ArrayNode keyPoints = objectMapper.createArrayNode();
            String lowerQuery = userQuery.toLowerCase();
            
            for (JsonNode section : contentJson.get("sections")) {
                if (section.has("heading") && section.has("paragraphs")) {
                    String heading = section.get("heading").asText();
                    
                    // Check if this section is relevant to the query
                    if (isRelevantSection(heading.toLowerCase(), lowerQuery)) {
                        ObjectNode sectionInfo = objectMapper.createObjectNode();
                        sectionInfo.put("heading", heading);
                        
                        // Combine paragraphs into a single text
                        StringBuilder paragraphText = new StringBuilder();
                        JsonNode paragraphs = section.get("paragraphs");
                        if (paragraphs.isArray()) {
                            for (JsonNode paragraph : paragraphs) {
                                paragraphText.append(paragraph.asText()).append(" ");
                            }
                        }
                        sectionInfo.put("content", paragraphText.toString().trim());
                        
                        // Add lists if available
                        if (section.has("list") && section.get("list").isArray()) {
                            ArrayNode listItems = objectMapper.createArrayNode();
                            for (JsonNode listItem : section.get("list")) {
                                listItems.add(listItem.asText());
                            }
                            sectionInfo.set("listItems", listItems);
                        }
                        
                        keyPoints.add(sectionInfo);
                    }
                }
            }
            
            if (keyPoints.size() > 0) {
                structuredInfo.set("keyInformation", keyPoints);
            }
        }
        
        // Extract tables in a simplified format with header handling
        if (contentJson.has("tables") && contentJson.get("tables").isArray()) {
            ArrayNode simplifiedTables = objectMapper.createArrayNode();
            for (JsonNode table : contentJson.get("tables")) {
                ObjectNode tableInfo = objectMapper.createObjectNode();
                
                // Check if headers exist and are not empty
                boolean hasHeaders = table.has("headers") && 
                                   table.get("headers").isArray() && 
                                   table.get("headers").size() > 0;
                
                if (table.has("rows") && table.get("rows").isArray()) {
                    JsonNode rows = table.get("rows");
                    
                    if (hasHeaders) {
                        // Use existing headers
                        ArrayNode headers = objectMapper.createArrayNode();
                        for (JsonNode header : table.get("headers")) {
                            headers.add(header.asText());
                        }
                        tableInfo.set("headers", headers);
                        
                        // Add all rows as data
                        ArrayNode rowsData = objectMapper.createArrayNode();
                        for (JsonNode row : rows) {
                            if (row.isArray()) {
                                ArrayNode rowData = objectMapper.createArrayNode();
                                for (JsonNode cell : row) {
                                    rowData.add(cell.asText());
                                }
                                rowsData.add(rowData);
                            }
                        }
                        tableInfo.set("rows", rowsData);
                    } else if (rows.size() > 0) {
                        // Headers are null/empty, use first row as headers
                        JsonNode firstRow = rows.get(0);
                        if (firstRow.isArray()) {
                            ArrayNode headers = objectMapper.createArrayNode();
                            for (JsonNode header : firstRow) {
                                headers.add(header.asText());
                            }
                            tableInfo.set("headers", headers);
                            
                            // Add remaining rows as data (skip first row since it's headers)
                            ArrayNode rowsData = objectMapper.createArrayNode();
                            for (int i = 1; i < rows.size(); i++) {
                                JsonNode row = rows.get(i);
                                if (row.isArray()) {
                                    ArrayNode rowData = objectMapper.createArrayNode();
                                    for (JsonNode cell : row) {
                                        rowData.add(cell.asText());
                                    }
                                    rowsData.add(rowData);
                                }
                            }
                            tableInfo.set("rows", rowsData);
                        }
                    }
                }
                
                // Only add table if it has meaningful data
                if (tableInfo.has("headers") && tableInfo.has("rows")) {
                    simplifiedTables.add(tableInfo);
                }
            }
            
            if (simplifiedTables.size() > 0) {
                structuredInfo.set("tables", simplifiedTables);
            }
        }
        
        return structuredInfo;
    }
} 