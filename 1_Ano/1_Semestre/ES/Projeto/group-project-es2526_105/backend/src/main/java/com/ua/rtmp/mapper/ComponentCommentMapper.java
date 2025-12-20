package com.ua.rtmp.mapper;

import com.ua.rtmp.dto.response.CommentResponseDTO;
import com.ua.rtmp.model.ComponentComment;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ComponentCommentMapper {

    private final ModelMapper modelMapper;

    public CommentResponseDTO toResponse(ComponentComment comment) {
        if (comment == null) {
            return null;
        }

        CommentResponseDTO response = modelMapper.map(comment, CommentResponseDTO.class);

        if (comment.getComponent() != null) {
            response.setComponentId(comment.getComponent().getId());
        }

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getId());
        }

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentResponseDTO> replyResponses = comment.getReplies().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            response.setReplies(replyResponses);
        }

        return response;
    }

    public List<CommentResponseDTO> toResponseList(List<ComponentComment> comments) {
        return comments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
