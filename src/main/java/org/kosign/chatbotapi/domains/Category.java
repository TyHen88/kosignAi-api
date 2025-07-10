package org.kosign.chatbotapi.domains;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.kosign.chatbotapi.enums.Status;

import java.sql.Types;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Column(name = "sts", nullable = false, length = Types.CHAR)
    @JdbcTypeCode(Types.CHAR)
    @Convert(converter = Status.Converter.class)
    private Status status;

    private Long workflowId;

    @Builder
    public Category(Long id, String name, Status status, Long workflowId) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.workflowId = workflowId;
    }


}
