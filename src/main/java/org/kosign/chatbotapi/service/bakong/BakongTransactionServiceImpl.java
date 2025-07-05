package org.kosign.chatbotapi.service.bakong;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.kosign.chatbotapi.components.common.api.StatusCode;
import org.kosign.chatbotapi.exception.BusinessException;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionRequest;
import org.kosign.chatbotapi.payload.bakong.BakongTransactionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import javax.naming.AuthenticationException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class BakongTransactionServiceImpl implements BakongTransactionService {

    private final ObjectMapper objectMapper;

    @Value("${bakong.api.base-url}")
    private String bakongBaseUrl;

    @Value("${bakong.api.check-transaction.endpoint}")
    private String checkTransactionEndpoint;

    @Value("${bakong.api.renew-token.email}")
    private String configuredEmail;

    @Value("${bakong.api.timeout:30000}")
    private long timeout;

    @Value("${bakong.api.max-retries:3}")
    private int maxRetries;

    @Value("${bakong.api.token}")
    private String token;

    // Constants
    private static final String SERVICE_NAME = "transaction_service";

    // Optimized HTTP client with connection pooling and better configuration
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(15))
            .callTimeout(Duration.ofSeconds(45))
            .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> {
                Request request = chain.request();
                long startTime = System.currentTimeMillis();
                Response response = chain.proceed(request);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("HTTP request to {} took {} ms", request.url(), duration);
                return response;
            })
            .build();

    // Performance metrics
    private final AtomicLong transactionCheckCount = new AtomicLong(0);
    private final AtomicLong successfulChecks = new AtomicLong(0);
    private final AtomicLong failedChecks = new AtomicLong(0);

    // Async executor for non-blocking operations
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "bakong-async-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    @Override
    public BakongTransactionResponse checkTransactionStatus(BakongTransactionRequest request) {
        // Input validation
        if (request == null || request.getHash() == null || request.getHash().trim().isEmpty()) {
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔍 **Missing Transaction Information** - Please provide a valid transaction hash to check the status.");
        }

        String transactionHash = request.getHash().trim();
        long checkNumber = transactionCheckCount.incrementAndGet();
        log.info("🔍 Checking transaction status #{} for hash: {}", checkNumber, transactionHash);
        
        Instant startTime = Instant.now();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String accessToken = token;
                BakongTransactionResponse response = performApiCall(request, accessToken);
                
                // Log success metrics
                Duration duration = Duration.between(startTime, Instant.now());
                successfulChecks.incrementAndGet();
                log.info("✅ Transaction check #{} completed successfully for hash: {} in {} ms (attempt {})", 
                    checkNumber, transactionHash, duration.toMillis(), attempt);
                
                return response;
                
            } catch (RemoteException e) {
                // Authentication failure (401)
                log.warn("🔐 Authentication failed on attempt {}: {}. Invalidating token and retrying.", attempt, e.getMessage());
                // invalidateToken();
                lastException = e;
                
                if (attempt == maxRetries) {
                    failedChecks.incrementAndGet();
                    log.error("❌ Failed to authenticate with Bakong API after {} attempts.", maxRetries);
                    throw new BusinessException(StatusCode.UNAUTHORIZED, 
                        "🔐 **Authentication Issue** - Unable to verify your transaction at this time. " +
                        "This is a temporary service issue. Please try again in a few minutes or contact support if the problem persists.");
                }
                
                // Progressive backoff between retries
                try {
                    Thread.sleep(Math.min(1000 * attempt, 3000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(StatusCode.BAD_REQUEST, 
                        "🔄 **Service Interrupted** - Transaction check was interrupted. Please try again.");
                }
                
            } catch (BusinessException e) {
                // Don't retry business exceptions (validation errors, etc.)
                failedChecks.incrementAndGet();
                log.error("💼 Business exception during transaction check #{}: {}", checkNumber, e.getMessage());
                throw e;
            } catch (Exception e) {
                // Don't retry other exceptions (network errors, etc.)
                failedChecks.incrementAndGet();
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("💥 Unexpected error during transaction check #{} after {} ms: {}", checkNumber, duration.toMillis(), e.getMessage(), e);
                
                throw new BusinessException(StatusCode.BAD_REQUEST, 
                    "🔄 **Service Temporarily Unavailable** - We're experiencing technical difficulties. " +
                    "Please try again in a few minutes. If the problem continues, contact PPC Bank support.");
            }
        }
        
        // This should not be reached
        failedChecks.incrementAndGet();
        throw new BusinessException(StatusCode.AUTHENTICATION_FAILED, 
            "🔄 **Service Error** - An unexpected issue occurred while checking your transaction. Please contact support.");
    }

    private BakongTransactionResponse performApiCall(BakongTransactionRequest request, String accessToken) 
            throws IOException, AuthenticationException {
        
        String apiUrl = bakongBaseUrl + checkTransactionEndpoint;
        String requestBodyJson = objectMapper.writeValueAsString(request);
        
        RequestBody body = RequestBody.create(requestBodyJson, MediaType.get("application/json; charset=utf-8"));

        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("User-Agent", "PPC-ChatBot/1.0")
                .addHeader("X-Request-ID", String.valueOf(System.currentTimeMillis()))
                .build();

        log.debug("🌐 Executing Bakong API call to: {}", apiUrl);
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            int statusCode = response.code();
            log.debug("📡 Bakong API response status: {}", statusCode);
            
            if (statusCode == 401) {
                throw new RemoteException("Unauthorized: Token may be invalid or expired.");
            }
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("🚫 Bakong API call failed with status: {} and body: {}", statusCode, errorBody);
                
                String userMessage = generateUserFriendlyErrorMessage(statusCode, errorBody);
                throw new BusinessException(StatusCode.BAD_REQUEST, userMessage);
            }

            String responseBody = response.body().string();
            log.debug("📋 Bakong API successful response received (length: {})", responseBody.length());
            
            BakongTransactionResponse transactionResponse = objectMapper.readValue(responseBody, BakongTransactionResponse.class);

            System.err.println("transactionResponse: " + transactionResponse);
            // Enhanced response validation
            validateApiResponse(transactionResponse, request.getHash());
            
            return transactionResponse;
        }
    }

    private void validateApiResponse(BakongTransactionResponse response, String requestHash) {
        if (response.getResponseCode() == null) {
            log.error("🚫 Invalid response code from Bakong API: {}", response.getResponseCode());
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔍 **Transaction Not Found** - The transaction with the provided details could not be located. " +
                "Please verify your transaction hash, amount, and currency are correct.");
        }
        
        if (response.getData() == null) {
            log.error("📭 No transaction data in response from Bakong API");
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔍 **No Transaction Data** - The system couldn't retrieve transaction details. " +
                "Please check your transaction information and try again.");
        }
        
        log.info("✅ Transaction validation successful for hash: {}", requestHash);
    }

    private String generateUserFriendlyErrorMessage(int statusCode, String errorBody) {
        return switch (statusCode) {
            case 400 -> "🔍 **Invalid Request** - Please check your transaction details (hash, amount, currency) and try again.";
            case 403 -> "🔐 **Access Denied** - We don't have permission to access this transaction. Please contact support.";
            case 404 -> "🔍 **Transaction Not Found** - No transaction found with the provided details. Please verify your information.";
            case 429 -> "⏱️ **Service Busy** - Too many requests at the moment. Please wait a moment and try again.";
            case 500, 502, 503 -> "🔧 **Service Maintenance** - The payment system is temporarily unavailable. Please try again later.";
            case 504 -> "⏱️ **Request Timeout** - The request took too long to process. Please try again.";
            default -> "🔄 **Service Issue** - We're experiencing technical difficulties. Please try again or contact support.";
        };
    }


    /**
     * Invalidate current token
     */
    // private void invalidateToken() {
    //     try {
    //         log.debug("🗑️ Invalidating access token");
    //         bakongTokenService.invalidateTokens(SERVICE_NAME, configuredEmail);
    //     } catch (Exception e) {
    //         log.warn("⚠️ Failed to invalidate token: {}", e.getMessage());
    //     }
    // }

    // Performance monitoring
    @Cacheable("transaction-service-metrics")
    public String getServiceMetrics() {
        long totalChecks = transactionCheckCount.get();
        double successRate = totalChecks > 0 ? (double) successfulChecks.get() / totalChecks * 100 : 0;
        
        return String.format(
            "Transaction Service Metrics: checks=%d, successes=%d, failures=%d, success_rate=%.1f%%",
            totalChecks, successfulChecks.get(), failedChecks.get(), successRate
        );
    }

   

    // Graceful shutdown
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Shutting down Bakong transaction service...");
        log.info("📊 Final metrics: {}", getServiceMetrics());
        
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close HTTP client resources
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        
        log.info("✅ Bakong transaction service shutdown complete");
    }
}