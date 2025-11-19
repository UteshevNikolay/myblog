package org.myblog.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostImage {

    private Long postId;
    private byte[] data;
    private String contentType;
    private Long sizeBytes;
    private String filename;
}
