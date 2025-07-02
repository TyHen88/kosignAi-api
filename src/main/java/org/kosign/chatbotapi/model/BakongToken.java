package org.kosign.chatbotapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Simplified entity for storing Bakong API tokens
 */
@Entity
@Table(name = "bakong_tokens", indexes = {
    @Index(name = "idx_bakong_tokens_service_email", columnList = "service_name, email"),
    @Index(name = "idx_bakong_tokens_expires", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BakongToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Service identifier (e.g., "transaction_service")
     */
    @Column(name = "service_name", nullable = false, length = 100)
    @Builder.Default
    private String serviceName = "transaction_service";
    
    /**
     * Email used for token generation
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    /**
     * Access token from Bakong API
     */
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;
    
    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Record creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Record last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is expired with buffer time (minutes)
     */
    public boolean isExpiredWithBuffer(int bufferMinutes) {
        return expiresAt != null && LocalDateTime.now().plusMinutes(bufferMinutes).isAfter(expiresAt);
    }
    
    /**
     * Check if token is valid (not expired)
     */
    public boolean isValid() {
        return !isExpired();
    }
    
    /**
     * Check if token is valid with buffer
     */
    public boolean isValidWithBuffer(int bufferMinutes) {
        return !isExpiredWithBuffer(bufferMinutes);
    }
} 