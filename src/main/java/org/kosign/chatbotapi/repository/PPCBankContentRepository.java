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
        
        // JSON-aware search methods for structured content
        @Query(value = """

                SELECT * FROM tb_ppc_bank
                        WHERE status = 200 AND (
                            -- Search in JSON title
                            (content_json->>'title') ILIKE CONCAT('%', :keyword, '%')
                        
                            -- Search in section headings
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE (section->>'heading') ILIKE CONCAT('%', :keyword, '%')
                            )
                        
                            -- Search in section paragraphs (only if paragraphs is array)
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE jsonb_typeof(section->'paragraphs') = 'array'
                                  AND EXISTS (
                                    SELECT 1 FROM jsonb_array_elements_text(section->'paragraphs') AS paragraph
                                    WHERE paragraph ILIKE CONCAT('%', :keyword, '%')
                                  )
                            )
                        
                            -- Search in section lists (only if list is array)
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE jsonb_typeof(section->'list') = 'array'
                                  AND EXISTS (
                                    SELECT 1 FROM jsonb_array_elements_text(section->'list') AS list_item
                                    WHERE list_item ILIKE CONCAT('%', :keyword, '%')
                                  )
                            )
                            -- Search in table data
                        
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'tables') AS table_data,
                                              jsonb_array_elements(table_data->'rows') AS row,
                                              jsonb_array_elements_text(row) AS cell
                                WHERE cell ILIKE CONCAT('%', :keyword, '%')
                            )
                        
                            -- Fallback to full-text search if available
                            OR (fts_vector IS NOT NULL AND fts_vector @@ websearch_to_tsquery('english', :keyword))
                        
                            -- Final fallback to regular text search (unaccented)
                            OR (
                                unaccent(lower(title)) ILIKE unaccent(lower(CONCAT('%', :keyword, '%')))
                                OR unaccent(lower(content)) ILIKE unaccent(lower(CONCAT('%', :keyword, '%')))
                            )
                        )
                        ORDER BY
                            -- Prioritize JSON title matches
                            CASE WHEN (content_json->>'title') ILIKE CONCAT('%', :keyword, '%') THEN 100 ELSE 0 END +
                            -- Then section heading matches
                            CASE WHEN EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE (section->>'heading') ILIKE CONCAT('%', :keyword, '%')
                            ) THEN 80 ELSE 0 END +
                            -- Then FTS rank if available
                            CASE
                                WHEN fts_vector IS NOT NULL THEN ts_rank(fts_vector, websearch_to_tsquery('english', :keyword)) * 50
                                ELSE 0
                            END DESC,
                            updated_at DESC
                        LIMIT 20
                        """, nativeQuery = true)
        List<PPCBank> searchByFullText(@Param("keyword") String keyword);

        @Query(value = """

                SELECT * FROM tb_ppc_bank
                        WHERE status = 200 AND (
                            (content_json->>'title') ILIKE CONCAT('%', :combinedKeywords, '%')
                        
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE
                                    (section->>'heading') ILIKE CONCAT('%', :combinedKeywords, '%')
                        
                                    OR (
                                        jsonb_typeof(section->'paragraphs') = 'array' AND EXISTS (
                                            SELECT 1 FROM jsonb_array_elements_text(section->'paragraphs') AS paragraph
                                            WHERE paragraph ILIKE CONCAT('%', :combinedKeywords, '%')
                                        )
                                    )
                        
                                    OR (
                                        jsonb_typeof(section->'list') = 'array' AND EXISTS (
                                            SELECT 1 FROM jsonb_array_elements_text(section->'list') AS list_item
                                            WHERE list_item ILIKE CONCAT('%', :combinedKeywords, '%')
                                        )
                                    )
                            )
                        
                            OR EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'tables') AS table_data,
                                              jsonb_array_elements(table_data->'rows') AS row,
                                              jsonb_array_elements_text(row) AS cell
                                WHERE cell ILIKE CONCAT('%', :combinedKeywords, '%')
                            )
                        
                            OR (
                                fts_vector IS NOT NULL AND fts_vector @@ websearch_to_tsquery('english', :combinedKeywords)
                            )
                        
                            OR (
                                unaccent(lower(title)) ILIKE unaccent(lower(CONCAT('%', :combinedKeywords, '%')))
                                OR unaccent(lower(content)) ILIKE unaccent(lower(CONCAT('%', :combinedKeywords, '%')))
                            )
                        )
                        ORDER BY
                            CASE WHEN (content_json->>'title') ILIKE CONCAT('%', :combinedKeywords, '%') THEN 100 ELSE 0 END +
                            CASE WHEN EXISTS (
                                SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                                WHERE (section->>'heading') ILIKE CONCAT('%', :combinedKeywords, '%')
                            ) THEN 80 ELSE 0 END DESC,
                            updated_at DESC
                        LIMIT 20
                        """, nativeQuery = true)
        List<PPCBank> findByMultipleKeywordsCombined(@Param("combinedKeywords") String combinedKeywords);

        @Query(value = """
                        SELECT * FROM tb_ppc_bank p
                        WHERE p.status = 200 AND (
                            (:keyword1 IS NULL OR (
                                (p.content_json->>'title') ILIKE CONCAT('%', :keyword1, '%')
                                OR EXISTS (
                                    SELECT 1 FROM jsonb_array_elements(p.content_json->'sections') AS section
                                    WHERE (section->>'heading') ILIKE CONCAT('%', :keyword1, '%')
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'paragraphs') AS paragraph WHERE paragraph ILIKE CONCAT('%', :keyword1, '%'))
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'list') AS list_item WHERE list_item ILIKE CONCAT('%', :keyword1, '%'))
                                )
                                OR (p.fts_vector IS NOT NULL AND p.fts_vector @@ websearch_to_tsquery('english', :keyword1))
                                OR unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :keyword1, '%')))
                                OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :keyword1, '%')))
                            )) AND
                            (:keyword2 IS NULL OR (
                                (p.content_json->>'title') ILIKE CONCAT('%', :keyword2, '%')
                                OR EXISTS (
                                    SELECT 1 FROM jsonb_array_elements(p.content_json->'sections') AS section
                                    WHERE (section->>'heading') ILIKE CONCAT('%', :keyword2, '%')
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'paragraphs') AS paragraph WHERE paragraph ILIKE CONCAT('%', :keyword2, '%'))
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'list') AS list_item WHERE list_item ILIKE CONCAT('%', :keyword2, '%'))
                                )
                                OR (p.fts_vector IS NOT NULL AND p.fts_vector @@ websearch_to_tsquery('english', :keyword2))
                                OR unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :keyword2, '%')))
                                OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :keyword2, '%')))
                            )) AND
                            (:keyword3 IS NULL OR (
                                (p.content_json->>'title') ILIKE CONCAT('%', :keyword3, '%')
                                OR EXISTS (
                                    SELECT 1 FROM jsonb_array_elements(p.content_json->'sections') AS section
                                    WHERE (section->>'heading') ILIKE CONCAT('%', :keyword3, '%')
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'paragraphs') AS paragraph WHERE paragraph ILIKE CONCAT('%', :keyword3, '%'))
                                       OR EXISTS (SELECT 1 FROM jsonb_array_elements_text(section->'list') AS list_item WHERE list_item ILIKE CONCAT('%', :keyword3, '%'))
                                )
                                OR (p.fts_vector IS NOT NULL AND p.fts_vector @@ websearch_to_tsquery('english', :keyword3))
                                OR unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :keyword3, '%')))
                                OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :keyword3, '%')))
                            ))
                        )
                        ORDER BY p.updated_at DESC
                        LIMIT 20
                        """, nativeQuery = true)
        List<PPCBank> findByMultipleKeywords(
                        @Param("keyword1") String keyword1,
                        @Param("keyword2") String keyword2,
                        @Param("keyword3") String keyword3);

        // Specific JSON structure search methods
        @Query(value = """
                        SELECT * FROM tb_ppc_bank
                        WHERE status = 200 AND 
                              (content_json->>'title') ILIKE CONCAT('%', :titleKeyword, '%')
                        ORDER BY updated_at DESC
                        """, nativeQuery = true)
        List<PPCBank> findByJsonTitle(@Param("titleKeyword") String titleKeyword);

        @Query(value = """
                        SELECT * FROM tb_ppc_bank
                        WHERE status = 200 AND EXISTS (
                            SELECT 1 FROM jsonb_array_elements(content_json->'sections') AS section
                            WHERE (section->>'heading') ILIKE CONCAT('%', :heading, '%')
                        )
                        ORDER BY updated_at DESC
                        """, nativeQuery = true)
        List<PPCBank> findByJsonSectionHeading(@Param("heading") String heading);

        @Query(value = """
                        SELECT * FROM tb_ppc_bank
                        WHERE status = 200 AND EXISTS (
                            SELECT 1 FROM jsonb_array_elements(content_json->'tables') AS table_data,
                                          jsonb_array_elements(table_data->'rows') AS row,
                                          jsonb_array_elements_text(row) AS cell
                            WHERE cell ILIKE CONCAT('%', :tableContent, '%')
                        )
                        ORDER BY updated_at DESC
                        """, nativeQuery = true)
        List<PPCBank> findByJsonTableContent(@Param("tableContent") String tableContent);

        // Simple text search methods (fallback)
        @Query("SELECT p FROM PPCBank p WHERE p.status = 200 AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :term, '%'))) ORDER BY p.updatedAt DESC")
        List<PPCBank> findByTitleOrContentContainingIgnoreCase(@Param("term") String term);

        @Query("SELECT p FROM PPCBank p WHERE LOWER(p.url) LIKE LOWER(CONCAT('%', :urlPattern, '%'))")
        List<PPCBank> findByUrlContainingIgnoreCase(@Param("urlPattern") String urlPattern);

        // Page and status methods
        @Query("SELECT p FROM PPCBank p ORDER BY p.updatedAt DESC")
        Page<PPCBank> findRecentPages(Pageable pageable);

        List<PPCBank> findByStatus(Integer status);

        @Query("SELECT COUNT(p) FROM PPCBank p")
        Long getTotalPageCount();

        // Projection query for API responses
        @Query(value = """
                        SELECT p.id AS no,
                        p.url AS url,
                        p.title AS title,
                        p.content AS content,
                        TO_CHAR(p.updated_at, 'YYYY-MM-DD') AS updatedAt
                        FROM tb_ppc_bank p
                        WHERE p.status = 200
                             AND (
                                 :searchValue IS NULL OR :searchValue = '' OR
                                 unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                                 OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                             )
                        ORDER BY p.updated_at DESC
                        """, countQuery = """
                        SELECT count(p.id)
                        FROM tb_ppc_bank p
                        WHERE p.status = 200
                             AND (
                                 :searchValue IS NULL OR :searchValue = '' OR
                                 unaccent(lower(p.title)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                                 OR unaccent(lower(p.content)) ILIKE unaccent(lower(CONCAT('%', :searchValue, '%')))
                             )
                        """, nativeQuery = true)
        Page<IGetPageContents> findAllContentByStatus(@Param("searchValue") String searchValue, Pageable pageable);

}