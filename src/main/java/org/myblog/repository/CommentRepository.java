package org.myblog.repository;

import org.myblog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByIdAsc(Long postId);
    Comment findByPostIdAndId(Long postId, Long commentId);
}
