package org.myblog.repository;

import lombok.RequiredArgsConstructor;
import org.myblog.entity.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TagRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Tag> tagRowMapper = (rs, rowNum) -> {
        Tag tag = new Tag();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        return tag;
    };

    public Optional<Tag> findByNameIgnoreCase(String name) {
        String sql = "SELECT id, name FROM tags WHERE LOWER(name) = LOWER(?)";
        List<Tag> tags = jdbcTemplate.query(sql, tagRowMapper, name);
        return tags.isEmpty() ? Optional.empty() : Optional.of(tags.get(0));
    }

    public Tag save(Tag tag) {
        if (tag.getId() == null) {
            String sql = "INSERT INTO tags (name) VALUES (?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, tag.getName());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            tag.setId(key != null ? key.longValue() : null);
        } else {
            String sql = "UPDATE tags SET name = ? WHERE id = ?";
            jdbcTemplate.update(sql, tag.getName(), tag.getId());
        }
        return tag;
    }

    public Optional<Tag> findById(Long id) {
        String sql = "SELECT id, name FROM tags WHERE id = ?";
        List<Tag> tags = jdbcTemplate.query(sql, tagRowMapper, id);
        return tags.isEmpty() ? Optional.empty() : Optional.of(tags.get(0));
    }

    public List<Tag> findAll() {
        String sql = "SELECT id, name FROM tags";
        return jdbcTemplate.query(sql, tagRowMapper);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
