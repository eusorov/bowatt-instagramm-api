package com.bowatt.instagramm.api.repositories;

import com.bowatt.instagramm.api.models.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findAllByOrderByNameAsc();
}
