package com.my.blog.project.myblogonboot.myblog.mapper;

import com.my.blog.project.myblogonboot.myblog.dto.comment.CommentResponse;
import com.my.blog.project.myblogonboot.myblog.entity.Comment;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy =
        ReportingPolicy.IGNORE)
public interface CommentMapper {

    CommentResponse toDto(Comment comment);

    List<CommentResponse> toDtoList(List<Comment> comments);
}
