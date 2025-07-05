package org.kosign.chatbotapi.controller;

import org.kosign.chatbotapi.components.common.api.ChatAIRestController;
import org.kosign.chatbotapi.payload.MultiSortBuilder;
import org.kosign.chatbotapi.payload.workflow.WorkflowRequest;
import org.kosign.chatbotapi.service.workflow.WorkflowServices;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController extends ChatAIRestController {
    private final WorkflowServices workflowServices;

    @PostMapping
    public Object createWorkflow(@RequestBody WorkflowRequest request) throws Throwable {
        workflowServices.createWorkflow(request);
        return ok();
    }

    @PutMapping("/{id}")
    public Object updateWorkflow(@PathVariable Long id, @RequestBody WorkflowRequest request) throws Throwable {
        workflowServices.updateWorkflow(id, request);
        return ok();
    }

    @DeleteMapping("/{id}")
    public Object deleteWorkflow(@PathVariable Long id) throws Throwable {
        workflowServices.deleteWorkflow(id);
        return ok();
    }

    @GetMapping
    public Object getAllWorkflows(
        @RequestParam(name = "search_value", required = false) String searchValue,
        @RequestParam(name = "page_number", defaultValue = "0") Integer pageNumber,
        @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize
    ) throws Throwable{
        List<org.springframework.data.domain.Sort.Order> sortBuilder = new MultiSortBuilder().with("updated_at:desc").build();
        Pageable pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(sortBuilder));
        return ok(workflowServices.getAlllActiveWorkflows(searchValue, pageRequest));
    }
}