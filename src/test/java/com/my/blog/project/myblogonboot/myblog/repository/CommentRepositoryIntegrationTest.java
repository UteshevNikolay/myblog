package com.my.blog.project.myblogonboot.myblog.repository;

import com.my.blog.project.myblogonboot.myblog.config.AbstractIntegrationTest;
import com.my.blog.project.myblogonboot.myblog.entity.Comment;
import com.my.blog.project.myblogonboot.myblog.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CommentRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Post testPost;

    @BeforeEach
    void setUp() {
        // Delete in correct order
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_images");
        jdbcTemplate.execute("DELETE FROM posts_tags");
        jdbcTemplate.execute("DELETE FROM posts");
        jdbcTemplate.execute("DELETE FROM tags");

        // Create a test post
        testPost = new Post();
        testPost.setTitle("Test Post");
        testPost.setText("Content");
        testPost.setTags(new HashSet<>());
        testPost.setLikesCount(0);
        testPost.setCommentsCount(0);
        testPost = postRepository.save(testPost);
    }

    @Test
    void save_shouldPersistComment() {
        // Given
        Comment comment = new Comment(testPost.getId(), "Test comment");

        // When
        Comment saved = commentRepository.save(comment);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getText()).isEqualTo("Test comment");
        assertThat(saved.getPostId()).isEqualTo(testPost.getId());
    }

    @Test
    void findByPostIdOrderByIdAsc_shouldReturnCommentsInOrder() {
        // Given
        Comment comment1 = commentRepository.save(new Comment(testPost.getId(), "First comment"));
        Comment comment2 = commentRepository.save(new Comment(testPost.getId(), "Second comment"));
        Comment comment3 = commentRepository.save(new Comment(testPost.getId(), "Third comment"));

        // When
        List<Comment> results = commentRepository.findByPostIdOrderByIdAsc(testPost.getId());

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getText()).isEqualTo("First comment");
        assertThat(results.get(1).getText()).isEqualTo("Second comment");
        assertThat(results.get(2).getText()).isEqualTo("Third comment");
    }

    @Test
    void findByPostIdOrderByIdAsc_shouldReturnEmptyList_whenNoComments() {
        // When
        List<Comment> results = commentRepository.findByPostIdOrderByIdAsc(testPost.getId());

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void findByPostIdAndId_shouldReturnComment_whenExists() {
        // Given
        Comment comment = commentRepository.save(new Comment(testPost.getId(), "Test comment"));

        // When
        Comment found = commentRepository.findByPostIdAndId(testPost.getId(), comment.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(comment.getId());
        assertThat(found.getText()).isEqualTo("Test comment");
    }

    @Test
    void findByPostIdAndId_shouldReturnNull_whenNotExists() {
        // When
        Comment found = commentRepository.findByPostIdAndId(testPost.getId(), 999L);

        // Then
        assertThat(found).isNull();
    }

    @Test
    void delete_shouldRemoveComment() {
        // Given
        Comment comment = commentRepository.save(new Comment(testPost.getId(), "Test comment"));

        // When
        commentRepository.deleteById(comment.getId());

        // Then
        Comment found = commentRepository.findByPostIdAndId(testPost.getId(), comment.getId());
        assertThat(found).isNull();
    }

    @Test
    void comments_shouldBeIsolatedByPost() {
        // Given
        Post post2 = new Post();
        post2.setTitle("Another Post");
        post2.setText("Content");
        post2.setTags(new HashSet<>());
        post2.setLikesCount(0);
        post2.setCommentsCount(0);
        post2 = postRepository.save(post2);

        commentRepository.save(new Comment(testPost.getId(), "Comment on post 1"));
        commentRepository.save(new Comment(post2.getId(), "Comment on post 2"));

        // When
        List<Comment> comments1 = commentRepository.findByPostIdOrderByIdAsc(testPost.getId());
        List<Comment> comments2 = commentRepository.findByPostIdOrderByIdAsc(post2.getId());

        // Then
        assertThat(comments1).hasSize(1);
        assertThat(comments1.get(0).getText()).isEqualTo("Comment on post 1");

        assertThat(comments2).hasSize(1);
        assertThat(comments2.get(0).getText()).isEqualTo("Comment on post 2");
    }
}

