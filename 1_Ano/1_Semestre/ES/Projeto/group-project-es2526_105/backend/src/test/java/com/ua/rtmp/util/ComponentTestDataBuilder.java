package com.ua.rtmp.util;

import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.Vulnerability;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ComponentTestDataBuilder {

    private UUID id;
    private String name = "Default Component";
    private String description = "Default component description";
    private ThreatModel threatModel;
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    public static ComponentTestDataBuilder aComponent() {
        return new ComponentTestDataBuilder();
    }

    public static ComponentTestDataBuilder aWebServerComponent() {
        return new ComponentTestDataBuilder()
                .withName("Web Server")
                .withDescription("Handles HTTP requests and serves web content");
    }

    public static ComponentTestDataBuilder aDatabaseComponent() {
        return new ComponentTestDataBuilder()
                .withName("Database Server")
                .withDescription("Stores and manages application data");
    }

    public static ComponentTestDataBuilder anApiGatewayComponent() {
        return new ComponentTestDataBuilder()
                .withName("API Gateway")
                .withDescription("Routes API requests to microservices");
    }

    public static ComponentTestDataBuilder anAuthenticationServiceComponent() {
        return new ComponentTestDataBuilder()
                .withName("Authentication Service")
                .withDescription("Manages user authentication and authorization");
    }

    public ComponentTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ComponentTestDataBuilder withRandomId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public ComponentTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ComponentTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ComponentTestDataBuilder withThreatModel(ThreatModel threatModel) {
        this.threatModel = threatModel;
        return this;
    }

    public ComponentTestDataBuilder withVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
        return this;
    }

    public ComponentTestDataBuilder withNullDescription() {
        this.description = null;
        return this;
    }

    public ComponentTestDataBuilder withMaxLengthName() {
        this.name = "A".repeat(255);
        return this;
    }

    public ComponentTestDataBuilder withTooLongName() {
        this.name = "A".repeat(256);
        return this;
    }

    public ComponentTestDataBuilder withBlankName() {
        this.name = "";
        return this;
    }

    public ComponentTestDataBuilder withNullName() {
        this.name = null;
        return this;
    }

    public Component build() {
        Component component = new Component();
        component.setId(id);
        component.setName(name);
        component.setDescription(description);
        component.setThreatModel(threatModel);
        component.setVulnerabilities(vulnerabilities);
        return component;
    }

    public Component buildWithConstructor() {
        return new Component(id, name, description, threatModel, vulnerabilities, new java.util.ArrayList<>());
    }
}
