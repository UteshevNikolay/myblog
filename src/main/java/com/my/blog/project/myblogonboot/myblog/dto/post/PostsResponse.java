package com.my.blog.project.myblogonboot.myblog.dto.post;

import java.util.List;

public record PostsResponse(List<PostResponse> posts, boolean hasPrev, boolean hasNext,
                            int lastPage) {

    public PostsResponse(List<PostResponse> posts) {
        this(posts, false, true, 0);
    }
}
