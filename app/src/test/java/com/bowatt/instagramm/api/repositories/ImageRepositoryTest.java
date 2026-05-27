package com.bowatt.instagramm.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class ImageRepositoryTest {

    private static final Instant CREATED_AT_1 = Instant.parse("2026-05-27T10:00:00Z");
    private static final Instant CREATED_AT_2 = Instant.parse("2026-05-27T11:00:00Z");
    private static final Instant CREATED_AT_3 = Instant.parse("2026-05-27T12:00:00Z");

    @Autowired private ImageRepository imageRepository;
    @Autowired private TagRepository tagRepository;

    private Tag beachTag;
    private Tag sunsetTag;
    private Tag summerTag;
    private Image beachAndSunsetImage;
    private Image beachOnlyImage;

    @BeforeEach
    void setUp() {
        beachTag = tagRepository.save(new Tag("beach"));
        sunsetTag = tagRepository.save(new Tag("sunset"));
        summerTag = tagRepository.save(new Tag("summer"));

        beachAndSunsetImage =
                imageRepository.save(
                        newImage(
                                "both.jpg",
                                "1234567890both.jpg",
                                "beach and sunset",
                                Set.of(beachTag, sunsetTag),
                                CREATED_AT_1));
        beachOnlyImage =
                imageRepository.save(
                        newImage(
                                "beach.jpg",
                                "1234567890beach.jpg",
                                "beach only",
                                Set.of(beachTag),
                                CREATED_AT_2));
        imageRepository.save(
                newImage(
                        "sunset.jpg",
                        "1234567890sunset.jpg",
                        "sunset and summer",
                        Set.of(sunsetTag, summerTag),
                        CREATED_AT_3));
    }

    @Test
    void findByAllTagNames_returnsOnlyImagesMatchingAllRequestedTags() {
        var page =
                imageRepository.findByAllTagNames(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(1, page.getTotalElements());
        assertEquals(beachAndSunsetImage.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findByAllTagNames_excludesImagesWithOnlySubsetOfRequestedTags() {
        var page =
                imageRepository.findByAllTagNames(
                        List.of("beach", "summer"),
                        2,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertTrue(page.isEmpty());
    }

    @Test
    void findByAllTagNames_withSingleTag_returnsAllImagesContainingThatTag() {
        var page =
                imageRepository.findByAllTagNames(
                        List.of("beach"),
                        1,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(2, page.getTotalElements());
        assertEquals(
                List.of(beachOnlyImage.getId(), beachAndSunsetImage.getId()),
                page.getContent().stream().map(Image::getId).toList());
    }

    @Test
    void findByAllTagNames_supportsPagination() {
        imageRepository.save(
                newImage(
                        "both-2.jpg",
                        "both-2.jpg.jpg",
                        "another beach and sunset",
                        Set.of(beachTag, sunsetTag),
                        CREATED_AT_3.plusSeconds(1)));
        imageRepository.save(
                newImage(
                        "both-3.jpg",
                        "both-3.jpg.jpg",
                        "yet another beach and sunset",
                        Set.of(beachTag, sunsetTag),
                        CREATED_AT_3.plusSeconds(2)));

        var firstPage =
                imageRepository.findByAllTagNames(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));
        var secondPage =
                imageRepository.findByAllTagNames(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
    }

    @Test
    void findByAllTagNames_eagerlyLoadsTags() {
        var page =
                imageRepository.findByAllTagNames(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        Image image = page.getContent().getFirst();
        assertTrue(Hibernate.isInitialized(image.getTags()));
        assertEquals(Set.of("beach", "sunset"), tagNames(image));
    }

    private static Image newImage(
            String originalFilename,
            String storedFilename,
            String title,
            Set<Tag> tags,
            Instant createdAt) {
        return new Image(
                originalFilename,
                storedFilename,
                "image/jpeg",
                128L,
                title,
                new LinkedHashSet<>(tags),
                createdAt);
    }

    private static Set<String> tagNames(Image image) {
        return image.getTags().stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
