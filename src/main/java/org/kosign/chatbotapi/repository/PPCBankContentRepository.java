package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.PPCBank;
import org.kosign.chatbotapi.payload.IGetPageContents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PPCBankContentRepository extends JpaRepository<PPCBank, Long> {

    // Search by content containing keywords (case-insensitive)
    @Query("SELECT p FROM PPCBank p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PPCBank> findByContentContainingIgnoreCase(@Param("keyword") String keyword);

    // Search by title containing keywords (case-insensitive)
    @Query("SELECT p FROM PPCBank p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PPCBank> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * Searches title, content, and tables for a keyword.
     * Uses native query for accent-insensitive and case-insensitive matching.
     */
    /**
     * Searches title, content, and tables for a keyword.
     * Uses native query for accent-insensitive and case-insensitive matching.
     */
    @Query(
            value = """
            SELECT * FROM tb_ppc_bank
            WHERE fts_vector @@ websearch_to_tsquery('english', :keyword)
                    """,
            nativeQuery = true
    )
    List<PPCBank> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * Searches title, content, and tables for multiple keywords.
     * Uses native query for accent-insensitive and case-insensitive matching.
     */
    @Query(
            value = """
            SELECT * FROM tb_ppc_bank p WHERE
            (:keyword1 IS NULL OR p.fts_vector @@ websearch_to_tsquery('english', :keyword1)) AND
            (:keyword2 IS NULL OR p.fts_vector @@ websearch_to_tsquery('english', :keyword2)) AND
            (:keyword3 IS NULL OR p.fts_vector @@ websearch_to_tsquery('english', :keyword3))
            """,
            nativeQuery = true
    )
    List<PPCBank> findByMultipleKeywords(
            @Param("keyword1") String keyword1,
            @Param("keyword2") String keyword2,
            @Param("keyword3") String keyword3
    );

    // Get recent pages with pagination
    @Query("SELECT p FROM PPCBank p ORDER BY p.updatedAt DESC")
    Page<PPCBank> findRecentPages(Pageable pageable);

    // Find pages by URL pattern
    @Query("SELECT p FROM PPCBank p WHERE LOWER(p.url) LIKE LOWER(CONCAT('%', :urlPattern, '%'))")
    List<PPCBank> findByUrlContainingIgnoreCase(@Param("urlPattern") String urlPattern);

    // Get page count
    @Query("SELECT COUNT(p) FROM PPCBank p")
    Long getTotalPageCount();

    // Get pages with specific status codes
    List<PPCBank> findByStatus(Integer status);

    @Query(value = """
            SELECT p.id AS id,
            p.url AS url,
            p.title AS title,
            p.content AS content,
            TO_CHAR(p.updated_at, 'YYYY-MM-DD')  AS updatedAt
            FROM tb_ppc_bank p
            WHERE p.status = '200'
                 AND (
                     unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                     OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                 )
            """,
            countQuery = """
            SELECT count(p.id)
            FROM tb_ppc_bank p
            WHERE p.status = '200'
                 AND (
                     unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                     OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                 )
            """,
            nativeQuery = true)
    Page<IGetPageContents> findAllContentByStatus(@Param("searchValue") String searchValue, Pageable pageable);

    /**
     * Searches title, content, and tables for multiple keywords in a single query.
     * Uses OR logic to match any of the keywords.
     */
    @Query(
            value = """
            SELECT * FROM tb_ppc_bank
            WHERE fts_vector @@ websearch_to_tsquery('english', :combinedKeywords)
            ORDER BY ts_rank(fts_vector, websearch_to_tsquery('english', :combinedKeywords)) DESC
            LIMIT 20
                    """,
            nativeQuery = true
    )
    List<PPCBank> findByMultipleKeywordsCombined(@Param("combinedKeywords") String combinedKeywords);

}