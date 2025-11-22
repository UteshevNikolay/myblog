package com.my.blog.project.myblogonboot.myblog.repository;

import com.my.blog.project.myblogonboot.myblog.config.AbstractIntegrationTest;
import com.my.blog.project.myblogonboot.myblog.entity.Post;
import com.my.blog.project.myblogonboot.myblog.entity.PostImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PostImageRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private PostRepository postRepository;

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
    void save_shouldPersistImage() {
        // Given
        Post post = createAndSavePost("Test Post");
        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(new byte[]{1, 2, 3, 4, 5});
        image.setFilename("test.jpg");
        image.setContentType("image/jpeg");
        image.setSizeBytes(5L);

        // When
        PostImage saved = postImageRepository.save(image);

        // Then
        assertThat(saved.getPostId()).isEqualTo(post.getId());
        assertThat(saved.getData()).hasSize(5);
        assertThat(saved.getFilename()).isEqualTo("test.jpg");
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getSizeBytes()).isEqualTo(5L);
    }

    @Test
    void findByPostId_shouldReturnImage_whenExists() {
        // Given
        Post post = createAndSavePost("Test Post");
        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(new byte[]{1, 2, 3});
        image.setFilename("test.jpg");
        image.setContentType("image/jpeg");
        image.setSizeBytes(3L);
        postImageRepository.save(image);

        // When
        Optional<PostImage> found = postImageRepository.findByPostId(post.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getData()).hasSize(3);
        assertThat(found.get().getFilename()).isEqualTo("test.jpg");
    }

    @Test
    void findByPostId_shouldReturnEmpty_whenNotExists() {
        // Given
        Post post = createAndSavePost("Test Post");

        // When
        Optional<PostImage> found = postImageRepository.findByPostId(post.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void update_shouldReplaceExistingImage() {
        // Given
        Post post = createAndSavePost("Test Post");
        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(new byte[]{1, 2, 3});
        image.setFilename("old.jpg");
        image.setContentType("image/jpeg");
        image.setSizeBytes(3L);
        postImageRepository.save(image);

        // When - Update the image
        image.setData(new byte[]{4, 5, 6, 7});
        image.setFilename("new.jpg");
        image.setSizeBytes(4L);
        postImageRepository.save(image);

        // Then
        Optional<PostImage> updated = postImageRepository.findByPostId(post.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getData()).hasSize(4);
        assertThat(updated.get().getFilename()).isEqualTo("new.jpg");
        assertThat(updated.get().getSizeBytes()).isEqualTo(4L);
    }

    @Test
    void delete_shouldRemoveImage() {
        // Given
        Post post = createAndSavePost("Test Post");
        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(new byte[]{1, 2, 3});
        image.setFilename("test.jpg");
        image.setContentType("image/jpeg");
        image.setSizeBytes(3L);
        postImageRepository.save(image);

        // When
        postImageRepository.delete(image);

        // Then
        Optional<PostImage> found = postImageRepository.findByPostId(post.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldHandleLargeImages() {
        // Given
        Post post = createAndSavePost("Test Post");
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(largeData);
        image.setFilename("large.jpg");
        image.setContentType("image/jpeg");
        image.setSizeBytes((long) largeData.length);

        // When
        PostImage saved = postImageRepository.save(image);

        // Then
        Optional<PostImage> found = postImageRepository.findByPostId(post.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getData()).hasSize(1024 * 1024);
        assertThat(found.get().getSizeBytes()).isEqualTo(1024 * 1024);
    }

    @Test
    void save_shouldHandleDifferentImageFormats() {
        // Given
        Post post = createAndSavePost("Test Post");
        String[] formats = {"image/jpeg", "image/png", "image/gif", "image/webp"};

        for (String contentType : formats) {
            // Clean previous image
            jdbcTemplate.execute("DELETE FROM post_images");

            PostImage image = new PostImage();
            image.setPostId(post.getId());
            image.setData(("test-" + contentType).getBytes());
            image.setFilename("test." + contentType.split("/")[1]);
            image.setContentType(contentType);
            image.setSizeBytes((long) ("test-" + contentType).length());

            // When
            PostImage saved = postImageRepository.save(image);

            // Then
            assertThat(saved.getContentType()).isEqualTo(contentType);

            Optional<PostImage> found = postImageRepository.findByPostId(post.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getContentType()).isEqualTo(contentType);
        }
    }

    @Test
    void save_shouldMaintainDataIntegrity() {
        // Given
        Post post = createAndSavePost("Test Post");
        byte[] originalData = new byte[256];
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) i;
        }

        PostImage image = new PostImage();
        image.setPostId(post.getId());
        image.setData(originalData);
        image.setFilename("integrity-test.bin");
        image.setContentType("application/octet-stream");
        image.setSizeBytes((long) originalData.length);

        // When
        postImageRepository.save(image);

        // Then
        Optional<PostImage> retrieved = postImageRepository.findByPostId(post.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getData()).isEqualTo(originalData);
    }

    @Test
    void findByPostId_shouldReturnEmptyForNonExistentPost() {
        // Given
        Long nonExistentPostId = 999L;

        // When
        Optional<PostImage> found = postImageRepository.findByPostId(nonExistentPostId);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void multiplePostsCanHaveIndependentImages() {
        // Given
        Post post1 = createAndSavePost("Post 1");
        Post post2 = createAndSavePost("Post 2");

        PostImage image1 = new PostImage();
        image1.setPostId(post1.getId());
        image1.setData("image1 data".getBytes());
        image1.setFilename("image1.jpg");
        image1.setContentType("image/jpeg");
        image1.setSizeBytes(11L);

        PostImage image2 = new PostImage();
        image2.setPostId(post2.getId());
        image2.setData("image2 data".getBytes());
        image2.setFilename("image2.png");
        image2.setContentType("image/png");
        image2.setSizeBytes(11L);

        // When
        postImageRepository.save(image1);
        postImageRepository.save(image2);

        // Then
        Optional<PostImage> foundImage1 = postImageRepository.findByPostId(post1.getId());
        Optional<PostImage> foundImage2 = postImageRepository.findByPostId(post2.getId());

        assertThat(foundImage1).isPresent();
        assertThat(foundImage2).isPresent();
        assertThat(foundImage1.get().getFilename()).isEqualTo("image1.jpg");
        assertThat(foundImage2.get().getFilename()).isEqualTo("image2.png");
        assertThat(foundImage1.get().getData()).isNotEqualTo(foundImage2.get().getData());
    }

    private Post createAndSavePost(String title) {
        Post post = new Post();
        post.setTitle(title);
        post.setText("Content");
        post.setTags(new HashSet<>());
        post.setLikesCount(Integer.valueOf(0));
        post.setCommentsCount(Integer.valueOf(0));
        return postRepository.save(post);
    }
}

