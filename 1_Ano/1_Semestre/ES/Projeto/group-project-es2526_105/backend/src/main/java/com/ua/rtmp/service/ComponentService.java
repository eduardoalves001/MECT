package com.ua.rtmp.service;

import com.ua.rtmp.exception.DuplicateResourceException;
import com.ua.rtmp.exception.ResourceNotFoundException;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.repository.ComponentRepository;
import com.ua.rtmp.repository.ThreatModelRepository;
import com.ua.rtmp.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ThreatModelRepository threatModelRepository;

    @Transactional(readOnly = true)
    public List<Component> getAllComponentsByThreatModelId(UUID threatModelId) {
        log.info("getAllComponentsByThreatModelId started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("READ", "COMPONENT", null);
        
        List<Component> components = componentRepository.findByThreatModelId(threatModelId);
        log.info("getAllComponentsByThreatModelId completed successfully: threatModelId={}, count={}", threatModelId, components.size());
        return components;
    }

    @Transactional(readOnly = true)
    public List<Component> searchComponents(UUID threatModelId, String search) {
        log.info("searchComponents started: threatModelId={}, search={}", threatModelId, search);
        LoggingUtil.setOperationContext("READ", "COMPONENT", null);
        
        List<Component> components;
        if (search == null || search.trim().isEmpty()) {
            log.debug("Empty search parameter, returning all components for threat model");
            components = componentRepository.findByThreatModelId(threatModelId);
        } else {
            log.debug("Searching components with term: {}", search.trim());
            components = componentRepository.searchComponentsByThreatModelId(threatModelId, search.trim());
        }
        
        log.info("searchComponents completed successfully: threatModelId={}, search={}, count={}", threatModelId, search, components.size());
        return components;
    }

    @Transactional(readOnly = true)
    public Component getComponentById(UUID id) {
        log.info("getComponentById started: id={}", id);
        LoggingUtil.setOperationContext("READ", "COMPONENT", id.toString());
        
        Component component = findComponentById(id);
        log.info("getComponentById completed successfully: id={}, name={}", id, component.getName());
        return component;
    }

    @Transactional
    public Component createComponent(UUID threatModelId, String name, String description) {
        log.info("createComponent started: threatModelId={}, name={}", threatModelId, name);
        LoggingUtil.setOperationContext("CREATE", "COMPONENT", null);
        
        ThreatModel threatModel = threatModelRepository.findById(threatModelId)
                .orElseThrow(() -> {
                    log.error("createComponent failed: threat model not found: threatModelId={}", threatModelId);
                    return new ResourceNotFoundException("ThreatModel not found with id: " + threatModelId);
                });

        if (componentRepository.existsByNameAndThreatModelId(name, threatModelId)) {
            log.error("createComponent failed: duplicate component name: name={}, threatModelId={}", name, threatModelId);
            throw new DuplicateResourceException("Component with name '" + name + "' already exists in this threat model");
        }

        Component component = new Component();
        component.setName(name);
        component.setDescription(description);
        threatModel.addComponent(component);

        Component saved = componentRepository.save(component);
        if (saved.getId() != null) {
            LoggingUtil.setOperationContext("CREATE", "COMPONENT", saved.getId().toString());
        }
        log.info("createComponent completed successfully: id={}, name={}, threatModelId={}", saved.getId(), saved.getName(), threatModelId);
        return saved;
    }

    @Transactional
    public Component updateComponent(UUID id, String name, String description) {
        log.info("updateComponent started: id={}, newName={}", id, name);
        LoggingUtil.setOperationContext("UPDATE", "COMPONENT", id.toString());
        
        Component component = findComponentById(id);
        log.debug("Found existing component: id={}, oldName={}", id, component.getName());
        
        if (!component.getName().equals(name) &&
            componentRepository.existsByNameAndThreatModelId(name, component.getThreatModel().getId())) {
            log.error("updateComponent failed: duplicate component name: id={}, newName={}", id, name);
            throw new DuplicateResourceException("Component with name '" + name + "' already exists in this threat model");
        }

        component.setName(name);
        component.setDescription(description);

        Component saved = componentRepository.save(component);
        log.info("updateComponent completed successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void deleteComponent(UUID id) {
        log.info("deleteComponent started: id={}", id);
        LoggingUtil.setOperationContext("DELETE", "COMPONENT", id.toString());
        
        Component component = findComponentById(id);
        log.debug("Deleting component: id={}, name={}", id, component.getName());
        
        component.getThreatModel().removeComponent(component);
        componentRepository.delete(component);
        log.info("deleteComponent completed successfully: id={}", id);
    }

    private Component findComponentById(UUID id) {
        return componentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Component not found: id={}", id);
                    return new ResourceNotFoundException("Component not found with id: " + id);
                });
    }
}