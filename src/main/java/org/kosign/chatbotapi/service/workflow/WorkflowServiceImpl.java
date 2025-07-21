package org.kosign.chatbotapi.service.workflow;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kosign.chatbotapi.components.common.Pagination;
import org.kosign.chatbotapi.domains.Category;
import org.kosign.chatbotapi.domains.Workflow;
import org.kosign.chatbotapi.domains.openAPITools.ApiRequest;
import org.kosign.chatbotapi.domains.openAPITools.ApiRequestRepository;
import org.kosign.chatbotapi.enums.Status;
import org.kosign.chatbotapi.payload.MainResponse;
import org.kosign.chatbotapi.payload.openApi.OpenApiRequest;
import org.kosign.chatbotapi.payload.workflow.WorkflowRequest;
import org.kosign.chatbotapi.payload.workflow.WorkflowResponse;
import org.kosign.chatbotapi.repository.CategoryRepository;
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowServices {
    private final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final ApiRequestRepository apiRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public Object getAlllActiveWorkflows(String searchValue, Pageable pageable) throws Throwable {
        var response = workflowRepository.findAllActiveWorkflows(searchValue, pageable);

        if (response.isEmpty()) {
            return MainResponse.builder()
                    .data(Collections.emptyList())
                    .pagination(new Pagination(Page.empty(pageable)))
                    .build();
        }

        var workflowResponses = response.map(workflow ->
                WorkflowResponse.builder()
                        .id(workflow.getId())
                        .title(workflow.getTitle())
                        .goalStatement(workflow.getGoalStatement())
                        .imageUrl(workflow.getImageUrl())
                        .createdAt(workflow.getCreatedAt())
                        .updatedAt(workflow.getUpdatedAt())
                        .categoryId(workflow.getCategoryId())
                        .categoryName(workflow.getCategoryName())
                        .metadata(parseBrokenMetadataString(workflow.getMetadata()).toString())
                        .build()
        ).toList();

        return MainResponse.builder()
                .data(workflowResponses)
                .pagination(new Pagination(response))
                .build();
    }

    private Map<String, Map<String, Object>> parseBrokenMetadataString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;

        String fixed = raw
                .replaceAll("([{,]\\s*)([a-zA-Z0-9_]+)(=)", "$1\"$2\":")       // step1= → "step1":
                .replaceAll("=\\{", ":{")                                     // = { → :{
                .replaceAll("([a-zA-Z0-9_]+)=([^,\\}]+)", "\"$1\":\"$2\"")    // key=value → "key":"value"
                .replace("=", ":")                                            // Fallback cleanup
                .replaceAll(",\\s*}", "}");                                   // Remove trailing comma



        try {
            return objectMapper.readValue(fixed, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.error("Invalid metadata format:\nOriginal: {}\nFixed: {}", raw, fixed, e);
            throw new IllegalStateException("Failed to convert Map-style metadata to JSON", e);
        }
    }





    @Override
    @Transactional
    public void createWorkflow(WorkflowRequest request) throws Throwable {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new RuntimeException("Title is required");
        }

        if (request.getGoalStatement() == null || request.getGoalStatement().isEmpty()) {
            throw new RuntimeException("Goal statement is required");
        }

        Workflow workflow = Workflow.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .goalStatement(request.getGoalStatement())
                .metadata(request.getMetadata())
                .status(Status.ACTIVE)
                .build();

        Workflow savedWorkflow = workflowRepository.save(workflow);

        // Create and save category
        Category category = Category.builder()
                .name(request.getCategoryName())
                .workflowId(savedWorkflow.getId())
                .status(Status.ACTIVE)
                .build();
        categoryRepository.save(category);

        var openApiRequests = request.getOpenApiRequests();

        if (openApiRequests != null && !openApiRequests.isEmpty()) {
            for (var openApiRequest : openApiRequests) {
                // Check if required fields are non-null and not empty
                if (openApiRequest.getAuthType() != null && !openApiRequest.getAuthType().isEmpty() &&
                        openApiRequest.getUrl() != null && !openApiRequest.getUrl().isEmpty() &&
                        openApiRequest.getMethod() != null &&
                        (openApiRequest.getAuthType().equalsIgnoreCase("NONE") ||
                                (openApiRequest.getAuthKey() != null && !openApiRequest.getAuthKey().isEmpty() &&
                                        openApiRequest.getAuthValue() != null && !openApiRequest.getAuthValue().isEmpty()))) {

                    ApiRequest apiRequest = ApiRequest.builder()
                            .name(openApiRequest.getName())
                            .keywords(openApiRequest.getKeywords())
                            .url(openApiRequest.getUrl())
                            .authType(openApiRequest.getAuthType())
                            .authKey(openApiRequest.getAuthKey())
                            .authValue(openApiRequest.getAuthValue())
                            .method(openApiRequest.getMethod())
                            .workflowId(savedWorkflow.getId())
                            .build();

                    apiRequestRepository.save(apiRequest);
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateWorkflow(Long id, WorkflowRequest request) throws Throwable {
        Workflow workflow = workflowRepository.findById(id).orElseThrow(() -> new RuntimeException("Workflow not found"));

        workflow.setTitle(request.getTitle());
        workflow.setImageUrl(request.getImageUrl());
        workflow.setGoalStatement(request.getGoalStatement());
        workflow.setMetadata(request.getMetadata());
        workflow.setStatus(Status.ACTIVE);
        workflowRepository.save(workflow);

        Category category = categoryRepository.findByWorkflowId(id)
                .orElseThrow(() -> new RuntimeException("Category not found for this workflow"));

        category.setName(request.getCategoryName());
        category.setStatus(Status.ACTIVE);
        categoryRepository.save(category);

        // Remove old API requests
        List<ApiRequest> existingApiRequests = apiRequestRepository.findAllByWorkflowId(id);
        if (!existingApiRequests.isEmpty()) {
            apiRequestRepository.deleteAll(existingApiRequests);
        }

        List<OpenApiRequest> openApiRequests = request.getOpenApiRequests();
        if (openApiRequests != null && !openApiRequests.isEmpty()) {
            for (OpenApiRequest openApiRequest : openApiRequests) {
                // Validate fields (for 'NONE' authType, we ignore authKey and authValue)
                if (openApiRequest.getAuthType() != null && !openApiRequest.getAuthType().isEmpty() &&
                        openApiRequest.getUrl() != null && !openApiRequest.getUrl().isEmpty() &&
                        openApiRequest.getMethod() != null &&
                        (openApiRequest.getAuthType().equalsIgnoreCase("NONE") ||
                                (openApiRequest.getAuthKey() != null && !openApiRequest.getAuthKey().isEmpty() &&
                                        openApiRequest.getAuthValue() != null && !openApiRequest.getAuthValue().isEmpty()))) {

                    ApiRequest apiRequest = ApiRequest.builder()
                            .name(openApiRequest.getName())
                            .keywords(openApiRequest.getKeywords())
                            .url(openApiRequest.getUrl())
                            .authType(openApiRequest.getAuthType())
                            .authKey(openApiRequest.getAuthKey())
                            .authValue(openApiRequest.getAuthValue())
                            .method(openApiRequest.getMethod())
                            .workflowId(id)
                            .build();

                    apiRequestRepository.save(apiRequest);
                }
            }
        }

    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id) throws Throwable {
        Workflow workflow = workflowRepository.findById(id).orElseThrow(() -> new RuntimeException("Workflow not found"));
        workflow.setStatus(Status.DELETED);
        workflowRepository.save(workflow);
    }

}
