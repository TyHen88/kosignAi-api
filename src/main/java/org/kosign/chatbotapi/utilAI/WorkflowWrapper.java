package org.kosign.chatbotapi.utilAI;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.kosign.chatbotapi.payload.workflow.WorkflowData;

import java.util.HashMap;
import java.util.Map;

public class WorkflowWrapper {
    private Map<String, WorkflowData> workflows;

    @JsonAnySetter
    public void setWorkflows(String key, WorkflowData value) {
        if (workflows == null) {
            workflows = new HashMap<>();
        }
        workflows.put(key, value);
    }

    public Map<String, WorkflowData> getWorkflows() {
        return workflows;
    }
}
