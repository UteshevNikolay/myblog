package org.myblog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.myblog.entity.Post;
import org.myblog.entity.PostImage;
import org.myblog.repository.PostImageRepository;
import org.myblog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private ImageService imageService;

    private Post testPost;
    private PostImage testPostImage;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setId(1L);
        testPost.setTitle("Test Post");
        testPost.setText("Test Content");
        testPost.setTags(new HashSet<>());
        testPost.setLikesCount(0);
        testPost.setCommentsCount(0);

        testPostImage = new PostImage();
        testPostImage.setPostId(testPost.getId());
        testPostImage.setData(new byte[]{1, 2, 3, 4, 5});
        testPostImage.setFilename("test.jpg");
        testPostImage.setContentType("image/jpeg");
        testPostImage.setSizeBytes(5L);
    }

    @Test
    void uploadImage_shouldUploadSuccessfully_whenValidImage() throws IOException {
        // Given
        Long postId = 1L;
        byte[] imageData = "test image data".getBytes();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                imageData
        );

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(postImageRepository.findByPostId(postId)).thenReturn(Optional.empty());
        when(postImageRepository.save(any(PostImage.class))).thenReturn(testPostImage);

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, image);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(postRepository).findPostById(postId);
        verify(postImageRepository).findByPostId(postId);
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void uploadImage_shouldReplaceExistingImage_whenImageAlreadyExists() throws IOException {
        // Given
        Long postId = 1L;
        byte[] newImageData = "new image data".getBytes();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "new-test.jpg",
                "image/jpeg",
                newImageData
        );

        when(postRepository.findPostById(postId)).thenReturn(testPost);
        when(postImageRepository.findByPostId(postId)).thenReturn(Optional.of(testPostImage));
        when(postImageRepository.save(any(PostImage.class))).thenReturn(testPostImage);

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, image);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void uploadImage_shouldReturnBadRequest_whenImageIsNull() throws IOException {
        // Given
        Long postId = 1L;

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, null);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(postRepository, never()).findPostById(any());
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void uploadImage_shouldReturnBadRequest_whenImageIsEmpty() throws IOException {
        // Given
        Long postId = 1L;
        MockMultipartFile emptyImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, emptyImage);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(postRepository, never()).findPostById(any());
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void uploadImage_shouldReturnPayloadTooLarge_whenImageExceeds100MB() throws IOException {
        // Given
        Long postId = 1L;
        // Create a mock file that reports size > 100MB
        MultipartFile largeImage = mock(MultipartFile.class);
        when(largeImage.isEmpty()).thenReturn(false);
        when(largeImage.getSize()).thenReturn(101L * 1024 * 1024); // 101MB

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, largeImage);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verify(postRepository, never()).findPostById(any());
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void uploadImage_shouldReturnNotFound_whenPostDoesNotExist() throws IOException {
        // Given
        Long postId = 999L;
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test data".getBytes()
        );

        when(postRepository.findPostById(postId)).thenReturn(null);

        // When
        ResponseEntity<Void> result = imageService.uploadImage(postId, image);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(postRepository).findPostById(postId);
        verify(postImageRepository, never()).save(any());
    }

    @Test
    void getPostImage_shouldReturnImage_whenExists() {
        // Given
        Long postId = 1L;
        when(postImageRepository.findByPostId(postId)).thenReturn(Optional.of(testPostImage));

        // When
        Optional<PostImage> result = imageService.getPostImage(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPostImage);
        assertThat(result.get().getFilename()).isEqualTo("test.jpg");
        assertThat(result.get().getContentType()).isEqualTo("image/jpeg");
        verify(postImageRepository).findByPostId(postId);
    }

    @Test
    void getPostImage_shouldReturnEmpty_whenNotExists() {
        // Given
        Long postId = 1L;
        when(postImageRepository.findByPostId(postId)).thenReturn(Optional.empty());

        // When
        Optional<PostImage> result = imageService.getPostImage(postId);

        // Then
        assertThat(result).isEmpty();
        verify(postImageRepository).findByPostId(postId);
    }
}

