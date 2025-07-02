package org.kosign.chatbotapi.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

/**
 * Cache configuration for the application using ConcurrentMapCacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary cache manager for the application
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Set cache names for all the caches used in the application
        cacheManager.setCacheNames(Arrays.asList(
            "transaction-inquiry-cache",
            "transaction-metrics", 
            "transaction-service-metrics",
            "banking-search-cache",
            "domain-analysis-cache",
            "conversation-context-cache",
            "bakong-tokens"
        ));
        
        // Allow dynamic cache creation
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
} 