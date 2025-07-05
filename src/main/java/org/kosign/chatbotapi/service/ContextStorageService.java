package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.model.SessionContext;
import org.kosign.chatbotapi.model.TitleMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing session contexts with automatic cleanup
 */
@Service
public class ContextStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextStorageService.class);
    
    // Configuration constants
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;
    private static final int MAX_SESSIONS = 1000; // Prevent memory issues
    
    // Thread-safe storage for session contexts
    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();
    private final AtomicInteger totalQueries = new AtomicInteger(0);
    private final AtomicInteger contextHits = new AtomicInteger(0);
    
    @PostConstruct
    public void initialize() {
        logger.info("🔧 Context Storage Service initialized with {} minute session timeout", 
                   DEFAULT_SESSION_TIMEOUT_MINUTES);
    }
    
    /**
     * Store context for a session after a database query
     */
    public void storeContext(String sessionId, String query, String normalizedQuery, 
                           String queryIntent, List<TitleMatchResult> matchResults, String jsonContext) {
        try {
            totalQueries.incrementAndGet();
            
            // Get or create session context
            SessionContext context = sessionContexts.computeIfAbsent(sessionId, SessionContext::create);
            
            // Update context with new data
            context.updateContext(query, normalizedQuery, queryIntent, matchResults, jsonContext);
            
            // Add contextual metadata
            context.addMetadata("hasMatches", matchResults != null && !matchResults.isEmpty());
            if (matchResults != null && !matchResults.isEmpty()) {
                context.addMetadata("bestMatchScore", matchResults.get(0).getMatchScore());
                context.addMetadata("bestMatchConfidence", matchResults.get(0).getConfidenceLevel());
            }
            
            logger.debug("📝 Stored context for session {}: query='{}', matches={}", 
                        sessionId, query, matchResults != null ? matchResults.size() : 0);
            
            // Check if we need to limit sessions to prevent memory issues
            if (sessionContexts.size() > MAX_SESSIONS) {
                cleanupOldestSessions();
            }
            
        } catch (Exception e) {
            logger.error("❌ Error storing context for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve context for a session if available and relevant
     */
    public SessionContext getRelevantContext(String sessionId, String currentQuery, String currentQueryIntent) {
        try {
            SessionContext context = sessionContexts.get(sessionId);
            
            if (context == null) {
                logger.debug("🔍 No context found for session {}", sessionId);
                return null;
            }
            
            // Check if context is expired
            if (context.isExpired(DEFAULT_SESSION_TIMEOUT_MINUTES)) {
                logger.debug("⏰ Context expired for session {}, removing", sessionId);
                sessionContexts.remove(sessionId);
                return null;
            }
            
            // Check if context is relevant to current query
            if (!context.isRelevantTo(currentQuery, currentQueryIntent)) {
                logger.debug("🎯 Context not relevant for session {}: '{}' vs '{}'", 
                           sessionId, context.getLastQuery(), currentQuery);
                return null;
            }
            
            // Update access time
            context.touch();
            contextHits.incrementAndGet();
            
            logger.info("✅ Using stored context for session {}: previous='{}', current='{}'", 
                       sessionId, context.getLastQuery(), currentQuery);
            
            return context;
            
        } catch (Exception e) {
            logger.error("❌ Error retrieving context for session {}: {}", sessionId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if session has usable context without retrieving it
     */
    public boolean hasRelevantContext(String sessionId, String currentQuery, String currentQueryIntent) {
        SessionContext context = getRelevantContext(sessionId, currentQuery, currentQueryIntent);
        return context != null && context.hasHighConfidenceMatches();
    }
    
    /**
     * Clear context for a specific session
     */
    public void clearSession(String sessionId) {
        SessionContext removed = sessionContexts.remove(sessionId);
        if (removed != null) {
            logger.debug("🗑️ Cleared context for session {}", sessionId);
        }
    }
    
    /**
     * Get statistics about context usage
     */
    public String getContextStats() {
        int activeSessions = sessionContexts.size();
        int totalQueriesCount = totalQueries.get();
        int contextHitsCount = contextHits.get();
        double hitRate = totalQueriesCount > 0 ? (double) contextHitsCount / totalQueriesCount * 100 : 0;
        
        return String.format(
            "Context Storage Stats: active_sessions=%d, total_queries=%d, context_hits=%d, hit_rate=%.1f%%",
            activeSessions, totalQueriesCount, contextHitsCount, hitRate
        );
    }
    
    /**
     * Scheduled cleanup of expired sessions (every 10 minutes)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredSessions() {
        try {
            int initialSize = sessionContexts.size();
            
            sessionContexts.entrySet().removeIf(entry -> 
                entry.getValue().isExpired(DEFAULT_SESSION_TIMEOUT_MINUTES)
            );
            
            int removedCount = initialSize - sessionContexts.size();
            if (removedCount > 0) {
                logger.info("🧹 Cleaned up {} expired sessions, {} active sessions remaining", 
                           removedCount, sessionContexts.size());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error during session cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Emergency cleanup when session limit is reached
     */
    private void cleanupOldestSessions() {
        try {
            // Remove oldest 20% of sessions to free up space
            int removeCount = Math.max(1, sessionContexts.size() / 5);
            
            sessionContexts.entrySet().stream()
                .sorted((e1, e2) -> e1.getValue().getLastAccessTime()
                    .compareTo(e2.getValue().getLastAccessTime()))
                .limit(removeCount)
                .forEach(entry -> sessionContexts.remove(entry.getKey()));
            
            logger.warn("⚠️ Emergency cleanup: removed {} oldest sessions due to memory limit", removeCount);
            
        } catch (Exception e) {
            logger.error("❌ Error during emergency cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Force cleanup all sessions (for maintenance)
     */
    public void clearAllSessions() {
        int count = sessionContexts.size();
        sessionContexts.clear();
        logger.info("🗑️ Cleared all {} sessions", count);
    }
} 