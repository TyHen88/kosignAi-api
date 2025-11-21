package org.kosign.chatbotapi.domains;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_ppc_bank", indexes = {
    @Index(name = "idx_ppc_bank_status", columnList = "status"),
    @Index(name = "idx_ppc_bank_status_updated", columnList = "status, updated_at"),
    @Index(name = "idx_ppc_bank_title", columnList = "title"),
    @Index(name = "idx_ppc_bank_url", columnList = "url")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PPCBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    private String title;

    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY) // Lazy load large content
    private String content;

    @Column(name = "content_hash")
    private String contentHash;
    
    private Integer status;
    private Integer depth;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "content_json", columnDefinition = "JSONB")
    @Basic(fetch = FetchType.LAZY) // Lazy load large JSONB content
    private String contentJson;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
