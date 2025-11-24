package com.my.blog.project.myblogonboot.myblog.dto.post;

import java.util.List;

public record PostResponse(
        long id,
        String title,
        String text,
        List<String> tags,
        int likesCount,
        int commentsCount
) {
}
