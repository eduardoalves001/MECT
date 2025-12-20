package com.ua.rtmp.mapper;

import com.ua.rtmp.dto.response.NotificationResponseDTO;
import com.ua.rtmp.model.Notification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    public NotificationResponseDTO toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationResponseDTO response = new NotificationResponseDTO();
        response.setId(notification.getId());
        response.setRecipientUserId(notification.getRecipientUserId());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());

        UUID commentId = null;
        if (notification.getComponentComment() != null) {
            commentId = notification.getComponentComment().getId();
        } else if (notification.getVulnerabilityComment() != null) {
            commentId = notification.getVulnerabilityComment().getId();
        }
        response.setCommentId(commentId);

        return response;
    }

    public List<NotificationResponseDTO> toResponseList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
