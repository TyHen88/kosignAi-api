package org.kosign.chatbotapi.domains.openAPITools;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {
    Optional<ApiRequest> findByKeywords(String keywords);
    List<ApiRequest> findByNameContainingIgnoreCaseOrKeywordsContainingIgnoreCase(String name, String keywords);
}

