package org.myblog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.myblog.dto.comment.CommentResponse;
import org.myblog.entity.Comment;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy =
        ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(source = "post.id", target = "postId")
    CommentResponse toDto(Comment comment);

    List<CommentResponse> toDtoList(List<Comment> comments);
}
