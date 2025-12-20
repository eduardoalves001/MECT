package com.ua.rtmp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private UUID id;
    private String recipientUserId;
    private String message;
    private String type;
    private UUID commentId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
