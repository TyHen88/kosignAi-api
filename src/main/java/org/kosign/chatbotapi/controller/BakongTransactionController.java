package org.kosign.chatbotapi.controller;

import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionRequest;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.kosign.chatbotapi.payload.bakong.RenewTokenRequest;
import org.kosign.chatbotapi.payload.bakong.RenewTokenResponse;
import org.kosign.chatbotapi.service.bakong.BakongTransactionService;
import org.kosign.chatbotapi.service.bakong.TokenRenewalService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1")
public class BakongTransactionController extends ChatAIRestController {

    private final BakongTransactionService bakongTransactionService;
    private final TokenRenewalService tokenRenewalService;
    private final org.kosign.chatbotapi.service.TransactionAIService transactionAIService;

    @PostMapping("/check_transaction_by_short_hash")
    public ResponseEntity<?> checkTransactionStatus(@RequestHeader("Authorization") String authorization,
            @RequestHeader("Content-Type") String contentType,
            @Valid @RequestBody BakongTransactionRequest request) {
        if (!"application/json".equals(contentType)) {
            throw new IllegalArgumentException("Content-Type must be application/json");
        }

        // Validate authorization (simplified - in real app, implement proper JWT
        // validation)
        if (!authorization.startsWith("Bearer ")) {
            throw new SecurityException("Invalid authorization header");
        }

        BakongTransactionResponse response = bakongTransactionService.checkTransactionStatus(request);

        return ok(response);
    }

    @PostMapping("/renew_token")
    public ResponseEntity<?> renewToken(@Valid @RequestBody RenewTokenRequest request) {
        log.info("Token renewal requested for email: {}", request.getEmail());

        RenewTokenResponse response = tokenRenewalService.renewToken(request);

        log.info("Token renewal successful for email: {}", request.getEmail());
        return ok(response);
    }

    /**
     * Test endpoint to check if the internal transaction AI service is working
     */
    @PostMapping("/test_transaction_ai")
    public ResponseEntity<?> testTransactionAI(@RequestBody Map<String, String> request) {
        try {
            String hash = request.get("hash");
            String amount = request.get("amount");
            String currency = request.get("currency");

            System.err.println("=== TEST ENDPOINT CALLED ===");
            System.err.println(
                    "Testing transaction AI with: hash=" + hash + ", amount=" + amount + ", currency=" + currency);

            BakongTransactionResponse result = transactionAIService.checkTransactionStatus(hash, amount, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);

            return ok(response);

        } catch (Exception e) {
            System.err.println("Test endpoint error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Test endpoint to directly call BakongTransactionService
     */
    @PostMapping("/test_direct_api")
    public ResponseEntity<?> testDirectAPI(@Valid @RequestBody BakongTransactionRequest request) {
        try {
            System.err.println("=== DIRECT API TEST ENDPOINT CALLED ===");

            BakongTransactionResponse response = bakongTransactionService.checkTransactionStatus(request);

            return ok(response);

        } catch (Exception e) {
            System.err.println("Direct API test error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
