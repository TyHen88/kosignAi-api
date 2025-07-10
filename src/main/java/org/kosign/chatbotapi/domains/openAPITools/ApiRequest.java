package org.kosign.chatbotapi.domains.openAPITools;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    private String description;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @OneToMany(mappedBy = "apiRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestHeader> headers = new ArrayList<>();

    @OneToMany(mappedBy = "apiRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestParam> params = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String body;


    @Builder
    public ApiRequest(Long id, String name, String description, String url, HttpMethod method, List<RequestHeader> headers, List<RequestParam> params, String body) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.url = url;
        this.method = method;
        this.headers = headers != null ? headers : new ArrayList<>();
        this.params = params != null ? params : new ArrayList<>();
        this.body = body;
    }
}
