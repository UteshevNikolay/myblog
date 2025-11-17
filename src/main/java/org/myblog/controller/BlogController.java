package org.myblog.controller;

import org.myblog.dto.post.PostRequest;
import org.myblog.dto.post.PostResponse;
import org.myblog.dto.post.PostsResponse;
import org.myblog.dto.comment.CommentResponse;
import org.myblog.dto.comment.CommentRequest;
import org.myblog.entity.PostImage;
import org.myblog.service.ImageService;
import org.myblog.service.PostService;
import org.myblog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class BlogController {

    @Autowired
    private PostService postService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private ImageService imageService;

    @GetMapping
    public PostsResponse getAllPosts(@RequestParam(value = "search", required = false) String search,
                                     @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
                                     @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable("postId") Long postId) {
        return postService.getPostById(postId);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getPostComments(@PathVariable("postId") Long postId) {
        return commentService.getCommentsByPostId(postId);
    }

    @GetMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> getPostComment(@PathVariable("postId") Long postId,
                                                          @PathVariable("postId") Long commentId) {
        return commentService.getCommentByPostIdAndCommentId(postId, commentId);
    }

    @GetMapping("/{postId}/image")
    public ResponseEntity<byte[]> downloadImage(@PathVariable(name = "postId") long postId) {
        Optional<PostImage> optionalPostImage = imageService.getPostImage(postId);
        if (optionalPostImage.isEmpty() || optionalPostImage.get().getData() == null || optionalPostImage.get().getData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        PostImage postImage = optionalPostImage.get();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (postImage.getContentType() != null) {
            mediaType = MediaType.parseMediaType(postImage.getContentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
//                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                .contentLength(postImage.getData().length)
                .body(postImage.getData());
    }

    @PostMapping
    public PostResponse addPost(@RequestBody PostRequest postRequest) {

        return postService.savePost(postRequest);
    }

    @PostMapping("/{postId}/likes")
    public Long addLikeToPost(@PathVariable(name = "postId") long postId) {

        return postService.incrementLike(postId);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable("postId") Long postId,
                                                      @RequestBody CommentRequest request) {
        return commentService.addComment(postId, request);
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(@RequestBody PostRequest postRequest,
                                   @PathVariable("postId") Long postId) {

        return postService.updatePost(postId, postRequest);
    }

    @PutMapping("/{postId}/image")
    public ResponseEntity<Void> uploadImage(@PathVariable(name = "postId") Long postId,
                                            @RequestParam("image") MultipartFile image) throws IOException {

        return imageService.uploadImage(postId, image);
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public CommentResponse updatePostComment(@RequestBody CommentRequest commentRequest,
                                             @PathVariable("postId") Long postId, @PathVariable(
                    "commentId") Long commentId) {

        return commentService.updatePostComment(postId, commentId, commentRequest);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable("postId") Long postId) {

        return postService.deletePost(postId);
    }
}
