package org.kosign.chatbotapi.service.ApiRequestToolsService;

import org.kosign.chatbotapi.domains.openAPITools.ApiRequest;
import org.kosign.chatbotapi.domains.openAPITools.ApiRequestRepository;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponse;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponseRepository;
import org.kosign.chatbotapi.exception.ResourceNotFoundException;
import org.springdoc.api.OpenApiResourceNotFoundException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

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
        this.restTemplate = restTemplateBuilder.build();
    }

    public List<ApiRequest> getAllRequests() {
        return apiRequestRepository.findAll();
    }

    public ApiRequest getRequestById(Long id) {
        return apiRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + id));
    }

    public ApiRequest saveRequest(ApiRequest apiRequest) {
        // Ensure child entities know their parent
        if (apiRequest.getHeaders() != null) {
            apiRequest.getHeaders().forEach(header -> header.setApiRequest(apiRequest));
        }
        if (apiRequest.getParams() != null) {
            apiRequest.getParams().forEach(param -> param.setApiRequest(apiRequest));
        }
        return apiRequestRepository.save(apiRequest);
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
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(header ->
                        headers.add(header.getKey(), header.getValue()));
            }

            // Prepare URI with query parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(request.getUrl());
            if (request.getParams() != null) {
                request.getParams().forEach(param ->
                        uriBuilder.queryParam(param.getKey(), param.getValue()));
            }

            // Create request entity
            HttpEntity<String> entity = new HttpEntity<>(
                    request.getBody(),
                    headers
            );

            // Execute request
            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    org.springframework.http.HttpMethod.valueOf(request.getMethod().name()),
                    entity,
                    String.class
            );

            // Save response
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setApiRequest(request);
            apiResponse.setStatusCode(response.getStatusCode().value());
            apiResponse.setHeaders(response.getHeaders().toString());
            apiResponse.setBody(response.getBody());
            apiResponse.setTimestamp(LocalDateTime.now());

            return apiResponseRepository.save(apiResponse);
        } catch (Exception e) {
            // Save error response
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setApiRequest(request);
            errorResponse.setStatusCode(500);
            errorResponse.setHeaders("{}");
            errorResponse.setBody("Error executing request: " + e.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());

            return apiResponseRepository.save(errorResponse);
        }
    }

    public List<ApiResponse> getRequestHistory(Long requestId) {
        if (!apiRequestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException("Request not found with id: " + requestId);
        }
        return apiResponseRepository.findByApiRequestId(requestId);
    }
}