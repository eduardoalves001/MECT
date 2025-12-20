package com.ua.rtmp.util;

import com.ua.rtmp.model.Threat;
import com.ua.rtmp.model.enums.StrideCategory;

import java.util.UUID;

public class ThreatTestDataBuilder {

    private UUID id;
    private String name = "Default Threat";
    private String description = "Default threat description";
    private StrideCategory category = StrideCategory.SPOOFING;

    public static ThreatTestDataBuilder aThreat() {
        return new ThreatTestDataBuilder();
    }

    public static ThreatTestDataBuilder aSqlInjectionThreat() {
        return new ThreatTestDataBuilder()
                .withName("SQL Injection")
                .withDescription("An attacker can inject malicious SQL statements")
                .withCategory(StrideCategory.TAMPERING);
    }

    public static ThreatTestDataBuilder anXssThreat() {
        return new ThreatTestDataBuilder()
                .withName("Cross-Site Scripting")
                .withDescription("Malicious scripts executed in user browser")
                .withCategory(StrideCategory.SPOOFING);
    }

    public static ThreatTestDataBuilder aCsrfThreat() {
        return new ThreatTestDataBuilder()
                .withName("Cross-Site Request Forgery")
                .withDescription("Unauthorized commands transmitted from user browser")
                .withCategory(StrideCategory.SPOOFING);
    }

    public ThreatTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ThreatTestDataBuilder withRandomId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public ThreatTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ThreatTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ThreatTestDataBuilder withCategory(StrideCategory category) {
        this.category = category;
        return this;
    }

    public ThreatTestDataBuilder withNullDescription() {
        this.description = null;
        return this;
    }

    public ThreatTestDataBuilder withNullCategory() {
        this.category = null;
        return this;
    }

    public ThreatTestDataBuilder withMaxLengthName() {
        this.name = "A".repeat(255);
        return this;
    }

    public ThreatTestDataBuilder withTooLongName() {
        this.name = "A".repeat(256);
        return this;
    }

    public ThreatTestDataBuilder withBlankName() {
        this.name = "";
        return this;
    }

    public ThreatTestDataBuilder withNullName() {
        this.name = null;
        return this;
    }

    public Threat build() {
        Threat threat = new Threat();
        threat.setId(id);
        threat.setName(name);
        threat.setDescription(description);
        threat.setCategory(category);
        return threat;
    }

    public Threat buildWithConstructor() {
        return new Threat(id, name, description, category);
    }
}