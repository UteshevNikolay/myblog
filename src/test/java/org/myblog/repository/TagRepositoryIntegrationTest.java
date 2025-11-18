package org.myblog.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myblog.config.AbstractIntegrationTest;
import org.myblog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TagRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Delete in correct order
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_images");
        jdbcTemplate.execute("DELETE FROM posts_tags");
        jdbcTemplate.execute("DELETE FROM posts");
        jdbcTemplate.execute("DELETE FROM tags");
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
        tagRepository.deleteById(tag.getId());

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

        // When & Then
        Tag duplicate = new Tag("Java");

        // Should throw DataIntegrityViolationException due to unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> {
                    tagRepository.save(duplicate);
                }
        );
    }

    @Test
    void save_shouldEnforceCaseInsensitiveUniqueness() {
        // Given
        tagRepository.save(new Tag("java"));

        // When & Then - Try to save with different case
        Tag differentCase = new Tag("JAVA");

        // Should throw DataIntegrityViolationException due to case-insensitive unique constraint
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> {
                    tagRepository.save(differentCase);
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

