package org.kosign.chatbotapi.domains.openAPITools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_responses")
@Setter
@Getter
@NoArgsConstructor
public class ApiResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "api_request_id")
    @JsonIgnore
    private ApiRequest apiRequest;

    private int statusCode;

    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(columnDefinition = "TEXT")
    private String body;

    private LocalDateTime timestamp;

    @Builder
    public ApiResponse(Long id, ApiRequest apiRequest, int statusCode, String headers, String body, LocalDateTime timestamp) {
        this.id = id;
        this.apiRequest = apiRequest;
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.timestamp = timestamp;
    }

}

