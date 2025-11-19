package org.myblog.service;

import org.myblog.dto.search.SearchQuery;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchQueryBuilder {

    public SearchQuery buildSearchQuery(String searchRequest) {
        String normalizedSearchRequest = (searchRequest == null) ? "" : searchRequest.trim();

        boolean hasQuery;
        String searchQuery;
        boolean hasTags;
        List<String> tagsFromSearch = Collections.emptyList();

        if (normalizedSearchRequest.isBlank()) {
            hasQuery = false;
            hasTags = false;
            searchQuery = "";
        } else {
            Map<Boolean, List<String>> parts =
                    getSearchQueryAndTagsFromRequest(normalizedSearchRequest);

            tagsFromSearch = parts.getOrDefault(true, List.of()).stream()
                    .map(s -> s.substring(1))
                    .map(String::toLowerCase)
                    .distinct()
                    .toList();

            searchQuery = String.join(" ", parts.getOrDefault(false,
                    List.of()));

            hasQuery = !searchQuery.isBlank();
            hasTags = !tagsFromSearch.isEmpty();
        }

        return new SearchQuery(hasQuery, searchQuery, hasTags, tagsFromSearch);
    }

    private Map<Boolean, List<String>> getSearchQueryAndTagsFromRequest(String search) {
        return Arrays.stream(search.trim().replaceAll("\\s+", " ").split("\\s"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.partitioningBy(s -> s.startsWith("#")));
    }
}
