package com.my.blog.project.myblogonboot.myblog.dto.post;

import java.util.List;

public record PostRequest(
        String title,
        String text,
        List<String> tags
) {
}
