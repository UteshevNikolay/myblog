package com.my.blog.project.myblogonboot.myblog.repository;

import com.my.blog.project.myblogonboot.myblog.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> {
        Comment comment = new Comment();
        comment.setId(rs.getLong("id"));
        comment.setPostId(rs.getLong("post_id"));
        comment.setText(rs.getString("text"));
        return comment;
    };

    public List<Comment> findByPostIdOrderByIdAsc(Long postId) {
        String sql = "SELECT id, post_id, text FROM comments WHERE post_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, commentRowMapper, postId);
    }

    public Comment findByPostIdAndId(Long postId, Long commentId) {
        String sql = "SELECT id, post_id, text FROM comments WHERE post_id = ? AND id = ?";
        List<Comment> comments = jdbcTemplate.query(sql, commentRowMapper, postId, commentId);
        return comments.isEmpty() ? null : comments.get(0);
    }

    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            String sql = "INSERT INTO comments (post_id, text) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setLong(1, comment.getPostId());
                ps.setString(2, comment.getText());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            comment.setId(key != null ? key.longValue() : null);
        } else {
            String sql = "UPDATE comments SET post_id = ?, text = ? WHERE id = ?";
            jdbcTemplate.update(sql, comment.getPostId(), comment.getText(), comment.getId());
        }
        return comment;
    }

    public void deleteAll(List<Comment> comments) {
        for (Comment comment : comments) {
            deleteById(comment.getId());
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM comments WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
