package org.kosign.chatbotapi.domains;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kosign.chatbotapi.enums.Status;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tb_workflow", indexes = {
    @Index(name = "idx_workflow_status", columnList = "sts"),
    @Index(name = "idx_workflow_status_created", columnList = "sts, created_at"),
    @Index(name = "idx_workflow_title", columnList = "title")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String imageUrl;

    @Column(name = "goal_statement", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY) // Lazy load large text
    private String goalStatement;

    @Column(name = "sts", nullable = false, length = Types.CHAR)
    @JdbcTypeCode(Types.CHAR)
    @Convert(converter = Status.Converter.class)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 🔹 Dynamic data storage using JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Basic(fetch = FetchType.LAZY) // Lazy load large JSONB metadata
    private Map<String, Object> metadata;
}
