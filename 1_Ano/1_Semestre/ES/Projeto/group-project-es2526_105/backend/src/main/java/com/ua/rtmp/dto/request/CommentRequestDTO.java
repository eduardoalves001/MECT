package com.ua.rtmp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDTO {

    @NotBlank(message = "Content is required")
    private String content;

    private UUID vulnerabilityId;

    private UUID componentId;

    private UUID parentCommentId;
}
