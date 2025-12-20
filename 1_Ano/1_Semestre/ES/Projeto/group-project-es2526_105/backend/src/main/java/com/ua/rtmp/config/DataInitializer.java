package com.ua.rtmp.config;

import com.ua.rtmp.model.enums.StrideCategory;
import com.ua.rtmp.model.Threat;
import com.ua.rtmp.repository.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final ThreatRepository threatRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        long existingCount = threatRepository.count();
        
        if (existingCount > 0) {
            log.info("Data initialization skipped: {} threats already exist in database", existingCount);
            return;
        }

        log.info("Initializing threat catalog data...");
        
        List<Threat> threats = List.of(
                new Threat(null,
                    "SQL Injection",
                    "An attacker can inject malicious SQL code into application queries through unsanitized user inputs, potentially allowing unauthorized access to sensitive data, modification of database records, or execution of administrative operations. This occurs when user input is directly concatenated into SQL statements without proper validation or parameterization.",
                    StrideCategory.TAMPERING),

                new Threat(null,
                    "Cross-Site Scripting (XSS)",
                    "Malicious scripts are injected into web pages viewed by other users, allowing attackers to steal session cookies, capture keystrokes, redirect users to malicious sites, or deface websites. XSS vulnerabilities arise when applications include untrusted data in web pages without proper validation or escaping.",
                    StrideCategory.TAMPERING),

                new Threat(null,
                    "Cross-Site Request Forgery (CSRF)",
                    "An attacker tricks an authenticated user into executing unwanted actions on a web application by exploiting the user's existing session. This can lead to unauthorized fund transfers, password changes, or data modifications without the user's knowledge or consent.",
                    StrideCategory.TAMPERING),

                new Threat(null,
                    "Session Hijacking",
                    "An attacker intercepts or steals a valid session token through network sniffing, XSS attacks, or session fixation, allowing them to impersonate a legitimate user and gain unauthorized access to the application with the victim's privileges and permissions.",
                    StrideCategory.SPOOFING),

                new Threat(null,
                    "Credential Stuffing",
                    "Attackers use stolen username and password pairs from previous data breaches to gain unauthorized access to user accounts. This exploits the common practice of password reuse across multiple services, potentially compromising accounts even with valid credentials.",
                    StrideCategory.SPOOFING),

                new Threat(null,
                    "Man-in-the-Middle Attack",
                    "An attacker positions themselves between two communicating parties to intercept, read, or modify data in transit. This can occur on unsecured networks or through DNS spoofing, allowing the attacker to eavesdrop on sensitive communications or inject malicious content.",
                    StrideCategory.INFORMATION_DISCLOSURE),

                new Threat(null,
                    "Sensitive Data Exposure",
                    "Confidential information such as passwords, credit card numbers, personal identification data, or health records are inadequately protected through weak encryption, insecure storage, or transmission over unencrypted channels, making them vulnerable to unauthorized access.",
                    StrideCategory.INFORMATION_DISCLOSURE),

                new Threat(null,
                    "Insecure Direct Object References",
                    "The application exposes internal implementation objects such as files, directories, or database keys through URLs or parameters without proper authorization checks, allowing attackers to access unauthorized data by manipulating these references.",
                    StrideCategory.INFORMATION_DISCLOSURE),

                new Threat(null,
                    "Insufficient Logging and Monitoring",
                    "Inadequate logging, monitoring, and alerting mechanisms prevent timely detection and response to security incidents. Without proper audit trails, organizations cannot effectively investigate breaches, comply with regulations, or identify attack patterns.",
                    StrideCategory.REPUDIATION),

                new Threat(null,
                    "Log Tampering",
                    "Attackers modify, delete, or inject false entries into system logs to hide their malicious activities, frame innocent users, or disrupt forensic investigations. This undermines the integrity of audit trails and can prevent accurate incident response.",
                    StrideCategory.REPUDIATION),

                new Threat(null,
                    "Distributed Denial of Service (DDoS)",
                    "Attackers overwhelm a system, network, or application with a massive volume of requests from multiple sources, consuming resources and rendering the service unavailable to legitimate users. This can result in significant downtime, revenue loss, and reputation damage.",
                    StrideCategory.DENIAL_OF_SERVICE),

                new Threat(null,
                    "Resource Exhaustion",
                    "An attacker exploits inefficient code or lack of resource limits to consume excessive CPU, memory, disk space, or network bandwidth, causing performance degradation or system crashes that prevent legitimate users from accessing services.",
                    StrideCategory.DENIAL_OF_SERVICE),

                new Threat(null,
                    "Privilege Escalation",
                    "A user with limited privileges exploits vulnerabilities, misconfigurations, or design flaws to gain unauthorized elevated access rights, potentially obtaining administrative or root privileges that allow full system control and access to sensitive resources.",
                    StrideCategory.ELEVATION_OF_PRIVILEGE),

                new Threat(null,
                    "Broken Access Control",
                    "Inadequate enforcement of access restrictions allows users to act outside their intended permissions, accessing unauthorized functionality or data. This can result from missing authorization checks, insecure direct object references, or improper role validation.",
                    StrideCategory.ELEVATION_OF_PRIVILEGE),

                new Threat(null,
                    "XML External Entity (XXE) Injection",
                    "An attacker exploits vulnerable XML processors to access local or remote files, perform server-side request forgery, scan internal networks, or cause denial of service by injecting malicious external entity references into XML input.",
                    StrideCategory.INFORMATION_DISCLOSURE),

                new Threat(null,
                    "Insecure Deserialization",
                    "Untrusted data is deserialized without proper validation, allowing attackers to execute arbitrary code, perform injection attacks, or manipulate application logic by crafting malicious serialized objects that are processed by the application.",
                    StrideCategory.TAMPERING),

                new Threat(null,
                    "Server-Side Request Forgery (SSRF)",
                    "An attacker tricks the server into making requests to unintended locations, potentially accessing internal systems, cloud metadata services, or other protected resources that should not be accessible from the internet.",
                    StrideCategory.TAMPERING),

                new Threat(null,
                    "Remote Code Execution",
                    "Critical vulnerabilities allow attackers to execute arbitrary code on the target system remotely, potentially leading to complete system compromise, data theft, malware installation, or using the compromised system as a pivot point for further attacks.",
                    StrideCategory.ELEVATION_OF_PRIVILEGE),

                new Threat(null,
                    "Weak Cryptography",
                    "The use of outdated, weak, or improperly implemented cryptographic algorithms and protocols makes encrypted data vulnerable to cryptanalysis, brute-force attacks, or known exploits, compromising the confidentiality and integrity of sensitive information.",
                    StrideCategory.INFORMATION_DISCLOSURE),

                new Threat(null,
                    "API Security Misconfiguration",
                    "APIs lack proper authentication, authorization, rate limiting, or input validation, exposing sensitive endpoints and functionality to unauthorized access, abuse, or automated attacks that can compromise data and system integrity.",
                    StrideCategory.ELEVATION_OF_PRIVILEGE)
        );

        threatRepository.saveAll(threats);
        log.info("Threat catalog initialized successfully: {} threats added", threats.size());
    }
}