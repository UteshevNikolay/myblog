package org.myblog.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myblog.config.AbstractIntegrationTest;
import org.myblog.entity.Post;
import org.myblog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PostRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostRepository postRepository;

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
    void save_shouldPersistPost() {
        // Given
        Post post = new Post();
        post.setTitle("Test Post");
        post.setText("Test content");
        post.setTags(new HashSet<>());
        post.setLikesCount(0);
        post.setCommentsCount(0);

        // When
        Post saved = postRepository.save(post);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Post");
        assertThat(saved.getText()).isEqualTo("Test content");
    }

    @Test
    void findPostById_shouldReturnPost_whenExists() {
        // Given
        Post post = createPost("Test Post", "Content");
        Post saved = postRepository.save(post);

        // When
        Post found = postRepository.findPostById(saved.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTitle()).isEqualTo("Test Post");
    }

    @Test
    void findPostById_shouldReturnNull_whenNotExists() {
        // When
        Post found = postRepository.findPostById(999L);

        // Then
        assertThat(found).isNull();
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldFindMatchingPosts() {
        // Given
        postRepository.save(createPost("Java Tutorial", "Content"));
        postRepository.save(createPost("Spring Guide", "Content"));
        postRepository.save(createPost("Advanced Java", "Content"));

        // When
        Page<Post> results = postRepository.findByTitleContainingIgnoreCase("java", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Java Tutorial", "Advanced Java");
    }

    @Test
    void searchByTitleAndAllTagNames_shouldFindPostsByTitle() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Post post1 = createPostWithTags("Java Tutorial", "Content", javaTag);
        Post post2 = createPostWithTags("Spring Guide", "Content", javaTag);
        postRepository.save(post1);
        postRepository.save(post2);

        // When
        Page<Post> results = postRepository.searchByTitleAndAllTagNames(
                "Java", true, Arrays.asList(), false, 0, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle()).isEqualTo("Java Tutorial");
    }

    @Test
    void searchByTitleAndAllTagNames_shouldFindPostsByTags() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Tag springTag = tagRepository.save(new Tag("Spring"));

        Post post1 = createPostWithTags("Post 1", "Content", javaTag, springTag);
        Post post2 = createPostWithTags("Post 2", "Content", javaTag);
        Post post3 = createPostWithTags("Post 3", "Content", springTag);

        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);

        // When - Find posts with both Java AND Spring tags
        Page<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList("java", "spring"), true, 2, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle()).isEqualTo("Post 1");
    }

    @Test
    void searchByTitleAndAllTagNames_shouldFindPostsByTitleAndTags() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Tag springTag = tagRepository.save(new Tag("Spring"));

        Post post1 = createPostWithTags("Java Tutorial", "Content", javaTag, springTag);
        Post post2 = createPostWithTags("Java Guide", "Content", javaTag);
        Post post3 = createPostWithTags("Spring Tutorial", "Content", springTag);

        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);

        // When - Find posts with "Tutorial" in title AND Spring tag
        Page<Post> results = postRepository.searchByTitleAndAllTagNames(
                "Tutorial", true, Arrays.asList("spring"), true, 1, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent())
                .extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Java Tutorial", "Spring Tutorial");
    }

    @Test
    void incrementLikes_shouldIncrementLikesCount() {
        // Given
        Post post = createPost("Test Post", "Content");
        Post saved = postRepository.save(post);
        assertThat(saved.getLikesCount()).isEqualTo(0);

        // When
        postRepository.incrementLikes(saved.getId());
        postRepository.flush();

        // Then
        Long currentLikes = postRepository.getCurrentLikes(saved.getId());
        assertThat(currentLikes).isEqualTo(1);
    }

    @Test
    void incrementLikes_shouldIncrementMultipleTimes() {
        // Given
        Post post = createPost("Test Post", "Content");
        Post saved = postRepository.save(post);

        // When
        postRepository.incrementLikes(saved.getId());
        postRepository.incrementLikes(saved.getId());
        postRepository.incrementLikes(saved.getId());
        postRepository.flush();

        // Then
        Long currentLikes = postRepository.getCurrentLikes(saved.getId());
        assertThat(currentLikes).isEqualTo(3);
    }

    @Test
    void delete_shouldRemovePost() {
        // Given
        Post post = createPost("Test Post", "Content");
        Post saved = postRepository.save(post);

        // When
        postRepository.delete(saved);
        postRepository.flush();

        // Then
        Post found = postRepository.findPostById(saved.getId());
        assertThat(found).isNull();
    }

    @Test
    void finPostsByTagId_shouldReturnPostsWithTag() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Tag springTag = tagRepository.save(new Tag("Spring"));

        Post post1 = createPostWithTags("Post 1", "Content", javaTag);
        Post post2 = createPostWithTags("Post 2", "Content", javaTag, springTag);
        Post post3 = createPostWithTags("Post 3", "Content", springTag);

        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);

        // When
        List<Post> results = postRepository.finPostsByTagId(javaTag.getId());

        // Then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Post 1", "Post 2");
    }

    private Post createPost(String title, String text) {
        Post post = new Post();
        post.setTitle(title);
        post.setText(text);
        post.setTags(new HashSet<>());
        post.setLikesCount(0);
        post.setCommentsCount(0);
        return post;
    }

    private Post createPostWithTags(String title, String text, Tag... tags) {
        Post post = createPost(title, text);
        Set<Tag> tagSet = new HashSet<>(Arrays.asList(tags));
        post.setTags(tagSet);
        return post;
    }
}

