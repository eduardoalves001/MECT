package com.ua.rtmp.service;

import com.ua.rtmp.exception.ResourceNotFoundException;
import com.ua.rtmp.model.ComponentComment;
import com.ua.rtmp.model.VulnerabilityComment;
import com.ua.rtmp.model.Notification;
import com.ua.rtmp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createComponentCommentNotification(ComponentComment comment) {
        log.info("createComponentCommentNotification: commentId={}, authorUserId={}", 
                comment.getId(), comment.getAuthorUserId());

        String componentName = comment.getComponent().getName();
        String threatModelName = comment.getComponent().getThreatModel().getName();
        String entityDescription = String.format("component '%s' in '%s'",
                componentName, threatModelName);

        if (comment.getParentComment() != null) {
            String recipientUserId = comment.getParentComment().getAuthorUserId();
            
            if (recipientUserId.equals(comment.getAuthorUserId())) {
                log.debug("Skipping notification - comment author is replying to themselves");
                return;
            }

            String message = String.format("%s replied to your comment on %s",
                    comment.getAuthorUsername(), entityDescription);

            Notification notification = new Notification();
            notification.setRecipientUserId(recipientUserId);
            notification.setMessage(message);
            notification.setType("COMMENT_REPLY");
            notification.setComponentComment(comment);
            notification.setIsRead(false);

            notificationRepository.save(notification);
            log.info("Component comment reply notification created: recipientUserId={}", recipientUserId);
        } else {
            log.info("New top-level component comment created: message={} commented on {}", 
                    comment.getAuthorUsername(), entityDescription);
        }
    }

    @Transactional
    public void createVulnerabilityCommentNotification(VulnerabilityComment comment) {
        log.info("createVulnerabilityCommentNotification: commentId={}, authorUserId={}", 
                comment.getId(), comment.getAuthorUserId());

        String componentName = comment.getVulnerability().getComponent().getName();
        String threatModelName = comment.getVulnerability().getComponent().getThreatModel().getName();
        String threatName = comment.getVulnerability().getThreat().getName();
        String entityDescription = String.format("vulnerability '%s' on component '%s' in '%s'",
                threatName, componentName, threatModelName);

        if (comment.getParentComment() != null) {
            String recipientUserId = comment.getParentComment().getAuthorUserId();
            
            if (recipientUserId.equals(comment.getAuthorUserId())) {
                log.debug("Skipping notification - comment author is replying to themselves");
                return;
            }

            String message = String.format("%s replied to your comment on %s",
                    comment.getAuthorUsername(), entityDescription);

            Notification notification = new Notification();
            notification.setRecipientUserId(recipientUserId);
            notification.setMessage(message);
            notification.setType("COMMENT_REPLY");
            notification.setVulnerabilityComment(comment);
            notification.setIsRead(false);

            notificationRepository.save(notification);
            log.info("Vulnerability comment reply notification created: recipientUserId={}", recipientUserId);
        } else {
            log.info("New top-level vulnerability comment created: message={} commented on {}", 
                    comment.getAuthorUsername(), entityDescription);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String userId) {
        log.info("getNotificationsForUser: userId={}", userId);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(String userId) {
        log.info("getUnreadNotificationsForUser: userId={}", userId);
        return notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markNotificationAsRead(UUID notificationId, String userId) {
        log.info("markNotificationAsRead: notificationId={}, userId={}", notificationId, userId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to mark this notification as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification marked as read: id={}", notificationId);
    }

    @Transactional
    public void markAllNotificationsAsRead(String userId) {
        log.info("markAllNotificationsAsRead: userId={}", userId);
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("All notifications marked as read for userId={}", userId);
    }
}
