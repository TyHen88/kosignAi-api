package org.kosign.chatbotapi.domains.openAPITools;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_params")
@Getter
@Setter
@NoArgsConstructor
public class RequestParam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String key;
    private String value;

    @ManyToOne
    @JoinColumn(name = "api_request_id")
    private ApiRequest apiRequest;

    @Builder
    public RequestParam(Long id, String key, String value, ApiRequest apiRequest) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.apiRequest = apiRequest;
    }

}

