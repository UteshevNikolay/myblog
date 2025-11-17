package org.myblog.dto.comment;

public record CommentResponse(
        long id,
        String text,
        long postId
) {
}
