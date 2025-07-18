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
    List<Workflow> findTop5ByTitleContainingIgnoreCaseOrGoalStatementContainingIgnoreCase(String title, String goal);

//    @Query("""
//            SELECT w FROM Workflow w
//            WHERE w.status = '1'
//            AND (LOWER(w.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//                OR LOWER(w.goalStatement) LIKE LOWER(CONCAT('%', :titleKeyword, '%'))
//            ORDER BY w.createdAt DESC
//            """)
//    List<Workflow> findAllActiveWorkflows(@Param("titleKeyword") String titleKeyword);

    @Query(
            value = """
                    SELECT DISTINCT w.metadata
                    FROM tb_workflow w
                             LEFT JOIN tb_category c ON w.id = c.workflow_id
                             LEFT JOIN LATERAL regexp_split_to_table(:searchValue, '\\s+') AS word ON TRUE
                    WHERE w.sts = '1'
                      AND (
                        :searchValue IS NULL OR :searchValue = '' OR
                        unaccent(c.name) ILIKE unaccent(CONCAT('%', word, '%'))
                        )
                    """,
            nativeQuery = true
    )
    List<String> findAllActiveWorkflowsByTitle(@Param("searchValue") String searchValue);

    @Query(
            value = """

                    SELECT
                            w.id AS id,
                            w.created_at AS createdAt,
                            w.updated_at AS updatedAt,
                            c.name AS categoryName,
                            w.goal_statement AS goalStatement,
                            c.id AS categoryId,
                            w.image_url AS imageUrl,
                            w.sts AS status,
                            w.title AS title,
                            w.metadata AS metadata
                        FROM
                            tb_workflow w
                        JOIN
                            tb_category c ON c.workflow_id = w.id
                        WHERE
                            w.sts = '1'
                            AND (
                                :searchValue IS NULL OR :searchValue = '' OR
                                unaccent(w.title) ILIKE unaccent(CONCAT('%', :searchValue, '%')) OR
                                unaccent(c.name) ILIKE unaccent(CONCAT('%', :searchValue, '%'))
                            )
            """,
            countQuery = """
            SELECT COUNT(w.id)
            FROM
                tb_workflow w
            JOIN
                tb_category c ON c.workflow_id = w.id
            WHERE
                w.sts = '1'
                AND (
                    :searchValue IS NULL OR :searchValue = '' OR
                    unaccent(w.title) ILIKE unaccent(CONCAT('%', :searchValue, '%')) OR
                    unaccent(c.name) ILIKE unaccent(CONCAT('%', :searchValue, '%'))
                )
            """,
            nativeQuery = true
    )
    Page<IGetWorkflow> findAllActiveWorkflows(@Param("searchValue") String searchValue, Pageable pageable);


    @Query("""
            SELECT w FROM Workflow w  WHERE w.status = '1'""")
    List<Workflow> findAllByStatus();

}
