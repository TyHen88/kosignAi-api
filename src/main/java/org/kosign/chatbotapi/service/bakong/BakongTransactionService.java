package org.kosign.chatbotapi.service.bakong;

import org.kosign.chatbotapi.payload.bakong.BakongTransactionRequest;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;

public interface BakongTransactionService {
    BakongTransactionResponse checkTransactionStatus(BakongTransactionRequest request);
}