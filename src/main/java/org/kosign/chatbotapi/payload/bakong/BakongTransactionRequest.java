package org.kosign.chatbotapi.payload.bakong;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BakongTransactionRequest {
    private String hash;
    private Number amount;
    private String currency;
}
