package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.entity.SearchConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchConfigRepository extends JpaRepository<SearchConfig, Long> {
} 