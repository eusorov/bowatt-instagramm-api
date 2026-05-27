package com.bowatt.instagramm.api.repositories;

import com.bowatt.instagramm.api.models.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<Image> findById(Long id);

    @EntityGraph(attributePaths = "tags")
    Page<Image> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    List<Image> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    @Query(
            """
            SELECT i FROM Image i
            WHERE i.createdAt < :cursorCreatedAt
               OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId)
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Image> findAllAfterCursorOrderByCreatedAtDescIdDesc(
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    @Query(
            """
            SELECT i FROM Image i
            WHERE i.id IN (
                SELECT i2.id FROM Image i2 JOIN i2.tags t
                WHERE t.name IN :tagNames
                GROUP BY i2.id
                HAVING COUNT(DISTINCT t.name) = :tagCount
            )
            """)
    Page<Image> findByAllTagNames(
            @Param("tagNames") Collection<String> tagNames,
            @Param("tagCount") long tagCount,
            Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    @Query(
            """
            SELECT i FROM Image i
            WHERE i.id IN (
                SELECT i2.id FROM Image i2 JOIN i2.tags t
                WHERE t.name IN :tagNames
                GROUP BY i2.id
                HAVING COUNT(DISTINCT t.name) = :tagCount
            )
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Image> findByAllTagNamesOrderByCreatedAtDescIdDesc(
            @Param("tagNames") Collection<String> tagNames,
            @Param("tagCount") long tagCount,
            Pageable pageable);

    @EntityGraph(attributePaths = "tags")
    @Query(
            """
            SELECT i FROM Image i
            WHERE i.id IN (
                SELECT i2.id FROM Image i2 JOIN i2.tags t
                WHERE t.name IN :tagNames
                GROUP BY i2.id
                HAVING COUNT(DISTINCT t.name) = :tagCount
            )
            AND (
                i.createdAt < :cursorCreatedAt
                OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId)
            )
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Image> findByAllTagNamesAfterCursorOrderByCreatedAtDescIdDesc(
            @Param("tagNames") Collection<String> tagNames,
            @Param("tagCount") long tagCount,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);
}
