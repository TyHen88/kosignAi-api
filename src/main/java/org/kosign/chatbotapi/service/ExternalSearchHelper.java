package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.model.TitleMatchResult;
import org.kosign.chatbotapi.utilAI.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ExternalSearchHelper {

    private static final Logger logger = LoggerFactory.getLogger(ExternalSearchHelper.class);

    private final AIService aiService;

    @Autowired
    public ExternalSearchHelper(AIService aiService) {
        this.aiService = aiService;
    }

    public List<TitleMatchResult> performExternalSearch(String userQuery, int maxResults) {
        logger.debug("🔮 External search called for: {} (max: {})", userQuery, maxResults);

        try {
            PromptBuilder promptBuilder = new PromptBuilder(userQuery);
            promptBuilder.externalSearch(userQuery);
            String prompt = promptBuilder.build();

            var responseAI = aiService.callOpenAIAPI(prompt, userQuery);
            // Convert responseAI into TitleMatchResult (if applicable)
            return new ArrayList<>(); // Placeholder

        } catch (Exception e) {
            logger.error("Error during external search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}