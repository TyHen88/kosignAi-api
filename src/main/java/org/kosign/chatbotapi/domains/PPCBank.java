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
@Table(name = "tb_ppc_bank")
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
    private String content;

    private String contentHash;
    private Integer status;
    private Integer depth;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(columnDefinition = "JSONB")
    private String tables;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
