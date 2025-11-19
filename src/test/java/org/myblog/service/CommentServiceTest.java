package org.myblog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.myblog.dto.comment.CommentResponse;
import org.myblog.dto.comment.CommentRequest;
import org.myblog.entity.Comment;
import org.myblog.entity.Post;
import org.myblog.mapper.CommentMapper;
import org.myblog.repository.CommentRepository;
import org.myblog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setText("Test Content");
        testPost.setTags(new HashSet<>());
        testPost.setLikesCount(0);
        testPost.setCommentsCount(0);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setPostId(1L);
        testComment.setText("Test Comment");
    }

    @Test
    void addComment_shouldCreateComment_whenValidRequest() {
        // Given
        long postId = 1L;
        CommentRequest request = new CommentRequest("New Comment");
        Comment newComment = new Comment(postId, "New Comment");
        newComment.setId(2L);

        CommentResponse expectedResponse = new CommentResponse(2L, "New Comment", 1L);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.save(any(Comment.class))).thenReturn(newComment);
        when(commentMapper.toDto(newComment)).thenReturn(expectedResponse);
        when(postRepository.save(testPost)).thenReturn(testPost);

        // When
        ResponseEntity<CommentResponse> result = commentService.addComment(postId, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().text()).isEqualTo("New Comment");
        verify(commentRepository).save(any(Comment.class));
        verify(postRepository).save(testPost);
        assertThat(testPost.getCommentsCount()).isEqualTo(1);
    }

    @Test
    void addComment_shouldReturnBadRequest_whenRequestIsNull() {
        // Given
        long postId = 1L;

        // When
        ResponseEntity<CommentResponse> result = commentService.addComment(postId, null);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_shouldReturnBadRequest_whenTextIsEmpty() {
        // Given
        long postId = 1L;
        CommentRequest request = new CommentRequest("   ");

        // When
        ResponseEntity<CommentResponse> result = commentService.addComment(postId, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_shouldReturnNotFound_whenPostDoesNotExist() {
        // Given
        long postId = 999L;
        CommentRequest request = new CommentRequest("New Comment");

        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        ResponseEntity<CommentResponse> result = commentService.addComment(postId, request);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void getCommentsByPostId_shouldReturnComments_whenPostExists() {
        // Given
        long postId = 1L;
        Comment comment1 = new Comment(1L, postId, "Comment 1");
        Comment comment2 = new Comment(2L, postId, "Comment 2");
        List<Comment> comments = Arrays.asList(comment1, comment2);

        List<CommentResponse> expectedResponses = Arrays.asList(
            new CommentResponse(1L, "Comment 1", postId),
            new CommentResponse(2L, "Comment 2", postId)
        );

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdOrderByIdAsc(postId)).thenReturn(comments);
        when(commentMapper.toDtoList(comments)).thenReturn(expectedResponses);

        // When
        ResponseEntity<List<CommentResponse>> result = commentService.getCommentsByPostId(postId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
        verify(commentRepository).findByPostIdOrderByIdAsc(postId);
    }

    @Test
    void getCommentsByPostId_shouldReturnBadRequest_whenPostDoesNotExist() {
        // Given
        long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        ResponseEntity<List<CommentResponse>> result = commentService.getCommentsByPostId(postId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(commentRepository, never()).findByPostIdOrderByIdAsc(any());
    }

    @Test
    void getCommentByPostIdAndCommentId_shouldReturnComment_whenExists() {
        // Given
        long postId = 1L;
        long commentId = 1L;
        CommentResponse expectedResponse = new CommentResponse(1L, "Test Comment", postId);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdAndId(postId, commentId)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(expectedResponse);

        // When
        ResponseEntity<CommentResponse> result = commentService.getCommentByPostIdAndCommentId(postId, commentId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().text()).isEqualTo("Test Comment");
        verify(commentRepository).findByPostIdAndId(postId, commentId);
    }

    @Test
    void getCommentByPostIdAndCommentId_shouldReturnBadRequest_whenPostDoesNotExist() {
        // Given
        long postId = 999L;
        long commentId = 1L;
        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        ResponseEntity<CommentResponse> result = commentService.getCommentByPostIdAndCommentId(postId, commentId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(commentRepository, never()).findByPostIdAndId(any(), any());
    }
}

