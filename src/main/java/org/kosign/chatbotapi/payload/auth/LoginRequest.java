package org.kosign.chatbotapi.payload.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {
    @JsonProperty("user_name")
    private String username;

    @JsonProperty("password")
    private String password;
}
