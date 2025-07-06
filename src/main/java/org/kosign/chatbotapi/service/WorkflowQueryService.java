package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.domains.Workflow;
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowQueryService.class);

    @Autowired
    private WorkflowRepository workflowRepository;

    // Keywords that indicate problem-solving or workflow-specific queries
    private static final Set<String> PROBLEM_SOLVING_KEYWORDS = Set.of(
        "error", "failed", "problem", "issue", "trouble", "stuck", "pending", "declined", "rejected",
        "not working", "can't", "cannot", "unable", "won't", "doesn't work", "broken",
        "payment failed", "transaction error", "transfer problem", "money not received",
        "payment stuck", "transaction declined", "payment rejected", "card declined",
        "insufficient funds", "payment timeout", "transaction timeout",
        "account locked", "account blocked", "account suspended", "login problem", "access denied",
        "password reset", "forgotten password", "account recovery", "can't login", "locked out",
        "card blocked", "card not working", "atm problem", "pin blocked", "card expired",
        "card lost", "card stolen", "card damaged",
        "service unavailable", "system down", "maintenance", "outage", "slow response",
        "app not working", "website down", "mobile banking issue",
        "help", "solve", "fix", "resolve", "support", "assistance", "complaint",
        "why", "what happened", "what's wrong", "how to fix", "solution", "troubleshoot",
        // Enhanced payment transaction keywords
        "payment transaction", "payment issue", "payment problem", "payment not working",
        "transaction payment", "transfer payment", "money transfer", "payment gateway",
        "payment status", "payment pending", "bakong payment", "khqr payment", 
        "mobile payment", "online payment", "payment not received", "payment missing", 
        "payment lost", "payment delay", "payment processing", "payment confirmation"
    );

    // Enhanced workflow category classification with payment focus
    private static final Map<String, String> CATEGORY_MAPPING = createCategoryMapping();
    
    private static Map<String, String> createCategoryMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("transaction", "Transaction Issues");
        mapping.put("payment", "Payment Issues");
        mapping.put("transfer", "Transfer Issues");
        mapping.put("money", "Money Transfer Issues");
        mapping.put("bakong", "Bakong Payment Issues");
        mapping.put("khqr", "KHQR Payment Issues");
        mapping.put("mobile", "Mobile Payment Issues");
        mapping.put("online", "Online Payment Issues");
        mapping.put("account", "Account Issues");
        mapping.put("card", "Card Issues");
        mapping.put("login", "Login Issues");
        mapping.put("technical", "Technical Issues");
        mapping.put("service", "Service Issues");
        mapping.put("general", "General Issues");
        return Collections.unmodifiableMap(mapping);
    }

    /**
     * Check if the query likely requires workflow (problem-solving) resolution.
     */
    public boolean isLikelyWorkflowIntent(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) return false;

        String normalized = userQuery.toLowerCase();
        for (String keyword : PROBLEM_SOLVING_KEYWORDS) {
            if (normalized.contains(keyword)) {
                logger.debug("🔍 Detected workflow keyword match: '{}'", keyword);
                return true;
            }
        }
        return false;
    }

}
