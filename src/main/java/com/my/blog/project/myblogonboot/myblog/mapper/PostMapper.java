package com.my.blog.project.myblogonboot.myblog.mapper;

import com.my.blog.project.myblogonboot.myblog.dto.post.PostRequest;
import com.my.blog.project.myblogonboot.myblog.dto.post.PostResponse;
import com.my.blog.project.myblogonboot.myblog.entity.Post;
import com.my.blog.project.myblogonboot.myblog.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy =
        ReportingPolicy.IGNORE)
public interface PostMapper {

    Post toEntity(PostRequest postRequest);

    PostResponse toDto(Post post);

    default Set<Tag> map(List<String> list) {
        return list == null ? new HashSet<>() :
                new HashSet<>(
                        list.stream()
                                .map(Tag::new)
                                .collect(Collectors.toSet())
                );
    }

    default List<String> map(Set<Tag> tags) {
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }
}
