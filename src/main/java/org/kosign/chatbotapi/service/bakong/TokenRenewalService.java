package org.kosign.chatbotapi.service.bakong;

import org.kosign.chatbotapi.payload.bakong.RenewTokenRequest;
import org.kosign.chatbotapi.payload.bakong.RenewTokenResponse;

public interface TokenRenewalService {
    RenewTokenResponse renewToken(RenewTokenRequest request);
} 