package org.kosign.chatbotapi.domains.openAPITools;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kosign.chatbotapi.converter.CryptoConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "api_requests")
@Setter
@Getter
@NoArgsConstructor
public class ApiRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String keywords;
    private String description;

    @Column(nullable = false)
    private String authType; // "NONE", "BASIC", "BEARER", "API_KEY"

    @Column
    private String authKey; // For API_KEY type

//    @Convert(converter = CryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String authValue;

    @Column(nullable = false, unique = true)
    private String url;

    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @OneToMany(mappedBy = "apiRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestHeader> headers = new ArrayList<>();

    @OneToMany(mappedBy = "apiRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestParam> params = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String body;

    @ElementCollection
    @CollectionTable(name = "required_parameters", joinColumns = @JoinColumn(name = "api_request_id"))
    @Column(name = "parameter_name")
    private Set<String> requiredParameters = new HashSet<>();

    private Long workflowId; // Optional, if this request is part of a workflow

    @OneToMany(mappedBy = "apiRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiResponse> responses = new ArrayList<>();



    @Builder
    public ApiRequest(Long id, String name, String keywords, String authType, String authKey, String authValue, String description, String url, HttpMethod method, List<RequestHeader> headers, List<RequestParam> params, String body, Set<String> requiredParameters, Long workflowId, List<ApiResponse> responses) {
        this.id = id;
        this.name = name;
        this.keywords = keywords;
        this.authType = authType != null ? authType : "NONE"; // Default to NONE if not provided
        this.authKey = authKey;
        this.authValue = authValue;
        this.description = description;
        this.url = url;
        this.method = method;
        this.headers = headers != null ? headers : new ArrayList<>();
        this.params = params != null ? params : new ArrayList<>();
        this.body = body;
        this.requiredParameters = requiredParameters != null ? requiredParameters : new HashSet<>();
        this.workflowId = workflowId;
        this.responses = responses != null ? responses : new ArrayList<>();
    }
}
