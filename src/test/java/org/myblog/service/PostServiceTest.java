package org.myblog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.myblog.dto.post.PostRequest;
import org.myblog.dto.post.PostResponse;
import org.myblog.entity.Comment;
import org.myblog.entity.Post;
import org.myblog.entity.PostImage;
import org.myblog.entity.Tag;
import org.myblog.mapper.PostMapper;
import org.myblog.repository.CommentRepository;
import org.myblog.repository.PostImageRepository;
import org.myblog.repository.PostRepository;
import org.myblog.repository.TagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private SearchQueryBuilder queryBuilder;

    @InjectMocks
    private PostService postService;

    private Post testPost;
    private PostRequest postRequest;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setUp() {
        tag1 = new Tag(1L, "Java");
        tag2 = new Tag(2L, "Spring");

        Set<Tag> tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);

        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setText("Test Content");
        testPost.setTags(tags);
        testPost.setLikesCount(0);
        testPost.setCommentsCount(0);

        postRequest = new PostRequest("Test Post", "Test Content", List.of("Java", "Spring"));
    }

    @Test
    void savePost_shouldCreatePostWithTags() {
        // Given
        when(postMapper.toEntity(postRequest)).thenReturn(testPost);
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(tag1));
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.of(tag2));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse expectedResponse = new PostResponse(1L, "Test Post", "Test Content",
                List.of("Java", "Spring"), 0, 0);
        when(postMapper.toDto(testPost)).thenReturn(expectedResponse);

        // When
        PostResponse result = postService.savePost(postRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Post");
        assertThat(result.tags()).containsExactlyInAnyOrder("Java", "Spring");
        verify(postRepository).save(any(Post.class));
        verify(tagRepository, times(2)).findByNameIgnoreCase(anyString());
    }

    @Test
    void savePost_shouldCreateNewTags_whenTagsDoNotExist() {
        // Given
        when(postMapper.toEntity(postRequest)).thenReturn(testPost);
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(tagRepository.findByNameIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(tag1, tag2);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse expectedResponse = new PostResponse(1L, "Test Post", "Test Content",
                List.of("Java", "Spring"), 0, 0);
        when(postMapper.toDto(testPost)).thenReturn(expectedResponse);

        // When
        PostResponse result = postService.savePost(postRequest);

        // Then
        assertThat(result).isNotNull();
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void updatePost_shouldUpdatePostSuccessfully() {
        // Given
        long postId = 1L;
        PostRequest updateRequest = new PostRequest("Updated Title", "Updated Content",
                List.of("Java"));

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(tag1));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse expectedResponse = new PostResponse(1L, "Updated Title", "Updated Content",
                List.of("Java"), 0, 0);
        when(postMapper.toDto(testPost)).thenReturn(expectedResponse);

        // When
        PostResponse result = postService.updatePost(postId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Updated Title");
        verify(postRepository).save(testPost);
        assertThat(testPost.getTitle()).isEqualTo("Updated Title");
        assertThat(testPost.getText()).isEqualTo("Updated Content");
    }

    @Test
    void updatePost_shouldReturnNull_whenPostNotFound() {
        // Given
        long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        PostResponse result = postService.updatePost(postId, postRequest);

        // Then
        assertThat(result).isNull();
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_shouldDeletePostAndRelatedData() {
        // Given
        long postId = 1L;
        List<Comment> comments = new ArrayList<>();
        comments.add(new Comment(1L, postId, "Comment 1"));
        comments.add(new Comment(2L, postId, "Comment 2"));

        PostImage postImage = new PostImage();
        postImage.setPostId(postId);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(commentRepository.findByPostIdOrderByIdAsc(postId)).thenReturn(comments);
        when(postImageRepository.findByPostId(postId)).thenReturn(Optional.of(postImage));

        // When
        ResponseEntity<Void> result = postService.deletePost(postId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(commentRepository).deleteAll(comments);
        verify(postImageRepository).delete(postImage);
        verify(postRepository).delete(testPost);
    }

    @Test
    void deletePost_shouldReturnNotFound_whenPostDoesNotExist() {
        // Given
        long postId = 999L;
        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        ResponseEntity<Void> result = postService.deletePost(postId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(commentRepository, never()).deleteAll(any());
        verify(postRepository, never()).delete(any());
    }

    @Test
    void getPostById_shouldReturnPost() {
        // Given
        long postId = 1L;
        PostResponse expectedResponse = new PostResponse(1L, "Test Post", "Test Content",
                List.of("Java", "Spring"), 0, 0);

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(expectedResponse);

        // When
        PostResponse result = postService.getPostById(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Test Post");
        verify(postRepository).findPostById(postId);
    }

    @Test
    void incrementLike_shouldIncrementAndReturnLikesCount() {
        // Given
        long postId = 1L;
        Long expectedLikes = 5L;

        when(postRepository.getCurrentLikes(postId)).thenReturn(expectedLikes);

        // When
        Long result = postService.incrementLike(postId);

        // Then
        assertThat(result).isEqualTo(expectedLikes);
        verify(postRepository).incrementLikes(postId);
        verify(postRepository).getCurrentLikes(postId);
    }

}

