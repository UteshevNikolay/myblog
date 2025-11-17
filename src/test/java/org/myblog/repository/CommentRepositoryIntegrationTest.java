package org.myblog.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myblog.config.AbstractIntegrationTest;
import org.myblog.entity.Comment;
import org.myblog.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TagRepository tagRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    private Post testPost;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postImageRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();

        // Create a test post
        testPost = new Post();
        testPost.setTitle("Test Post");
        testPost.setText("Content");
        testPost.setTags(new HashSet<>());
        testPost.setLikesCount(Integer.valueOf(0));
        testPost.setCommentsCount(Integer.valueOf(0));
        testPost = postRepository.save(testPost);
    }

    @Test
    void save_shouldPersistComment() {
        // Given
        Comment comment = new Comment(testPost, "Test comment");

        // When
        Comment saved = commentRepository.save(comment);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getText()).isEqualTo("Test comment");
        assertThat(saved.getPost().getId()).isEqualTo(testPost.getId());
    }

    @Test
    void findByPostIdOrderByIdAsc_shouldReturnCommentsInOrder() {
        // Given
        Comment comment1 = commentRepository.save(new Comment(testPost, "First comment"));
        Comment comment2 = commentRepository.save(new Comment(testPost, "Second comment"));
        Comment comment3 = commentRepository.save(new Comment(testPost, "Third comment"));

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
        Comment comment = commentRepository.save(new Comment(testPost, "Test comment"));

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
        Comment comment = commentRepository.save(new Comment(testPost, "Test comment"));

        // When
        commentRepository.delete(comment);
        commentRepository.flush();

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
        post2.setLikesCount(Integer.valueOf(0));
        post2.setCommentsCount(Integer.valueOf(0));
        post2 = postRepository.save(post2);

        commentRepository.save(new Comment(testPost, "Comment on post 1"));
        commentRepository.save(new Comment(post2, "Comment on post 2"));

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

