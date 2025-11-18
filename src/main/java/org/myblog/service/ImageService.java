package org.myblog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.myblog.entity.Post;
import org.myblog.entity.PostImage;
import org.myblog.repository.PostImageRepository;
import org.myblog.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {

    private final PostImageRepository postImageRepository;
    private final PostRepository postRepository;

    @Transactional
    public ResponseEntity<Void> uploadImage(Long postId, MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        long size = image.getSize();
        if (size > 100L * 1024 * 1024) {/* 100MB limit for DB images, if we will need bigger size
                                          we should consider different approach of storing files */

            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        String contentType = image.getContentType();

        Post post = postRepository.findPostById(postId);
        if (post == null) {
            log.warn("Could not update image because post with id {} does not exist", postId);
            return ResponseEntity.notFound().build();
        }

        PostImage postImage = postImageRepository.findByPostId(postId).orElseGet(() -> {
            PostImage pi = new PostImage();
            pi.setPostId(postId);
            return pi;
        });
        postImage.setData(image.getBytes());
        postImage.setContentType(contentType);
        postImage.setSizeBytes(size);
        postImage.setFilename(image.getOriginalFilename());
        postImageRepository.save(postImage);

        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public Optional<PostImage> getPostImage(Long id) {
        return postImageRepository.findByPostId(id);
    }
}
