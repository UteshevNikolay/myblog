package org.myblog.repository;

import lombok.RequiredArgsConstructor;
import org.myblog.entity.PostImage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostImageRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PostImage> postImageRowMapper = (rs, rowNum) -> {
        PostImage postImage = new PostImage();
        postImage.setPostId(rs.getLong("post_id"));
        postImage.setData(rs.getBytes("data"));
        postImage.setContentType(rs.getString("content_type"));
        postImage.setSizeBytes(rs.getLong("size_bytes"));
        postImage.setFilename(rs.getString("filename"));
        return postImage;
    };

    public Optional<PostImage> findByPostId(Long postId) {
        String sql = "SELECT post_id, data, content_type, size_bytes, filename FROM post_images WHERE post_id = ?";
        List<PostImage> postImages = jdbcTemplate.query(sql, postImageRowMapper, postId);
        return postImages.isEmpty() ? Optional.empty() : Optional.of(postImages.get(0));
    }

    public PostImage save(PostImage postImage) {
        Optional<PostImage> existing = findByPostId(postImage.getPostId());
        if (existing.isEmpty()) {
            String sql = "INSERT INTO post_images (post_id, data, content_type, size_bytes, filename) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, postImage.getPostId(), postImage.getData(),
                               postImage.getContentType(), postImage.getSizeBytes(), postImage.getFilename());
        } else {
            String sql = "UPDATE post_images SET data = ?, content_type = ?, size_bytes = ?, filename = ? WHERE post_id = ?";
            jdbcTemplate.update(sql, postImage.getData(), postImage.getContentType(),
                               postImage.getSizeBytes(), postImage.getFilename(), postImage.getPostId());
        }
        return postImage;
    }

    public void delete(PostImage postImage) {
        String sql = "DELETE FROM post_images WHERE post_id = ?";
        jdbcTemplate.update(sql, postImage.getPostId());
    }
}
