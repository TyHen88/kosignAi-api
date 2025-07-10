package org.kosign.chatbotapi.controller;


import org.kosign.chatbotapi.domains.openAPITools.ApiRequest;
import org.kosign.chatbotapi.domains.openAPITools.ApiResponse;
import org.kosign.chatbotapi.service.ApiRequestToolsService.ApiRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ApiRequestController {
    private final ApiRequestService apiRequestService;
    
    public ApiRequestController(ApiRequestService apiRequestService) {
        this.apiRequestService = apiRequestService;
    }
    
    @GetMapping
    public ResponseEntity<List<ApiRequest>> getAllRequests() {
        return ResponseEntity.ok(apiRequestService.getAllRequests());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiRequest> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(apiRequestService.getRequestById(id));
    }
    
    @PostMapping
    public ResponseEntity<ApiRequest> createRequest(@RequestBody ApiRequest apiRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiRequestService.saveRequest(apiRequest));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiRequest> updateRequest(
            @PathVariable Long id, 
            @RequestBody ApiRequest apiRequest) {
        apiRequest.setId(id);
        return ResponseEntity.ok(apiRequestService.saveRequest(apiRequest));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        apiRequestService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse> executeRequest(@PathVariable Long id) {
        return ResponseEntity.ok(apiRequestService.executeRequest(id));
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ApiResponse>> getRequestHistory(@PathVariable Long id) {
        return ResponseEntity.ok(apiRequestService.getRequestHistory(id));
    }
}