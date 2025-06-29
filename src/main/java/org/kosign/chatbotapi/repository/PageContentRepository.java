package org.kosign.chatbotapi.repository;

import org.kosign.chatbotapi.domains.PageContent;
import org.kosign.chatbotapi.payload.IGetPageContents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageContentRepository extends JpaRepository<PageContent, Long> {

    // Search by content containing keywords (case-insensitive)
    @Query("SELECT p FROM PageContent p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PageContent> findByContentContainingIgnoreCase(@Param("keyword") String keyword);

    // Search by title containing keywords (case-insensitive)
    @Query("SELECT p FROM PageContent p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PageContent> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    // Search by both title and content
    @Query("SELECT p FROM PageContent p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PageContent> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword);

    // Full text search across multiple keywords
    @Query("SELECT p FROM PageContent p WHERE " +
           "(:keyword1 IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword1, '%'))) AND " +
           "(:keyword2 IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword2, '%'))) AND " +
           "(:keyword3 IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword3, '%')))")
    List<PageContent> findByMultipleKeywords(
            @Param("keyword1") String keyword1,
            @Param("keyword2") String keyword2,
            @Param("keyword3") String keyword3
    );

    // Get recent pages with pagination
    @Query("SELECT p FROM PageContent p ORDER BY p.updatedAt DESC")
    Page<PageContent> findRecentPages(Pageable pageable);

    // Find pages by URL pattern
    @Query("SELECT p FROM PageContent p WHERE LOWER(p.url) LIKE LOWER(CONCAT('%', :urlPattern, '%'))")
    List<PageContent> findByUrlContainingIgnoreCase(@Param("urlPattern") String urlPattern);

    // Get page count
    @Query("SELECT COUNT(p) FROM PageContent p")
    Long getTotalPageCount();

    // Get pages with specific status codes
    List<PageContent> findByStatus(Integer status);

    @Query(value = """
            SELECT p.id AS id,
            p.url AS url,
            p.title AS title,
            p.content AS content,
            TO_CHAR(p.updated_at, 'YYYY-MM-DD')  AS updatedAt 
            FROM pages p 
            WHERE p.status = '200'
                 AND (
                     unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                     OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                 )
             ORDER BY p.id ASC
            """, nativeQuery = true)
    Page<IGetPageContents> findAllContentByStatus(String sort, @Param("searchValue") String searchValue, Pageable pageable);
} 