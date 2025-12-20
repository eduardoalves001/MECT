package com.ua.rtmp.service;

import com.ua.rtmp.model.Threat;
import com.ua.rtmp.repository.ThreatRepository;
import com.ua.rtmp.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatService {

    private final ThreatRepository threatRepository;

    @Transactional(readOnly = true)
    public List<Threat> getAllThreats() {
        log.info("getAllThreats started");
        LoggingUtil.setOperationContext("READ", "THREAT", null);
        
        List<Threat> threats = threatRepository.findAll();
        log.info("getAllThreats completed successfully: count={}", threats.size());
        return threats;
    }

    @Transactional(readOnly = true)
    public Optional<Threat> getThreatById(UUID id) {
        log.info("getThreatById started: id={}", id);
        if (id != null) {
            LoggingUtil.setOperationContext("READ", "THREAT", id.toString());
        }
        
        Optional<Threat> threat = threatRepository.findById(id);
        if (threat.isPresent()) {
            log.info("getThreatById completed successfully: id={}, name={}, category={}", 
                    id, threat.get().getName(), threat.get().getCategory());
        } else {
            log.warn("getThreatById: threat not found: id={}", id);
        }
        return threat;
    }
}
