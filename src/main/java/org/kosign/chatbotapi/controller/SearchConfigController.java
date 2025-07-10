package org.kosign.chatbotapi.controller;

import org.kosign.chatbotapi.entity.SearchConfig;
import org.kosign.chatbotapi.service.SearchConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search-config")
@CrossOrigin(origins = "*")
public class SearchConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SearchConfigController.class);

    @Autowired
    private SearchConfigService configService;

    /**
     * DTO for search config update requests
     */
    public static class SearchConfigUpdateRequest {
        private Boolean dbOnly;
        private String updatedBy;

        // Constructors
        public SearchConfigUpdateRequest() {}

        public SearchConfigUpdateRequest(Boolean dbOnly, String updatedBy) {
            this.dbOnly = dbOnly;
            this.updatedBy = updatedBy;
        }

        // Getters and Setters
        public Boolean getDbOnly() { return dbOnly; }
        public void setDbOnly(Boolean dbOnly) { this.dbOnly = dbOnly; }

        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            SearchConfig config = configService.getConfig();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("config", config);
            response.put("currentMode", config.isDbOnly() ? "Database Only" : "Hybrid Search");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting search config: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get search configuration");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Toggle configuration using URL parameters (original endpoint)
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleConfig(
            @RequestParam boolean dbOnly,
            @RequestParam(required = false) String updatedBy) {
        try {
            configService.updateConfig(dbOnly, updatedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Search configuration updated successfully");
            response.put("newMode", dbOnly ? "Database Only" : "Hybrid Search");
            response.put("dbOnly", dbOnly);
            
            logger.info("✅ Search config updated via API (params): dbOnly={}", dbOnly);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error updating search config: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update search configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Update configuration using JSON body (new endpoint)
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody SearchConfigUpdateRequest request) {
        try {
            // Validate request
            if (request.getDbOnly() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "dbOnly field is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            configService.updateConfig(request.getDbOnly(), request.getUpdatedBy());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Search configuration updated successfully");
            response.put("newMode", request.getDbOnly() ? "Database Only" : "Hybrid Search");
            response.put("dbOnly", request.getDbOnly());
            
            logger.info("✅ Search config updated via API (JSON): dbOnly={}, updatedBy={}", 
                       request.getDbOnly(), request.getUpdatedBy());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error updating search config: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update search configuration: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get current status (simple response)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            SearchConfig config = configService.getConfig();
            
            Map<String, Object> response = new HashMap<>();
            response.put("dbOnly", config.isDbOnly());
            response.put("mode", config.isDbOnly() ? "Database Only" : "Hybrid Search");
            response.put("updatedBy", config.getUpdatedBy());
            response.put("updatedAt", config.getUpdatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting search status: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get search status");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

} 