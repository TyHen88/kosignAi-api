package org.kosign.chatbotapi.domains;



import java.sql.Types;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.JdbcTypeCode;
import org.kosign.chatbotapi.enums.Status;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String imageUrl;
    private String category;
    @Column(name = "goal_statement", columnDefinition = "TEXT")
    private String goalStatement;

    @Column(name = "sts",nullable = false, length = Types.CHAR)
    @JdbcTypeCode(Types.CHAR)
    @Convert(converter = Status.Converter.class)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}