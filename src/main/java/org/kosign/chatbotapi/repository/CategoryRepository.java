package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.Category;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Cacheable(value = "category-active-cache", key = "'all-active'")
    @Query("SELECT c FROM Category c WHERE c.status = '1'")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "100"))
    List<Category> findAllActive();

    @Cacheable(value = "category-workflow-cache", key = "#workflowId")
    Optional<Category> findByWorkflowId(Long workflowId);

}
