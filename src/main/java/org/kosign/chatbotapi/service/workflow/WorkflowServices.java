package org.kosign.chatbotapi.service.workflow;


import org.kosign.chatbotapi.payload.workflow.WorkflowRequest;
import org.springframework.data.domain.Pageable;

public interface WorkflowServices {
    Object getAlllActiveWorkflows(String searchValue, Pageable pageable) throws Throwable;

    void createWorkflow(WorkflowRequest request) throws Throwable;

    void updateWorkflow(Long id, WorkflowRequest request) throws Throwable;

    void deleteWorkflow(Long id) throws Throwable;
}