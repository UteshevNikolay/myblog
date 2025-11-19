package org.myblog.repository;

import lombok.RequiredArgsConstructor;
import org.myblog.entity.Post;
import org.myblog.entity.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setTitle(rs.getString("title"));
        post.setText(rs.getString("text"));
        post.setLikesCount(rs.getInt("likes_count"));
        post.setCommentsCount(rs.getInt("comments_count"));
        post.setTags(new HashSet<>());
        return post;
    };

    public PageResult<Post> searchByTitleAndAllTagNames(String query, boolean hasQuery,
                                                        List<String> tagNames, boolean hasTags,
                                                        long tagsCount, int page, int size) {
        // Convert 1-based page number to 0-based for SQL OFFSET
        // Page 1 -> offset 0, Page 2 -> offset 10, etc.
        int zeroBasedPage = Math.max(0, page - 1);

        StringBuilder sqlBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // Build the base query
        sqlBuilder.append("""
            SELECT p.id, p.title, p.text, p.likes_count, p.comments_count
            FROM posts p
            """);

        if (hasTags) {
            sqlBuilder.append("INNER JOIN posts_tags pt ON p.id = pt.post_id ");
            sqlBuilder.append("INNER JOIN tags t ON pt.tag_id = t.id ");
        }

        // Build WHERE clause
        List<String> whereConditions = new ArrayList<>();
        if (hasQuery) {
            whereConditions.add("LOWER(p.title) LIKE LOWER(?)");
            params.add("%" + query + "%");
        }
        if (hasTags) {
            String placeholders = tagNames.stream().map(n -> "?").collect(Collectors.joining(","));
            whereConditions.add("LOWER(t.name) IN (" + placeholders + ")");
            params.addAll(tagNames.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }

        if (!whereConditions.isEmpty()) {
            sqlBuilder.append("WHERE ").append(String.join(" AND ", whereConditions)).append(" ");
        }

        // Add GROUP BY and HAVING for tag matching
        if (hasTags) {
            sqlBuilder.append("GROUP BY p.id, p.title, p.text, p.likes_count, p.comments_count ");
            sqlBuilder.append("HAVING COUNT(DISTINCT t.id) = ? ");
            params.add(tagsCount);
        }

        // Count total results
        String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder + ") AS count_query";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Add pagination (use 0-based page for SQL OFFSET)
        sqlBuilder.append("ORDER BY p.id DESC ");
        sqlBuilder.append("LIMIT ? OFFSET ?");
        params.add(size);
        params.add(zeroBasedPage * size);

        List<Post> posts = jdbcTemplate.query(sqlBuilder.toString(), postRowMapper, params.toArray());
        loadTagsForPosts(posts);

        // Return with original 1-based page number
        return new PageResult<>(posts, total == null ? 0 : total, page, size);
    }

    public Post findPostById(Long id) {
        String sql = "SELECT id, title, text, likes_count, comments_count FROM posts WHERE id = ?";
        List<Post> posts = jdbcTemplate.query(sql, postRowMapper, id);
        if (posts.isEmpty()) {
            return null;
        }
        Post post = posts.get(0);
        loadTagsForPost(post);
        return post;
    }

    public Post save(Post post) {
        if (post.getId() == null) {
            String sql = "INSERT INTO posts (title, text, likes_count, comments_count) VALUES (?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, post.getTitle());
                ps.setString(2, post.getText());
                ps.setInt(3, post.getLikesCount() != null ? post.getLikesCount() : 0);
                ps.setInt(4, post.getCommentsCount() != null ? post.getCommentsCount() : 0);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            post.setId(key != null ? key.longValue() : null);
        } else {
            String sql = "UPDATE posts SET title = ?, text = ?, likes_count = ?, comments_count = ? WHERE id = ?";
            jdbcTemplate.update(sql, post.getTitle(), post.getText(),
                               post.getLikesCount(), post.getCommentsCount(), post.getId());
        }

        // Update tags relationship
        saveTags(post);
        return post;
    }

    private void saveTags(Post post) {
        // Delete existing tag relationships
        String deleteSql = "DELETE FROM posts_tags WHERE post_id = ?";
        jdbcTemplate.update(deleteSql, post.getId());

        // Insert new tag relationships
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            String insertSql = "INSERT INTO posts_tags (post_id, tag_id) VALUES (?, ?)";
            for (Tag tag : post.getTags()) {
                jdbcTemplate.update(insertSql, post.getId(), tag.getId());
            }
        }
    }

    public void delete(Post post) {
        deleteById(post.getId());
    }

    public void deleteById(Long id) {
        // Delete tag relationships first
        String deleteTagsSql = "DELETE FROM posts_tags WHERE post_id = ?";
        jdbcTemplate.update(deleteTagsSql, id);

        // Delete post
        String sql = "DELETE FROM posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void incrementLikes(long postId) {
        String sql = "UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?";
        jdbcTemplate.update(sql, postId);
    }

    public Long getCurrentLikes(Long postId) {
        String sql = "SELECT likes_count FROM posts WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, postId);
    }

    private void loadTagsForPost(Post post) {
        String sql = """
            SELECT t.id, t.name
            FROM tags t
            INNER JOIN posts_tags pt ON t.id = pt.tag_id
            WHERE pt.post_id = ?
            """;
        List<Tag> tags = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Tag tag = new Tag();
            tag.setId(rs.getLong("id"));
            tag.setName(rs.getString("name"));
            return tag;
        }, post.getId());
        post.setTags(new HashSet<>(tags));
    }

    private void loadTagsForPosts(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }

        String postIds = posts.stream()
            .map(p -> String.valueOf(p.getId()))
            .collect(Collectors.joining(","));

        String sql = """
            SELECT pt.post_id, t.id, t.name
            FROM tags t
            INNER JOIN posts_tags pt ON t.id = pt.tag_id
            WHERE pt.post_id IN (""" + postIds + ")";

        Map<Long, Set<Tag>> postTagsMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long postId = rs.getLong("post_id");
            Tag tag = new Tag();
            tag.setId(rs.getLong("id"));
            tag.setName(rs.getString("name"));
            postTagsMap.computeIfAbsent(postId, k -> new HashSet<>()).add(tag);
        });

        for (Post post : posts) {
            post.setTags(postTagsMap.getOrDefault(post.getId(), new HashSet<>()));
        }
    }

    public static class PageResult<T> {
        private final List<T> content;
        private final long totalElements;
        private final int pageNumber;
        private final int pageSize;

        public PageResult(List<T> content, long totalElements, int pageNumber, int pageSize) {
            this.content = content;
            this.totalElements = totalElements;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
        }

        public List<T> getContent() {
            return content;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return (int) Math.ceil((double) totalElements / pageSize);
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
