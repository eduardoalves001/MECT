package com.ua.rtmp.repository;

import com.ua.rtmp.model.ComponentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentCommentRepository extends JpaRepository<ComponentComment, UUID> {

    @Query("SELECT c FROM ComponentComment c WHERE c.component.id = :componentId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<ComponentComment> findTopLevelCommentsByComponentId(@Param("componentId") UUID componentId);

    @Query("SELECT c FROM ComponentComment c WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    List<ComponentComment> findRepliesByParentId(@Param("parentId") UUID parentId);
}
