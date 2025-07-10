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
        List<PPCBank> findTop5ByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);

        @Query(value = """

                WITH keywords AS (
                 SELECT lower(word) AS keyword
                 FROM regexp_split_to_table(?, '\\s+') AS word
                 WHERE lower(word) NOT IN (
                   'the', 'a', 'an', 'of', 'to', 'is', 'in', 'on', 'at', 'for',
                   'with', 'and', 'or', 'from', 'by', 'check', 'me', 'tell', 'about',
                   'this', 'that', 'it', 'you', 'your'
                 )
                ),
                matches AS (
                 SELECT
                   tb.*,
                   kw.keyword
                 FROM tb_ppc_bank tb
                 JOIN keywords kw ON TRUE
                 WHERE tb.status = 200
                   AND (
                     unaccent(lower(tb.title)) ILIKE unaccent(CONCAT('%', kw.keyword, '%'))
                     OR unaccent(lower(tb.content)) ILIKE unaccent(CONCAT('%', kw.keyword, '%'))
                     OR (tb.content_json ->> 'title') ILIKE CONCAT('%', kw.keyword, '%')
                     OR EXISTS (
                       SELECT 1
                       FROM jsonb_array_elements(tb.content_json -> 'sections') AS section
                       WHERE (section ->> 'heading') ILIKE CONCAT('%', kw.keyword, '%')
                         OR (
                           jsonb_typeof(section -> 'paragraphs') = 'array'
                           AND EXISTS (
                             SELECT 1
                             FROM jsonb_array_elements_text(section -> 'paragraphs') AS paragraph
                             WHERE paragraph ILIKE CONCAT('%', kw.keyword, '%')
                           )
                         )
                         OR (
                           jsonb_typeof(section -> 'list') = 'array'
                           AND EXISTS (
                             SELECT 1
                             FROM jsonb_array_elements_text(section -> 'list') AS list_item
                             WHERE list_item ILIKE CONCAT('%', kw.keyword, '%')
                           )
                         )
                     )
                     OR EXISTS (
                       SELECT 1
                       FROM jsonb_array_elements(tb.content_json -> 'tables') AS table_data,
                            jsonb_array_elements(table_data -> 'rows') AS row,
                            jsonb_array_elements_text(row) AS cell
                       WHERE cell ILIKE CONCAT('%', kw.keyword, '%')
                     )
                     OR (
                       tb.fts_vector IS NOT NULL
                       AND tb.fts_vector @@ plainto_tsquery('english', kw.keyword)
                     )
                   )
                ),
                ranked_matches AS (
                 SELECT *, COUNT(*) OVER (PARTITION BY id) AS match_count
                 FROM matches
                )
                SELECT *
                FROM ranked_matches
                WHERE match_count >= 2
                ORDER BY updated_at DESC
                LIMIT 200
               
                
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