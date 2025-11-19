package org.myblog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    private Long id;
    private String title;
    private String text;
    private Set<Tag> tags = new HashSet<>();
    private Integer likesCount = 0;
    private Integer commentsCount = 0;
    private PostImage image;
}
