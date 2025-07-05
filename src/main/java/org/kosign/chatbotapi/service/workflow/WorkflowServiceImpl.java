package org.kosign.chatbotapi.service.workflow;


import org.kosign.chatbotapi.components.common.Pagination;
import org.kosign.chatbotapi.domains.Workflow;
import org.kosign.chatbotapi.enums.Status;
import org.kosign.chatbotapi.payload.MainResponse;
import org.kosign.chatbotapi.payload.workflow.WorkflowRequest;
import org.kosign.chatbotapi.payload.workflow.WorkflowResponse;
import org.kosign.chatbotapi.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowServices {
    private final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
    private final WorkflowRepository workflowRepository;

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
                        .category(workflow.getCategory())
                        .build()
        ).toList();

        return MainResponse.builder()
                .data(workflowResponses)
                .pagination(new Pagination(response))
                .build();
    }


    @Override
    @Transactional
    public void createWorkflow(WorkflowRequest request) throws Throwable {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new RuntimeException("Title is required");
        }

        if (request.getCategory() == null || request.getCategory().isEmpty()) {
            throw new RuntimeException("Category is required");
        }

        if (request.getGoalStatement() == null || request.getGoalStatement().isEmpty()) {
            throw new RuntimeException("Goal statement is required");
        }
        
        Workflow workflow = Workflow.builder()
            .title(request.getTitle())
            .imageUrl(request.getImageUrl())
            .category(request.getCategory())
            .goalStatement(request.getGoalStatement())
            .status(Status.ACTIVE)
            .build();
        workflowRepository.save(workflow);
    }

    @Override
    @Transactional
    public void updateWorkflow(Long id, WorkflowRequest request) throws Throwable {
        Workflow workflow = workflowRepository.findById(id).orElseThrow(() -> new RuntimeException("Workflow not found"));
        
        workflow.setTitle(request.getTitle());
        workflow.setImageUrl(request.getImageUrl());
        workflow.setCategory(request.getCategory());
        workflow.setGoalStatement(request.getGoalStatement());
        workflow.setStatus(Status.ACTIVE);
        workflowRepository.save(workflow);

    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id) throws Throwable {
        Workflow workflow = workflowRepository.findById(id).orElseThrow(() -> new RuntimeException("Workflow not found"));
        workflow.setStatus(Status.DELETED);
        workflowRepository.save(workflow);
    }

}
