package org.kosign.chatbotapi.payload.openApi;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kosign.chatbotapi.domains.openAPITools.HttpMethod;

@Getter
@Setter
@NoArgsConstructor
public class OpenApiRequest {
    private String name;
    private String keywords;
    private String authType;
    private String authKey;
    private String authValue;
    private String url;
    @Enumerated(EnumType.STRING)
    private HttpMethod method;

    @Builder
    public OpenApiRequest(String name, String keywords, String authType, String authKey, String authValue, String url, HttpMethod method) {
        this.name = name;
        this.keywords = keywords;
        this.authType = authType;
        this.authKey = authKey;
        this.authValue = authValue;
        this.url = url;
        this.method = method;
    }

}
