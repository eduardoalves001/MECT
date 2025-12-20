package com.ua.rtmp.repository;

import com.ua.rtmp.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID> {
    List<Component> findByThreatModelId(UUID threatModelId);
    boolean existsByNameAndThreatModelId(String name, UUID threatModelId);
    Integer countByThreatModelId(UUID threatModelId);
    
    @Query("SELECT c FROM Component c WHERE c.threatModel.id = :threatModelId " +
           "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Component> searchComponentsByThreatModelId(
            @Param("threatModelId") UUID threatModelId,
            @Param("search") String search
    );
}
