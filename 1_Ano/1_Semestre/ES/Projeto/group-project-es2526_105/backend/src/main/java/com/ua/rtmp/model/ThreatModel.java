package com.ua.rtmp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "threat_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "ThreatModel",
    description = "Represents a threat model containing information about potential security threats and their mitigations. " +
                  "Includes unique ID, name, description, creation timestamp, and associated components."
)
public class ThreatModel {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(
        description = "Unique identifier for the threat model.",
        example = "e123f1ee-6c54-4b01-90e6-d701748f0851",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false, unique = true)
    @Schema(
        description = "Name of the threat model.",
        example = "Payment System Threat Model",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 255
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    @Schema(
        description = "Detailed description of the threat model.",
        example = "Covers threats and mitigations for the payment processing system."
    )
    private String description;

    @Column(name = "created_at")
    @Schema(
        description = "Timestamp when the threat model was created.",
        example = "2025-10-14T09:30:00"
    )
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "threatModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Schema(
        description = "List of components associated with this threat model."
    )
    private List<Component> components = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void addComponent(Component component) {
        components.add(component);
        component.setThreatModel(this);
    }

    public void removeComponent(Component component) {
        components.remove(component);
        component.setThreatModel(null);
    }
}