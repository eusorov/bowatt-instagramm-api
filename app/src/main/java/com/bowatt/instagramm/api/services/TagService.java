package com.bowatt.instagramm.api.services;

import com.bowatt.instagramm.api.repositories.TagRepository;
import com.bowatt.instagramm.api.web.dto.TagResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listAll() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .toList();
    }
}
