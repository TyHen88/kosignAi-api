package org.kosign.chatbotapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_search_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchConfig {
    
    @Id
    private Long id = 1L;  // singleton row
    
    @Column(name = "db_only", nullable = false)
    private boolean dbOnly = true;  // true = DB only, false = hybrid
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.updatedBy == null) {
            this.updatedBy = "SYSTEM";
        }
    }
} 