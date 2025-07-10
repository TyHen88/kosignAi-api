package org.kosign.chatbotapi.service;

import org.kosign.chatbotapi.entity.SearchConfig;
import org.kosign.chatbotapi.repository.SearchConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SearchConfigService.class);

    @Autowired
    private SearchConfigRepository repository;

    @Cacheable(value = "search-config", key = "'config'")
    public SearchConfig getConfig() {
        return repository.findById(1L).orElseGet(() -> {
            logger.info("🔧 Creating default search configuration (DB-only: true)");
            SearchConfig config = new SearchConfig();
            config.setDbOnly(true);
            config.setUpdatedBy("SYSTEM_INIT");
            return repository.save(config);
        });
    }

    @Transactional
    @CacheEvict(value = "search-config", key = "'config'")
    public void updateConfig(boolean dbOnly, String updatedBy) {
        SearchConfig config = getConfig();
        boolean wasDbOnly = config.isDbOnly();
        
        config.setDbOnly(dbOnly);
        config.setUpdatedBy(updatedBy != null ? updatedBy : "SYSTEM");
        repository.save(config);
        
        logger.info("🔄 Search configuration updated: {} -> {} (by: {})", 
            wasDbOnly ? "DB-only" : "Hybrid", 
            dbOnly ? "DB-only" : "Hybrid", 
            config.getUpdatedBy());
    }

    @Transactional
    @CacheEvict(value = "search-config", key = "'config'")
    public void updateConfig(boolean dbOnly) {
        updateConfig(dbOnly, "ADMIN");
    }

    /**
     * Quick check method for the AI service
     */
    public boolean isDbOnlyMode() {
        return getConfig().isDbOnly();
    }
} 