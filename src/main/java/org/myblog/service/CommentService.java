package org.myblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.myblog.dto.comment.CommentResponse;
import org.myblog.dto.comment.CommentRequest;
import org.myblog.entity.Comment;
import org.myblog.entity.Post;
import org.myblog.mapper.CommentMapper;
import org.myblog.repository.CommentRepository;
import org.myblog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public ResponseEntity<CommentResponse> getCommentByPostIdAndCommentId(Long postId, Long commentId) {
        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Could get comment by post id: {} and comment id: {}", postId, commentId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Comment comment = commentRepository.findByPostIdAndId(postId, commentId);
        return ResponseEntity.ok(commentMapper.toDto(comment));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<List<CommentResponse>> getCommentsByPostId(Long postId) {
        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Could get comment because post with id {} does not exist", postId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        List<Comment> comments = commentRepository.findByPostIdOrderByIdAsc(postId);
        return ResponseEntity.ok(commentMapper.toDtoList(comments));
    }

    @Transactional
    public CommentResponse updatePostComment(Long postId, Long commentId, CommentRequest commentRequest) {
        // Validate input
        if (commentRequest == null || commentRequest.text() == null || commentRequest.text().trim().isEmpty()) {
            log.warn("Cannot update comment - invalid request");
            return null;
        }

        // Find the post
        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Cannot update comment - post with id {} does not exist", postId);
            return null;
        }

        // Find the comment
        Comment comment = commentRepository.findByPostIdAndId(postId, commentId);
        if (comment == null) {
            log.warn("Cannot update comment - comment with id {} for post {} does not exist", commentId, postId);
            return null;
        }

        // Update the comment text
        String newText = commentRequest.text().trim();
        comment.setText(newText);
        Comment comment1 = commentRepository.save(comment);

        log.info("Updated comment {} for post {}", commentId, postId);

        // Return the updated post
        return commentMapper.toDto(comment1);
    }

    @Transactional
    public ResponseEntity<CommentResponse> addComment(Long postId, CommentRequest commentRequest) {
        if (commentRequest == null || commentRequest.text() == null || commentRequest.text().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String text = commentRequest.text().trim();
        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Could not create comment because post with id {} does not exist", postId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Comment comment = commentRepository.save(new Comment(post, text));

        int current = post.getCommentsCount();
        post.setCommentsCount(current + 1);
        postRepository.save(post);

        return ResponseEntity.status(HttpStatus.OK).body(commentMapper.toDto(comment));
    }
}
