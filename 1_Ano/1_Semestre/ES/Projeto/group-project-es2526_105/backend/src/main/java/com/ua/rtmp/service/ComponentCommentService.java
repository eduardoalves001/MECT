package com.ua.rtmp.service;

import com.ua.rtmp.exception.ResourceNotFoundException;
import com.ua.rtmp.model.ComponentComment;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.repository.ComponentCommentRepository;
import com.ua.rtmp.repository.ComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentCommentService {

    private final ComponentCommentRepository componentCommentRepository;
    private final ComponentRepository componentRepository;
    private final NotificationService notificationService;

    @Transactional
    public ComponentComment createComment(String content, UUID componentId, UUID parentCommentId, 
                                         String authorUserId, String authorUsername) {
        log.info("createComponentComment: authorUserId={}, componentId={}", authorUserId, componentId);

        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Component not found with id: " + componentId));

        ComponentComment comment = new ComponentComment();
        comment.setContent(content);
        comment.setAuthorUserId(authorUserId);
        comment.setAuthorUsername(authorUsername);
        comment.setComponent(component);

        if (parentCommentId != null) {
            ComponentComment parentComment = componentCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent comment not found with id: " + parentCommentId));
            comment.setParentComment(parentComment);
            log.debug("Comment is a reply to parentCommentId={}", parentComment.getId());
        }

        ComponentComment savedComment = componentCommentRepository.save(comment);
        log.info("Component comment created successfully: id={}", savedComment.getId());

        notificationService.createComponentCommentNotification(savedComment);

        return savedComment;
    }

    @Transactional(readOnly = true)
    public List<ComponentComment> getCommentsByComponentId(UUID componentId) {
        log.info("getCommentsByComponentId: componentId={}", componentId);
        
        if (!componentRepository.existsById(componentId)) {
            throw new ResourceNotFoundException("Component not found with id: " + componentId);
        }

        List<ComponentComment> comments = componentCommentRepository.findTopLevelCommentsByComponentId(componentId);
        log.info("Found {} top-level comments for component: id={}", comments.size(), componentId);
        
        return comments;
    }

    @Transactional(readOnly = true)
    public ComponentComment getCommentById(UUID commentId) {
        log.info("getComponentCommentById: commentId={}", commentId);
        
        return componentCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component comment not found with id: " + commentId));
    }

    @Transactional
    public void deleteComment(UUID commentId, String userId) {
        log.info("deleteComponentComment: commentId={}, userId={}", commentId, userId);
        
        ComponentComment comment = componentCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component comment not found with id: " + commentId));

        if (!comment.getAuthorUserId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this comment");
        }

        componentCommentRepository.delete(comment);
        log.info("Component comment deleted successfully: id={}", commentId);
    }
}
