package com.my.blog.project.myblogonboot.myblog.dto.comment;

public record CommentResponse(
        long id,
        String text,
        long postId
) {
}
