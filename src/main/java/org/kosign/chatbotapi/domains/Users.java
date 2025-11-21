package org.kosign.chatbotapi.domains;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.kosign.chatbotapi.enums.Role;
import org.kosign.chatbotapi.enums.StatusUser;

import java.sql.Types;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_user", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone"),
        @UniqueConstraint(columnNames = "username")
    },
    indexes = {
        @Index(name = "idx_user_status", columnList = "sts"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username")
    })
@Builder
public class Users extends UpdatableEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String password;

    private String confirmPassword;

    private String avatarUrl;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role; // e.g., ADMIN, USER, etc.

    @Column(name = "sts",nullable = false, length = Types.CHAR)
    @JdbcTypeCode(Types.CHAR)
    @Convert(converter = StatusUser.Converter.class)
    private StatusUser status;

}
