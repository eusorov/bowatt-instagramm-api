package com.bowatt.instagramm.api.repositories;

import com.bowatt.instagramm.api.models.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<Image> findById(Long id);

    @EntityGraph(attributePaths = "tags")
    Page<Image> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    /**
     * Find images by all tag names (AND condition) and order by created at and id in descending order
     * @param tagNames
     * @param tagCount
     * @param pageable
     * @return
     */
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
    Page<Image> findByAllTagNamesOrderByCreatedAtDescIdDesc(
            @Param("tagNames") Collection<String> tagNames,
            @Param("tagCount") long tagCount,
            Pageable pageable);
}
