package com.bowatt.instagramm.api.repositories;

import com.bowatt.instagramm.api.models.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @EntityGraph(attributePaths = "tags")
    Page<Image> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
