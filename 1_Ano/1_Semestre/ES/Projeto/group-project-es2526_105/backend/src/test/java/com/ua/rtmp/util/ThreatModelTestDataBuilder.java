package com.ua.rtmp.util;

import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ThreatModelTestDataBuilder {

    private UUID id;
    private String name = "Default Threat Model";
    private String description = "Default threat model description";
    private LocalDateTime createdAt;
    private List<Component> components = new ArrayList<>();

    public static ThreatModelTestDataBuilder aThreatModel() {
        return new ThreatModelTestDataBuilder();
    }

    public static ThreatModelTestDataBuilder aPaymentSystemThreatModel() {
        return new ThreatModelTestDataBuilder()
                .withName("Payment System Threat Model")
                .withDescription("Covers threats and mitigations for the payment processing system");
    }

    public static ThreatModelTestDataBuilder aWebApplicationThreatModel() {
        return new ThreatModelTestDataBuilder()
                .withName("Web Application Threat Model")
                .withDescription("Security threats for the web application layer");
    }

    public static ThreatModelTestDataBuilder anApiThreatModel() {
        return new ThreatModelTestDataBuilder()
                .withName("REST API Threat Model")
                .withDescription("API security threat analysis");
    }

    public ThreatModelTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ThreatModelTestDataBuilder withRandomId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public ThreatModelTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ThreatModelTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ThreatModelTestDataBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ThreatModelTestDataBuilder withComponents(List<Component> components) {
        this.components = components;
        return this;
    }

    public ThreatModelTestDataBuilder withNullDescription() {
        this.description = null;
        return this;
    }

    public ThreatModelTestDataBuilder withMaxLengthName() {
        this.name = "A".repeat(255);
        return this;
    }

    public ThreatModelTestDataBuilder withTooLongName() {
        this.name = "A".repeat(256);
        return this;
    }

    public ThreatModelTestDataBuilder withBlankName() {
        this.name = "";
        return this;
    }

    public ThreatModelTestDataBuilder withNullName() {
        this.name = null;
        return this;
    }

    public ThreatModel build() {
        ThreatModel threatModel = new ThreatModel();
        threatModel.setId(id);
        threatModel.setName(name);
        threatModel.setDescription(description);
        threatModel.setCreatedAt(createdAt);
        threatModel.setComponents(components);
        return threatModel;
    }

    // Use this method when you need to test the all-args constructor of ThreatModel directly,
    // such as for constructor coverage or serialization tests. For general test data creation,
    // prefer the build() method which uses the default constructor and setters.
    public ThreatModel buildWithConstructor() {
        return new ThreatModel(id, name, description, createdAt, components);
    }
}
