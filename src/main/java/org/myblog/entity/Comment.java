package org.myblog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {

    private Long id;
    private Long postId;
    private String text;

    public Comment(Long postId, String text) {
        this.postId = postId;
        this.text = text;
    }
}
