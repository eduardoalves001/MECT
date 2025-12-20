package com.ua.rtmp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import com.ua.rtmp.model.enums.StrideCategory;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "threats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "Threat",
    description = "Represents a security threat within the system. " +
                  "Contains details such as unique ID, name, description, and STRIDE category."
)
public class Threat {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(
        description = "Unique identifier for the threat.",
        example = "a123f1ee-6c54-4b01-90e6-d701748f0851",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    @Schema(
        description = "Name of the threat.",
        example = "SQL Injection",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 255
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    @Schema(
        description = "Detailed description of the threat.",
        example = "An attacker can inject malicious SQL statements."
    )
    private String description;
   
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Schema(
        description = "STRIDE category of the threat.",
        example = "Spoofing"
    )
    private StrideCategory category;
}