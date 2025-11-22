package com.my.blog.project.myblogonboot.myblog.service;

import com.my.blog.project.myblogonboot.myblog.dto.comment.CommentRequest;
import com.my.blog.project.myblogonboot.myblog.dto.comment.CommentResponse;
import com.my.blog.project.myblogonboot.myblog.entity.Comment;
import com.my.blog.project.myblogonboot.myblog.entity.Post;
import com.my.blog.project.myblogonboot.myblog.mapper.CommentMapper;
import com.my.blog.project.myblogonboot.myblog.repository.CommentRepository;
import com.my.blog.project.myblogonboot.myblog.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceUpdateCommentTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

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
        testPost.setCommentsCount(1);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setPostId(1L);
        testComment.setText("Original comment text");
    }

    @Test
    void updatePostComment_shouldUpdateCommentSuccessfully() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest("Updated comment text");

        CommentResponse expectedResponse = new CommentResponse(1L, "Updated comment text", 1L);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdAndId(postId, commentId)).thenReturn(testComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(expectedResponse);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.text()).isEqualTo("Updated comment text");
        assertThat(testComment.getText()).isEqualTo("Updated comment text");
        verify(commentRepository).save(testComment);
        verify(commentMapper).toDto(testComment);
    }

    @Test
    void updatePostComment_shouldTrimWhitespace() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest("   Updated text   ");

        CommentResponse expectedResponse = new CommentResponse(1L, "Updated text", 1L);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdAndId(postId, commentId)).thenReturn(testComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(expectedResponse);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Updated text");
        assertThat(testComment.getText()).isEqualTo("Updated text");
        verify(commentRepository).save(testComment);
    }

    @Test
    void updatePostComment_shouldReturnNull_whenRequestIsNull() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, null);

        // Then
        assertThat(result).isNull();
        verify(postRepository, never()).findPostById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatePostComment_shouldReturnNull_whenTextIsNull() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest(null);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNull();
        verify(postRepository, never()).findPostById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatePostComment_shouldReturnNull_whenTextIsEmpty() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest("   ");

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNull();
        verify(postRepository, never()).findPostById(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatePostComment_shouldReturnNull_whenPostNotFound() {
        // Given
        Long postId = 999L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest("Updated text");

        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNull();
        verify(postRepository).findPostById(postId);
        verify(commentRepository, never()).findByPostIdAndId(any(), any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatePostComment_shouldReturnNull_whenCommentNotFound() {
        // Given
        Long postId = 1L;
        Long commentId = 999L;
        CommentRequest request = new CommentRequest("Updated text");

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdAndId(postId, commentId)).thenReturn(null);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNull();
        verify(postRepository).findPostById(postId);
        verify(commentRepository).findByPostIdAndId(postId, commentId);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatePostComment_shouldReturnNull_whenCommentBelongsToDifferentPost() {
        // Given
        Long postId = 1L;
        Long commentId = 1L;
        CommentRequest request = new CommentRequest("Updated text");

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdAndId(postId, commentId)).thenReturn(null);

        // When
        CommentResponse result = commentService.updatePostComment(postId, commentId, request);

        // Then
        assertThat(result).isNull();
        verify(commentRepository, never()).save(any());
    }
}

