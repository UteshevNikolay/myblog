package com.my.blog.project.myblogonboot.myblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.blog.project.myblogonboot.myblog.config.AbstractIntegrationTest;
import com.my.blog.project.myblogonboot.myblog.dto.comment.CommentRequest;
import com.my.blog.project.myblogonboot.myblog.dto.post.PostRequest;
import com.my.blog.project.myblogonboot.myblog.dto.post.PostResponse;
import com.my.blog.project.myblogonboot.myblog.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class BlogControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PostService postService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_images");
        jdbcTemplate.execute("DELETE FROM posts_tags");
        jdbcTemplate.execute("DELETE FROM posts");
        jdbcTemplate.execute("DELETE FROM tags");
    }

    @Test
    void createPost_shouldReturn200_withValidRequest() throws Exception {
        // Given
        PostRequest request = new PostRequest("New Post", "Post content", Arrays.asList("Java", "Spring"));
        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("New Post"))
                .andExpect(jsonPath("$.text").value("Post content"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("Java", "Spring")))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void getPost_shouldReturn200_whenPostExists() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Test Post", "Content", Arrays.asList("Java")));

        // When & Then
        mockMvc.perform(get("/api/posts/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.text").value("Content"))
                .andExpect(jsonPath("$.tags[0]").value("Java"));
    }

    @Test
    void getAllPosts_shouldReturnPagedResults() throws Exception {
        // Given
        postService.savePost(new PostRequest("Post 1", "Content 1", Arrays.asList("Java")));
        postService.savePost(new PostRequest("Post 2", "Content 2", Arrays.asList("Spring")));
        postService.savePost(new PostRequest("Post 3", "Content 3", Arrays.asList("Testing")));

        // When & Then
        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.lastPage").isNumber())
                .andExpect(jsonPath("$.hasPrev").isBoolean())
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    void getAllPosts_shouldFilterBySearchQuery() throws Exception {
        // Given
        postService.savePost(new PostRequest("Java Tutorial", "Content", Arrays.asList("Java")));
        postService.savePost(new PostRequest("Spring Guide", "Content", Arrays.asList("Spring")));

        // When & Then
        mockMvc.perform(get("/api/posts")
                        .param("search", "Java")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    void updatePost_shouldReturn200_withValidRequest() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Original", "Content", Arrays.asList("Java")));

        PostRequest updateRequest = new PostRequest("Updated Title", "Updated Content",
                Arrays.asList("Spring", "Testing"));
        String requestJson = objectMapper.writeValueAsString(updateRequest);

        // When & Then
        mockMvc.perform(put("/api/posts/" + created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.text").value("Updated Content"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("Spring", "Testing")));
    }

    @Test
    void deletePost_shouldReturn204_whenPostExists() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post to Delete", "Content", Arrays.asList("Java")));

        // When & Then
        mockMvc.perform(delete("/api/posts/" + created.id()))
                .andExpect(status().isNoContent());

        // Verify post is deleted
        mockMvc.perform(get("/api/posts/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void deletePost_shouldReturn404_whenPostDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/posts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLike_shouldIncrementLikesCount() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post", "Content", Arrays.asList("Java")));

        // When & Then
        mockMvc.perform(post("/api/posts/" + created.id() + "/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));

        // Verify likes count increased
        mockMvc.perform(post("/api/posts/" + created.id() + "/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void uploadImage_shouldReturn204_withValidImage() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post with Image", "Content", Arrays.asList("Java")));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/posts/" + created.id() + "/image")
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNoContent());
    }

    @Test
    void downloadImage_shouldReturn200_whenImageExists() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post", "Content", Arrays.asList("Java")));

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/posts/" + created.id() + "/image")
                .file(image)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }));

        // When & Then
        mockMvc.perform(get("/api/posts/" + created.id() + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes("fake image content".getBytes()));
    }

    @Test
    void downloadImage_shouldReturn404_whenImageDoesNotExist() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post without Image", "Content", Arrays.asList("Java")));

        // When & Then
        mockMvc.perform(get("/api/posts/" + created.id() + "/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addComment_shouldReturn200_withValidRequest() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post", "Content", Arrays.asList("Java")));

        String commentJson = objectMapper.writeValueAsString(
                new CommentRequest("Test comment"));

        // When & Then
        mockMvc.perform(post("/api/posts/" + created.id() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.postId").value(created.id()))
                .andExpect(jsonPath("$.text").value("Test comment"));
    }

    @Test
    void getComments_shouldReturnAllComments() throws Exception {
        // Given
        PostResponse created = postService.savePost(
                new PostRequest("Post", "Content", Arrays.asList("Java")));

        String comment1 = objectMapper.writeValueAsString(
                new CommentRequest("Comment 1"));
        String comment2 = objectMapper.writeValueAsString(
                new CommentRequest("Comment 2"));

        mockMvc.perform(post("/api/posts/" + created.id() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(comment1));
        mockMvc.perform(post("/api/posts/" + created.id() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(comment2));

        // When & Then
        mockMvc.perform(get("/api/posts/" + created.id() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].text").value("Comment 1"))
                .andExpect(jsonPath("$[1].text").value("Comment 2"));
    }
}

