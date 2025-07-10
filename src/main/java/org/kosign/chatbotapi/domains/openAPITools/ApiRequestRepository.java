package org.kosign.chatbotapi.domains.openAPITools;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {
    List<ApiRequest> findByNameContainingIgnoreCase(String name);
}

