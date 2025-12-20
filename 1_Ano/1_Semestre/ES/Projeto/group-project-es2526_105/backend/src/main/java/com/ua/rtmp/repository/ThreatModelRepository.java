package com.ua.rtmp.repository;

import com.ua.rtmp.model.ThreatModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreatModelRepository extends JpaRepository<ThreatModel, UUID> {

    boolean existsByName(String name);
    
    @Query("SELECT tm FROM ThreatModel tm WHERE " +
           "(:searchPattern IS NULL OR LOWER(tm.name) LIKE :searchPattern " +
           "OR LOWER(tm.description) LIKE :searchPattern)")
    List<ThreatModel> searchThreatModels(@Param("searchPattern") String searchPattern);
    
    // Find threat models with active threats (vulnerabilities that are not mitigated or closed)
    @Query("SELECT DISTINCT tm FROM ThreatModel tm " +
           "JOIN tm.components c " +
           "JOIN c.vulnerabilities v " +
           "WHERE v.status IN ('IDENTIFIED', 'ANALYSED', 'IN_PROGRESS') " +
           "AND (:searchPattern IS NULL OR LOWER(tm.name) LIKE :searchPattern " +
           "OR LOWER(tm.description) LIKE :searchPattern)")
    List<ThreatModel> findThreatModelsWithActiveThreats(@Param("searchPattern") String searchPattern);
    
    // Find threat models with high-risk active threats (risk score >= threshold and not mitigated)
    @Query("SELECT DISTINCT tm FROM ThreatModel tm " +
           "JOIN tm.components c " +
           "JOIN c.vulnerabilities v " +
           "WHERE v.riskScore >= :riskThreshold " +
           "AND v.status IN ('IDENTIFIED', 'ANALYSED', 'IN_PROGRESS') " +
           "AND (:searchPattern IS NULL OR LOWER(tm.name) LIKE :searchPattern " +
           "OR LOWER(tm.description) LIKE :searchPattern)")
    List<ThreatModel> findThreatModelsWithHighRiskActiveThreats(@Param("searchPattern") String searchPattern, @Param("riskThreshold") Integer riskThreshold);
    
    // Find threat models with mitigated threats (vulnerabilities that are mitigated or closed)
    @Query("SELECT DISTINCT tm FROM ThreatModel tm " +
           "JOIN tm.components c " +
           "JOIN c.vulnerabilities v " +
           "WHERE v.status IN ('MITIGATED', 'CLOSED') " +
           "AND (:searchPattern IS NULL OR LOWER(tm.name) LIKE :searchPattern " +
           "OR LOWER(tm.description) LIKE :searchPattern)")
    List<ThreatModel> findThreatModelsWithMitigatedThreats(@Param("searchPattern") String searchPattern);
}
