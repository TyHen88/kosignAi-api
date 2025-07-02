package org.kosign.chatbotapi.service.bakong;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.kosign.chatbotapi.components.common.api.StatusCode;
import org.kosign.chatbotapi.exception.BusinessException;
import org.kosign.chatbotapi.payload.bakong.RenewTokenRequest;
import org.kosign.chatbotapi.payload.bakong.RenewTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenRenewalServiceImpl implements TokenRenewalService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${bakong.api.base-url}")
    private String bakongBaseUrl;
    
    @Value("${bakong.api.renew-token.endpoint}")
    private String renewTokenEndpoint;
    
    @Value("${bakong.api.renew-token.email}")
    private String configuredEmail;
    
    @Value("${bakong.api.timeout:30000}")
    private long timeout;

    // Performance metrics
    private final AtomicLong tokenRenewalAttempts = new AtomicLong(0);
    private final AtomicLong successfulRenewals = new AtomicLong(0);
    private volatile Duration lastRenewalDuration = Duration.ZERO;
    
    // Optimized HTTP client with connection pooling and monitoring
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(10))
            .callTimeout(Duration.ofSeconds(45))
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> {
                Request request = chain.request();
                long startTime = System.currentTimeMillis();
                Response response = chain.proceed(request);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("🌐 Token renewal HTTP request took {} ms", duration);
                return response;
            })
            .build();
    
    @Override
    public RenewTokenResponse renewToken(RenewTokenRequest request) {
        long attemptNumber = tokenRenewalAttempts.incrementAndGet();
        Instant startTime = Instant.now();
        
        log.info("🔑 Starting token renewal #{} for email: {}", attemptNumber, request.getEmail());
        
        try {
            // Enhanced email validation
            validateEmailRequest(request);
            
            // Perform token renewal with enhanced monitoring
            RenewTokenResponse response = performTokenRenewal(request);
            
            // Track success metrics
            Duration duration = Duration.between(startTime, Instant.now());
            lastRenewalDuration = duration;
            long successCount = successfulRenewals.incrementAndGet();
            
            log.info("✅ Token renewal #{} completed successfully in {} ms (total successes: {})", 
                attemptNumber, duration.toMillis(), successCount);
            
            return response;
            
        } catch (BusinessException e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("💼 Token renewal #{} failed after {} ms: {}", attemptNumber, duration.toMillis(), e.getMessage());
            throw e;
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("💥 Unexpected error during token renewal #{} after {} ms: {}", attemptNumber, duration.toMillis(), e.getMessage(), e);
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔐 **Authentication Service Error** - Unable to renew access credentials due to a technical issue. " +
                "Please try again in a few minutes or contact support if the problem persists.");
        }
    }

    private void validateEmailRequest(RenewTokenRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔐 **Invalid Request** - Email address is required for authentication.");
        }

        String requestEmail = request.getEmail().trim();
        if (!configuredEmail.equals(requestEmail)) {
            log.warn("🚫 Unauthorized email attempted token renewal: {} (expected: {})", requestEmail, configuredEmail);
            throw new BusinessException(StatusCode.UNAUTHORIZED, 
                "🔐 **Access Denied** - This email address is not authorized for token renewal. " +
                "Please contact support if you believe this is an error.");
        }
    }

    private RenewTokenResponse performTokenRenewal(RenewTokenRequest request) throws IOException {
        // Build the API URL
        String apiUrl = bakongBaseUrl + renewTokenEndpoint;
        log.debug("🌐 Calling Bakong token renewal API at: {}", apiUrl);
        
        // Create optimized request body
        String requestBody = objectMapper.writeValueAsString(request);
        RequestBody body = RequestBody.create(
            requestBody, 
            MediaType.get("application/json; charset=utf-8")
        );
        
        // Build enhanced HTTP request
        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "PPC-ChatBot-TokenService/1.0")
                .addHeader("X-Request-ID", "token-" + System.currentTimeMillis())
                .addHeader("X-Request-Source", "chatbot-api")
                .build();
        
        // Execute the request with enhanced error handling
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            int statusCode = response.code();
            log.debug("📡 Token renewal API response status: {}", statusCode);
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("🚫 Bakong token renewal failed with status: {} and body: {}", statusCode, errorBody);
                
                String userMessage = generateUserFriendlyErrorMessage(statusCode, errorBody);
                throw new BusinessException(StatusCode.BAD_REQUEST, userMessage);
            }
            
            // Parse and validate response
            String responseBody = response.body().string();
            log.debug("📋 Token renewal successful response received (length: {})", responseBody.length());
            
            RenewTokenResponse renewTokenResponse = objectMapper.readValue(responseBody, RenewTokenResponse.class);
            
            // Enhanced response validation
            validateTokenResponse(renewTokenResponse);
            
            log.info("🎯 Token renewal successful for email: {}", request.getEmail());
            log.debug("🔑 Access token length: {} characters", renewTokenResponse.getAccessToken().length());
            
            return renewTokenResponse;
        }
    }

    private void validateTokenResponse(RenewTokenResponse response) {
        if (response.getResponseCode() == null || response.getResponseCode() != 0) {
            log.error("🚫 Invalid response code from Bakong token API: {} - {}", 
                response.getResponseCode(), response.getResponseMessage());
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔐 **Authentication Failed** - The payment gateway rejected the token renewal request. " +
                "This may be a temporary issue. Please try again in a few minutes.");
        }
        
        String accessToken = response.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.error("📭 Received empty or null token from Bakong API");
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔐 **Invalid Token Response** - The authentication service returned an invalid token. " +
                "Please try again or contact support if the issue persists.");
        }

        if (accessToken.length() < 10) {
            log.error("🔍 Received suspiciously short token: {} characters", accessToken.length());
            throw new BusinessException(StatusCode.BAD_REQUEST, 
                "🔐 **Invalid Token** - The received authentication token appears to be invalid. " +
                "Please try again.");
        }
    }

    private String generateUserFriendlyErrorMessage(int statusCode, String errorBody) {
        return switch (statusCode) {
            case 400 -> "🔐 **Invalid Credentials** - The authentication request was rejected. Please contact support.";
            case 401 -> "🔐 **Unauthorized** - Access denied for token renewal. Please verify your credentials.";
            case 403 -> "🔐 **Access Forbidden** - This account is not permitted to renew tokens. Contact support.";
            case 404 -> "🔍 **Service Not Found** - The authentication service is temporarily unavailable.";
            case 429 -> "⏱️ **Rate Limited** - Too many authentication attempts. Please wait a moment and try again.";
            case 500, 502, 503 -> "🔧 **Service Maintenance** - The authentication service is temporarily unavailable. Please try again later.";
            case 504 -> "⏱️ **Request Timeout** - The authentication request timed out. Please try again.";
            default -> "🔄 **Authentication Error** - Unable to renew access credentials. Please try again or contact support.";
        };
    }

    // Performance monitoring methods
    public String getServiceMetrics() {
        double successRate = tokenRenewalAttempts.get() > 0 ? 
            (double) successfulRenewals.get() / tokenRenewalAttempts.get() * 100 : 0;
        
        return String.format(
            "Token Service Metrics: attempts=%d, successes=%d, success_rate=%.1f%%, last_duration=%dms",
            tokenRenewalAttempts.get(),
            successfulRenewals.get(),
            successRate,
            lastRenewalDuration.toMillis()
        );
    }

    public boolean isHealthy() {
        // Consider service healthy if success rate is above 80% in recent attempts
        return tokenRenewalAttempts.get() == 0 || 
               (double) successfulRenewals.get() / tokenRenewalAttempts.get() >= 0.8;
    }

    // Graceful shutdown
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Shutting down token renewal service...");
        log.info("📊 Final metrics: {}", getServiceMetrics());
        
        // Close HTTP client resources
        httpClient.dispatcher().executorService().shutdown();
        try {
            if (!httpClient.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                httpClient.dispatcher().executorService().shutdownNow();
            }
        } catch (InterruptedException e) {
            httpClient.dispatcher().executorService().shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        httpClient.connectionPool().evictAll();
        log.info("✅ Token renewal service shutdown complete");
    }
} 