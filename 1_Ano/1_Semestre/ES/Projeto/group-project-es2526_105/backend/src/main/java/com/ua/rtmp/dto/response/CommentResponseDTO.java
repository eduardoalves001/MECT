package com.ua.rtmp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    private UUID id;
    private String content;
    private String authorUserId;
    private String authorUsername;
    private UUID vulnerabilityId;
    private UUID componentId;
    private UUID parentCommentId;
    private List<CommentResponseDTO> replies = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
