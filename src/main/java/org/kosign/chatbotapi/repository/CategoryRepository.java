package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c WHERE c.status = '1'")
    List<Category> findAllActive();

    Optional<Category> findByWorkflowId(Long workflowId);

}
