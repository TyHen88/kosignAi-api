package org.kosign.chatbotapi.domains.openAPITools;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {

    List<ApiRequest> findAllByWorkflowId(Long workflowId);

    List<ApiRequest> findByUrl(String url);

    @Query("SELECT ar FROM ApiRequest ar WHERE ar.keywords LIKE %:keywords%")
    List<ApiRequest> findByKeywords(@Param("keywords") String keywords);


}

