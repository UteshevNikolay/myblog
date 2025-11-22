package com.my.blog.project.myblogonboot.myblog.dto.search;

import java.util.List;

public record SearchQuery(boolean hasQuery,
                          String searchQuery,
                          boolean hasTags,
                          List<String> tagsFromSearch) {
}
