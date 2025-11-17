package org.myblog.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myblog.config.AbstractIntegrationTest;
import org.myblog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TagRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Use native queries to delete in correct order
        entityManager.createNativeQuery("DELETE FROM comments").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM post_images").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM posts_tags").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
        entityManager.flush();
    }

    @Test
    void save_shouldPersistTag() {
        // Given
        Tag tag = new Tag("Java");

        // When
        Tag saved = tagRepository.save(tag);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Java");
    }

    @Test
    void findByNameIgnoreCase_shouldFindTag() {
        // Given
        tagRepository.save(new Tag("Java"));

        // When
        Optional<Tag> found = tagRepository.findByNameIgnoreCase("java");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Java");
    }

    @Test
    void findByNameIgnoreCase_shouldBeCaseInsensitive() {
        // Given
        tagRepository.save(new Tag("Spring"));

        // When & Then
        assertThat(tagRepository.findByNameIgnoreCase("spring")).isPresent();
        assertThat(tagRepository.findByNameIgnoreCase("SPRING")).isPresent();
        assertThat(tagRepository.findByNameIgnoreCase("Spring")).isPresent();
        assertThat(tagRepository.findByNameIgnoreCase("SpRiNg")).isPresent();
    }

    @Test
    void findByNameIgnoreCase_shouldReturnEmpty_whenNotExists() {
        // When
        Optional<Tag> found = tagRepository.findByNameIgnoreCase("NonExistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void delete_shouldRemoveTag() {
        // Given
        Tag tag = tagRepository.save(new Tag("ToDelete"));

        // When
        tagRepository.delete(tag);
        tagRepository.flush();

        // Then
        Optional<Tag> found = tagRepository.findByNameIgnoreCase("ToDelete");
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllTags() {
        // Given
        tagRepository.save(new Tag("Java"));
        tagRepository.save(new Tag("Spring"));
        tagRepository.save(new Tag("Docker"));

        // When
        var allTags = tagRepository.findAll();

        // Then
        assertThat(allTags).hasSize(3);
        assertThat(allTags).extracting("name")
                .containsExactlyInAnyOrder("Java", "Spring", "Docker");
    }

    @Test
    void save_shouldEnforceUniqueConstraint() {
        // Given
        tagRepository.save(new Tag("Java"));
        entityManager.flush();

        // When & Then
        Tag duplicate = new Tag("Java");

        // Should throw DataIntegrityViolationException due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> {
                    tagRepository.save(duplicate);
                    entityManager.flush();
                }
        );
    }

    @Test
    void save_shouldEnforceCaseInsensitiveUniqueness() {
        // Given
        tagRepository.save(new Tag("java"));
        entityManager.flush();

        // When & Then - Try to save with different case
        Tag differentCase = new Tag("JAVA");

        // Should throw DataIntegrityViolationException due to case-insensitive unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> {
                    tagRepository.save(differentCase);
                    entityManager.flush();
                }
        );
    }

    @Test
    void save_shouldPreserveExactCase() {
        // Given
        Tag tag = new Tag("JavaScript");

        // When
        Tag saved = tagRepository.save(tag);

        // Then
        assertThat(saved.getName()).isEqualTo("JavaScript");

        // Verify case is preserved when retrieving
        Optional<Tag> found = tagRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("JavaScript");
    }
}

