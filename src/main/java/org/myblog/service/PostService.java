package org.myblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.myblog.dto.post.PostResponse;
import org.myblog.dto.post.PostRequest;
import org.myblog.dto.post.PostsResponse;
import org.myblog.dto.search.SearchQuery;
import org.myblog.entity.Comment;
import org.myblog.entity.Post;
import org.myblog.entity.PostImage;
import org.myblog.entity.Tag;
import org.myblog.mapper.PostMapper;
import org.myblog.repository.CommentRepository;
import org.myblog.repository.PostImageRepository;
import org.myblog.repository.PostRepository;
import org.myblog.repository.TagRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;
    private final SearchQueryBuilder queryBuilder;

    @Transactional(readOnly = true)
    public PostsResponse getPosts(String searchRequest, int pageNumber, int pageSize) {

        SearchQuery searchQuery = queryBuilder.buildSearchQuery(searchRequest);

        // Ensure valid page number and size (1-based pagination)
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize <= 0) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        long tagsCount = searchQuery.tagsFromSearch().size();

        // Repository now accepts 1-based page numbers
        PostRepository.PageResult<Post> page = postRepository.searchByTitleAndAllTagNames(
                searchQuery.searchQuery(),
                searchQuery.hasQuery(),
                searchQuery.tagsFromSearch(),
                searchQuery.hasTags(),
                tagsCount,
                pageNumber,
                pageSize);

        List<PostResponse> items = page.getContent().stream()
                .map(postMapper::toDto)
                .collect(java.util.stream.Collectors.toList());

        int totalPages = page.getTotalPages();
        boolean hasPrev = pageNumber > 1 && totalPages > 0;
        boolean hasNext = pageNumber < totalPages;

        return new PostsResponse(items, hasPrev, hasNext, totalPages);
    }

    @Transactional
    public PostResponse savePost(PostRequest postRequest) {
        Post post = postMapper.toEntity(postRequest);
        Set<Tag> managedTags = new HashSet<>();
        for (Tag tag : post.getTags()) {
            Tag managedTag = tagRepository.findByNameIgnoreCase(tag.getName())
                    .orElseGet(() -> tagRepository.save(tag)); // persist new tag
            managedTags.add(managedTag);
        }
        post.setTags(managedTags);
        Post save = postRepository.save(post);

        return postMapper.toDto(save);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest postRequest) {
        Post post = postRepository.findPostById(id);
        if (post == null) {
            return null;
        }

        post.setTitle(postRequest.title());
        post.setText(postRequest.text());

        Set<Tag> managedTags = new HashSet<>();
        if (postRequest.tags() != null) {
            for (String tagName : postRequest.tags()) {
                Tag managedTag = tagRepository.findByNameIgnoreCase(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName)));
                managedTags.add(managedTag);
            }
        }
        post.setTags(managedTags);

        return postMapper.toDto(postRepository.save(post));
    }

    @Transactional
    public ResponseEntity<Void> deletePost(Long postId) {
        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Cannot delete post with id {} because it does not exist", postId);
            return ResponseEntity.notFound().build();
        }

        // Delete all comments associated with this post
        List<Comment> comments = commentRepository.findByPostIdOrderByIdAsc(postId);
        if (!comments.isEmpty()) {
            commentRepository.deleteAll(comments);
            log.info("Deleted {} comments for post {}", comments.size(), postId);
        }

        // Delete post image if exists
        Optional<PostImage> postImage = postImageRepository.findByPostId(postId);
        postImage.ifPresent(image -> {
            postImageRepository.delete(image);
            log.info("Deleted image for post {}", postId);
        });

        // Delete the post
        postRepository.delete(post);
        log.info("Deleted post with id {}", postId);

        return ResponseEntity.noContent().build();
    }

    public PostResponse getPostById(Long postId) {
        return postMapper.toDto(postRepository.findPostById(postId));
    }

    @Transactional
    public Long incrementLike(long postId) {
        postRepository.incrementLikes(postId);
        return postRepository.getCurrentLikes(postId);
    }
}
