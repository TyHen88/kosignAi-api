package org.kosign.chatbotapi.service.ApiRequestToolsService;

import org.kosign.chatbotapi.domains.openAPITools.ApiRequest;
import org.kosign.chatbotapi.domains.openAPITools.ApiRequestRepository;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponse;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponseRepository;
import org.kosign.chatbotapi.exception.ResourceNotFoundException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
public class ApiRequestService {
    private final ApiRequestRepository apiRequestRepository;
    private final ApiResponseRepository apiResponseRepository;
    private final RestTemplate restTemplate;

    public ApiRequestService(ApiRequestRepository apiRequestRepository,
                             ApiResponseRepository apiResponseRepository,
                             RestTemplateBuilder restTemplateBuilder) {
        this.apiRequestRepository = apiRequestRepository;
        this.apiResponseRepository = apiResponseRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public List<ApiRequest> getAllRequests() {
        return apiRequestRepository.findAll();
    }

    @Cacheable(value = "apiRequests", key = "#id")
    public ApiRequest getRequestById(Long id) {
        return apiRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    public ApiRequest saveRequest(ApiRequest apiRequest) {
        validateApiRequest(apiRequest);

        // Ensure child entities know their parent
        if (apiRequest.getHeaders() != null) {
            apiRequest.getHeaders().forEach(header -> header.setApiRequest(apiRequest));
        }
        if (apiRequest.getParams() != null) {
            apiRequest.getParams().forEach(param -> param.setApiRequest(apiRequest));
        }
        return apiRequestRepository.save(apiRequest);
    }

    private void validateApiRequest(ApiRequest apiRequest) {
        Objects.requireNonNull(apiRequest, "ApiRequest cannot be null");

        if (!StringUtils.hasText(apiRequest.getUrl())) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        if (apiRequest.getMethod() == null) {
            throw new IllegalArgumentException("HTTP method must be specified");
        }

        // Validate auth configuration
        if (!"NONE".equals(apiRequest.getAuthType())) {
            if (!StringUtils.hasText(apiRequest.getAuthKey())) {
                throw new IllegalArgumentException("Auth key is required for auth type: " + apiRequest.getAuthType());
            }
            if (!StringUtils.hasText(apiRequest.getAuthValue())) {
                throw new IllegalArgumentException("Auth value is required for auth type: " + apiRequest.getAuthType());
            }
        }
    }

    public void deleteRequest(Long id) {
        if (!apiRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Request not found with id: " + id);
        }
        apiRequestRepository.deleteById(id);
    }

    public ApiResponse executeRequest(Long requestId) {
        ApiRequest request = getRequestById(requestId);
        return executeRequest(request);
    }

    public ApiResponse executeRequest(ApiRequest request) {
        try {
            HttpHeaders headers = prepareHeaders(request);
            String finalUrl = prepareUrl(request);
            HttpEntity<String> entity = prepareEntity(request, headers);

            ResponseEntity<String> response = executeHttpRequest(request, finalUrl, entity);
            return saveSuccessfulResponse(request, response);

        } catch (Exception e) {
            return saveErrorResponse(request, e);
        }
    }

    private HttpHeaders prepareHeaders(ApiRequest request) {
        HttpHeaders headers = new HttpHeaders();

        // Handle authentication
        switch (request.getAuthType()) {
            case "BASIC":
                String basicAuth = "Basic " + Base64.getEncoder()
                        .encodeToString((request.getAuthKey() + ":" + request.getAuthValue()).getBytes());
                headers.set("Authorization", basicAuth);
                break;
            case "BEARER":
                headers.setBearerAuth(request.getAuthValue());
                break;
            case "API_KEY":
                headers.set(request.getAuthKey(), request.getAuthValue());
                break;
            case "NONE":
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type: " + request.getAuthType());
        }

        // Add custom headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(header ->
                    headers.add(header.getKey(), header.getValue()));
        }

        return headers;
    }

    private String prepareUrl(ApiRequest request) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(request.getUrl());
        if (request.getParams() != null) {
            request.getParams().forEach(param ->
                    uriBuilder.queryParam(param.getKey(), param.getValue()));
        }
        return uriBuilder.toUriString();
    }

    private HttpEntity<String> prepareEntity(ApiRequest request, HttpHeaders headers) {
        return new HttpEntity<>(request.getBody(), headers);
    }

    private ResponseEntity<String> executeHttpRequest(ApiRequest request, String url, HttpEntity<String> entity) {
        return restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.valueOf(request.getMethod().name()),
                entity,
                String.class
        );
    }

    private ApiResponse saveSuccessfulResponse(ApiRequest request, ResponseEntity<String> response) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setApiRequest(request);
        apiResponse.setStatusCode(response.getStatusCode().value());
        apiResponse.setHeaders(response.getHeaders().toString());
        apiResponse.setBody(response.getBody());
        apiResponse.setTimestamp(LocalDateTime.now());

        return apiResponseRepository.save(apiResponse);
    }

    private ApiResponse saveErrorResponse(ApiRequest request, Exception e) {
        ApiResponse errorResponse = new ApiResponse();
        errorResponse.setApiRequest(request);
        errorResponse.setStatusCode(500);
        errorResponse.setHeaders("{}");
        errorResponse.setBody("Error executing request: " + e.getMessage());
        errorResponse.setTimestamp(LocalDateTime.now());

        return apiResponseRepository.save(errorResponse);
    }

    public List<ApiResponse> getRequestHistory(Long requestId) {
        if (!apiRequestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException("Request not found with id: " + requestId);
        }
        return apiResponseRepository.findByApiRequestId(requestId);
    }
}