package com.bowatt.instagramm.api.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bowatt.instagramm.api.models.Image;
import com.bowatt.instagramm.api.models.Tag;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ImageRepositoryTest {

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
                                Set.of(beachTag, sunsetTag)));
        beachOnlyImage =
                imageRepository.save(
                        newImage(
                                "beach.jpg",
                                "1234567890beach.jpg",
                                "beach only",
                                Set.of(beachTag)
                                ));
        imageRepository.save(
                newImage(
                        "sunset.jpg",
                        "1234567890sunset.jpg",
                        "sunset and summer",
                        Set.of(sunsetTag, summerTag)
                        ));
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_returnsOnlyImagesMatchingAllRequestedTags() {
        var page =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(beachAndSunsetImage.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_excludesImagesWithOnlySubsetOfRequestedTags() {
        var page =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "summer"),
                        2,
                        PageRequest.of(0, 20));

        assertTrue(page.isEmpty());
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_withSingleTag_returnsAllImagesContainingThatTag() {
        var page =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach"),
                        1,
                        PageRequest.of(0, 20));

        assertEquals(2, page.getTotalElements());
        assertEquals(
                List.of(beachOnlyImage.getId(), beachAndSunsetImage.getId()),
                page.getContent().stream().map(Image::getId).toList());
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_supportsPagination() {
        imageRepository.save(
                newImage(
                        "both-2.jpg",
                        "1234567890both-2.jpg",
                        "another beach and sunset",
                        Set.of(beachTag, sunsetTag)
                        ));
        imageRepository.save(
                newImage(
                        "both-3.jpg",
                        "1234567890both-3.jpg",
                        "yet another beach and sunset",
                        Set.of(beachTag, sunsetTag)
                        ));

        var firstPage =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 2));
        var secondPage =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(1, 2));

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_eagerlyLoadsTags() {
        var page =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "sunset"),
                        2,
                        PageRequest.of(0, 20));

        Image image = page.getContent().getFirst();
        assertTrue(Hibernate.isInitialized(image.getTags()));
        assertEquals(Set.of("beach", "sunset"), tagNames(image));
    }

    @Test
    void findAllByOrderByCreatedAtDescIdDesc_returnsFirstCursorPage() {
        var firstPage = imageRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));

        assertEquals(2, firstPage.getContent().size());
    }

    @Test
    void findByAllTagNamesOrderByCreatedAtDescIdDesc_returnsFirstCursorPage() {
        imageRepository.save(
                newImage(
                        "both-2.jpg",
                        "1234567890both-2.jpg",
                        "another beach and sunset",
                        Set.of(beachTag, sunsetTag)
                        ));

        var firstPage =
                imageRepository.findByAllTagNamesOrderByCreatedAtDescIdDesc(
                        List.of("beach", "sunset"), 2, PageRequest.of(0, 1));

        assertEquals(1, firstPage.getContent().size());
    }

    private static Image newImage(
            String originalFilename,
            String storedFilename,
            String title,
            Set<Tag> tags){
        return new Image(
                originalFilename,
                storedFilename,
                "image/jpeg",
                128L,
                title,
                new LinkedHashSet<>(tags)
                );
    }

    private static Set<String> tagNames(Image image) {
        return image.getTags().stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
