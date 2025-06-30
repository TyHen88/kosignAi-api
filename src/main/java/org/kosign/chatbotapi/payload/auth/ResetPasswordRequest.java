package org.kosign.chatbotapi.payload.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ResetPasswordRequest(
        @JsonProperty("session_id")
        @NotBlank
        String sessionId,

        @JsonProperty("phonenumber")
        String phonenumber,

        @JsonProperty("password")
        @Length(max = 50)
        String password,

        @JsonProperty("otp_code")
        @NotBlank
        String otpCode

)
{

}
