package com.ua.rtmp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "Component",
    description = "Represents a component within a threat model. " +
                  "Contains details such as unique ID, name, description, and reference to the parent threat model."
)
public class Component {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(
        description = "Unique identifier for the component.",
        example = "d290f1ee-6c54-4b01-90e6-d701748f0851",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    @Schema(
        description = "Name of the component.",
        example = "Web Server",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 255
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    @Schema(
        description = "Detailed description of the component.",
        example = "Handles HTTP requests and serves web content."
    )
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_model_id", nullable = false)
    @JsonBackReference
    @Schema(
        description = "Reference to the parent threat model.",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private ThreatModel threatModel;

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ComponentComment> comments = new ArrayList<>();

    public void addVulnerability(Vulnerability vulnerability) {
        vulnerabilities.add(vulnerability);
        vulnerability.setComponent(this);
    }

    public void removeVulnerability(Vulnerability vulnerability) {
        vulnerabilities.remove(vulnerability);
        vulnerability.setComponent(null);
    }
}