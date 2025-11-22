package com.my.blog.project.myblogonboot.myblog.repository;

import com.my.blog.project.myblogonboot.myblog.config.AbstractIntegrationTest;
import com.my.blog.project.myblogonboot.myblog.entity.Post;
import com.my.blog.project.myblogonboot.myblog.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PostRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostRepository postRepository;

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
    void searchByTitleOnly_shouldFindMatchingPosts() {
        // Given
        postRepository.save(createPost("Java Tutorial", "Content"));
        postRepository.save(createPost("Spring Guide", "Content"));
        postRepository.save(createPost("Advanced Java", "Content"));

        // When - Search on first page
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "java", true, Arrays.asList(), false, 0, 1, 10);

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

        // When - Search on first page
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "Java", true, Arrays.asList(), false, 0, 1, 10);

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

        // When - Find posts with both Java AND Spring tags (first page)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList("java", "spring"), true, 2, 1, 10);

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

        // When - Find posts with "Tutorial" in title AND Spring tag (first page)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "Tutorial", true, Arrays.asList("spring"), true, 1, 1, 10);

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

        // Then
        Post found = postRepository.findPostById(saved.getId());
        assertThat(found).isNull();
    }

    @Test
    void pagination_shouldReturnFirstPage() {
        // Given - Create 15 posts
        for (int i = 1; i <= 15; i++) {
            postRepository.save(createPost("Post " + i, "Content " + i));
        }

        // When - Request first page (page 1) with 10 items
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 10);

        // Then
        assertThat(results.getContent()).hasSize(10);
        assertThat(results.getTotalElements()).isEqualTo(15);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getPageNumber()).isEqualTo(1);
        assertThat(results.getPageSize()).isEqualTo(10);
    }

    @Test
    void pagination_shouldReturnSecondPage() {
        // Given - Create 15 posts
        for (int i = 1; i <= 15; i++) {
            postRepository.save(createPost("Post " + i, "Content " + i));
        }

        // When - Request second page (page 2) with 10 items
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 2, 10);

        // Then
        assertThat(results.getContent()).hasSize(5);
        assertThat(results.getTotalElements()).isEqualTo(15);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getPageNumber()).isEqualTo(2);
        assertThat(results.getPageSize()).isEqualTo(10);
    }

    @Test
    void pagination_shouldReturnLastPage() {
        // Given - Create 25 posts
        for (int i = 1; i <= 25; i++) {
            postRepository.save(createPost("Post " + i, "Content " + i));
        }

        // When - Request last page (page 3) with 10 items per page
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 3, 10);

        // Then
        assertThat(results.getContent()).hasSize(5);
        assertThat(results.getTotalElements()).isEqualTo(25);
        assertThat(results.getTotalPages()).isEqualTo(3);
        assertThat(results.getPageNumber()).isEqualTo(3);
    }

    @Test
    void pagination_shouldReturnEmptyPageWhenPageNumberTooHigh() {
        // Given - Create 5 posts
        for (int i = 1; i <= 5; i++) {
            postRepository.save(createPost("Post " + i, "Content " + i));
        }

        // When - Request page 6 (which doesn't exist)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 6, 10);

        // Then
        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isEqualTo(5);
        assertThat(results.getTotalPages()).isEqualTo(1);
    }

    @Test
    void pagination_shouldHandleDifferentPageSizes() {
        // Given - Create 10 posts
        for (int i = 1; i <= 10; i++) {
            postRepository.save(createPost("Post " + i, "Content " + i));
        }

        // When - Request first page with page size of 3
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 3);

        // Then
        assertThat(results.getContent()).hasSize(3);
        assertThat(results.getTotalElements()).isEqualTo(10);
        assertThat(results.getTotalPages()).isEqualTo(4);
        assertThat(results.getPageSize()).isEqualTo(3);
    }

    @Test
    void pagination_shouldWorkWithSearchQuery() {
        // Given - Create posts with different titles
        postRepository.save(createPost("Java Tutorial 1", "Content"));
        postRepository.save(createPost("Spring Guide", "Content"));
        postRepository.save(createPost("Java Tutorial 2", "Content"));
        postRepository.save(createPost("Docker Guide", "Content"));
        postRepository.save(createPost("Java Advanced", "Content"));

        // When - Search for "Java" with pagination (first page)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "Java", true, Arrays.asList(), false, 0, 1, 2);

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getContent())
                .extracting(Post::getTitle)
                .allMatch(title -> title.toLowerCase().contains("java"));
    }

    @Test
    void pagination_shouldWorkWithTagFilter() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Tag springTag = tagRepository.save(new Tag("Spring"));

        // Create 8 posts with Java tag
        for (int i = 1; i <= 8; i++) {
            postRepository.save(createPostWithTags("Java Post " + i, "Content", javaTag));
        }
        // Create 2 posts with Spring tag
        postRepository.save(createPostWithTags("Spring Post 1", "Content", springTag));
        postRepository.save(createPostWithTags("Spring Post 2", "Content", springTag));

        // When - Filter by Java tag with page size 5 (first page)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList("java"), true, 1, 1, 5);

        // Then
        assertThat(results.getContent()).hasSize(5);
        assertThat(results.getTotalElements()).isEqualTo(8);
        assertThat(results.getTotalPages()).isEqualTo(2);
    }

    @Test
    void pagination_shouldWorkWithMultipleTagsFilter() {
        // Given
        Tag javaTag = tagRepository.save(new Tag("Java"));
        Tag springTag = tagRepository.save(new Tag("Spring"));
        Tag dockerTag = tagRepository.save(new Tag("Docker"));

        // Create posts with different tag combinations
        postRepository.save(createPostWithTags("Post 1", "Content", javaTag, springTag));
        postRepository.save(createPostWithTags("Post 2", "Content", javaTag));
        postRepository.save(createPostWithTags("Post 3", "Content", javaTag, springTag));
        postRepository.save(createPostWithTags("Post 4", "Content", springTag, dockerTag));
        postRepository.save(createPostWithTags("Post 5", "Content", javaTag, springTag));

        // When - Filter by both Java and Spring tags with pagination (first page)
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList("java", "spring"), true, 2, 1, 2);

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getTotalPages()).isEqualTo(2);
    }

    @Test
    void pagination_shouldReturnEmptyWhenNoResults() {
        // Given - Create some posts
        postRepository.save(createPost("Post 1", "Content"));
        postRepository.save(createPost("Post 2", "Content"));

        // When - Search for non-existent term
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "NonExistent", true, Arrays.asList(), false, 0, 1, 10);

        // Then
        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isEqualTo(0);
        assertThat(results.getTotalPages()).isEqualTo(0);
    }

    @Test
    void pagination_shouldCalculateTotalPagesCorrectly() {
        // Given - Create different numbers of posts
        for (int i = 1; i <= 7; i++) {
            postRepository.save(createPost("Post " + i, "Content"));
        }

        // Test 1: 7 posts, 5 per page = 2 pages (first page)
        PostRepository.PageResult<Post> result1 = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 5);
        assertThat(result1.getTotalPages()).isEqualTo(2);

        // Test 2: 7 posts, 10 per page = 1 page (first page)
        PostRepository.PageResult<Post> result2 = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 10);
        assertThat(result2.getTotalPages()).isEqualTo(1);

        // Test 3: 7 posts, 7 per page = 1 page (first page)
        PostRepository.PageResult<Post> result3 = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 7);
        assertThat(result3.getTotalPages()).isEqualTo(1);
    }

    @Test
    void pagination_shouldReturnPostsInCorrectOrder() {
        // Given - Create posts with specific order
        Post post1 = postRepository.save(createPost("First Post", "Content"));
        Post post2 = postRepository.save(createPost("Second Post", "Content"));
        Post post3 = postRepository.save(createPost("Third Post", "Content"));

        // When - Get first page
        PostRepository.PageResult<Post> results = postRepository.searchByTitleAndAllTagNames(
                "", false, Arrays.asList(), false, 0, 1, 2);

        // Then - Posts should be in descending order by ID (newest first)
        assertThat(results.getContent()).hasSize(2);
        // Since posts are ordered by ID DESC, the first one should have a higher ID
        assertThat(results.getContent().get(0).getId()).isGreaterThan(results.getContent().get(1).getId());
        // And the third post (most recent) should be first
        assertThat(results.getContent().get(0).getId()).isEqualTo(post3.getId());
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

