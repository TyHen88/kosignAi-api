package org.kosign.chatbotapi.repository;

import java.util.List;

import org.kosign.chatbotapi.domains.Workflow;
import org.kosign.chatbotapi.payload.workflow.IGetWorkflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowRepository extends JpaRepository<Workflow, Long>{
//
//    @Query("""
//            SELECT w FROM Workflow w
//            WHERE w.status = '1'
//            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//                OR LOWER(w.goalStatement) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//            ORDER BY w.createdAt DESC
//            """)
//    List<Workflow> findAllActiveWorkflows(@Param("titleKeyword") String titleKeyword);
//
//    @Query("""
//            SELECT w FROM Workflow w
//            WHERE w.status = '1'
//            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//                OR LOWER(w.goalStatement) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//            ORDER BY w.createdAt DESC
//            """)
//    List<Workflow> findAllActiveWorkflowsByTitle(@Param("titleKeyword") String titleKeyword);

    @Query(
            value = """
            SELECT w.id,
                   w.created_at,
                   w.updated_at,
                   w.category,
                   w.goal_statement,
                   w.image_url,
                   w.sts,
                   w.title
            FROM tb_workflow w
            WHERE w.sts = '1'
              AND (
                :searchValue IS NULL OR :searchValue = '' OR
                unaccent(lower(w.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                OR unaccent(lower(w.category)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
              )
            ORDER BY w.updated_at DESC
        """,
            countQuery = """
            SELECT COUNT(*)
            FROM tb_workflow w
            WHERE w.sts = '1'
              AND (
                :searchValue IS NULL OR :searchValue = '' OR
                unaccent(lower(w.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                OR unaccent(lower(w.category)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
              )
        """,
            nativeQuery = true
    )
    Page<IGetWorkflow> findAllActiveWorkflows(@Param("searchValue") String searchValue, Pageable pageable);

}
