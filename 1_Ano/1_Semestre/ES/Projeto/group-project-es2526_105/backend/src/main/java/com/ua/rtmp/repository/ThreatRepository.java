package com.ua.rtmp.repository;

import com.ua.rtmp.model.Threat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ThreatRepository extends JpaRepository<Threat, UUID> {
    boolean existsByName(String name);
}
