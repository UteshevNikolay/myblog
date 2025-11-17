package org.myblog.repository;

import org.myblog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.id = :tagId")
    List<Post> finPostsByTagId(@Param("tagId") Long tagId);

    // Optional simple search by title
    Page<Post> findByTitleContainingIgnoreCase(String query, Pageable pageable);

    // Search by title AND ALL of the provided tag names (case-insensitive, exact match); supports pagination
    // If hasTags=false, tag filter is ignored. If hasQuery=false, title filter is ignored.
    // Using GROUP BY + HAVING ensures ALL tag names must be present when tags are provided.
    @Query("""
            SELECT p
            FROM Post p
            JOIN p.tags t
            WHERE (:hasQuery = false OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:hasTags = false OR LOWER(t.name) IN :tagNames)
            GROUP BY p.id
            HAVING (:hasTags = false OR COUNT(DISTINCT t.id) = :tagsCount)
            """)
    Page<Post> searchByTitleAndAllTagNames(@Param("query") String query,
                                           @Param("hasQuery") boolean hasQuery,
                                           @Param("tagNames") List<String> tagNames,
                                           @Param("hasTags") boolean hasTags,
                                           @Param("tagsCount") long tagsCount,
                                           Pageable pageable);

    Post findPostById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikes(@Param("postId") long postId);

    @Query("SELECT p.likesCount FROM Post p WHERE p.id = :postId")
    Long getCurrentLikes(@Param("postId") Long postId);
}
