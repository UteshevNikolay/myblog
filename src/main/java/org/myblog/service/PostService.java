package org.myblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.myblog.dto.comment.CommentRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

        Pageable pageable = getPageRequest(pageNumber, pageSize);
        Page<Post> page;

        long tagsCount = searchQuery.tagsFromSearch().size();

        page = postRepository.searchByTitleAndAllTagNames(searchQuery.searchQuery(),
                searchQuery.hasQuery(),
                searchQuery.tagsFromSearch(),
                searchQuery.hasTags(), tagsCount, pageable);

        List<PostResponse> items = page.map(postMapper::toDto).getContent();

        int totalPages = page.getTotalPages();
        boolean hasPrev = pageable.getPageNumber() > 0 && totalPages > 0;
        boolean hasNext = pageable.getPageNumber() + 1 < totalPages;

        return new PostsResponse(items, hasPrev, hasNext, totalPages);
    }

    private Pageable getPageRequest(int pageNumber, int pageSize) {
        if (pageNumber < 0) pageNumber = 0;
        if (pageSize <= 0) pageSize = 20;
        if (pageSize > 100) pageSize = 100;

        int zeroBasedPageNumber = pageNumber - 1; // Spring Data uses 0-based pagination
        return PageRequest.of(zeroBasedPageNumber, pageSize);
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
