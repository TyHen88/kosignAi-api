package org.kosign.chatbotapi.domains.openAPITools;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_headers")
@Getter
@NoArgsConstructor
@Setter
public class RequestHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String key;
    private String value;

    @ManyToOne
    @JoinColumn(name = "api_request_id")
    private ApiRequest apiRequest;

    @Builder
    public RequestHeader(Long id, String key, String value, ApiRequest apiRequest) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.apiRequest = apiRequest;
    }
}

