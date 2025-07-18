package org.kosign.chatbotapi.domains.openAPITools;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {

    List<ApiRequest> findAllByWorkflowId(Long workflowId);

    Optional<ApiRequest> findByName(String name);


}

