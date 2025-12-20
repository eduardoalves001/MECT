package com.ua.rtmp.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.ua.rtmp.dto.response.RiskDistributionDTO;
import com.ua.rtmp.dto.response.ThreatModelStatsDTO;
import com.ua.rtmp.dto.response.ThreatsByCategoryDTO;
import com.ua.rtmp.exception.ResourceNotFoundException;
import com.ua.rtmp.model.Component;
import com.ua.rtmp.model.ThreatModel;
import com.ua.rtmp.model.Vulnerability;
import com.ua.rtmp.model.enums.RiskLevel;
import com.ua.rtmp.model.enums.StrideCategory;
import com.ua.rtmp.repository.ComponentRepository;
import com.ua.rtmp.repository.ThreatModelRepository;
import com.ua.rtmp.repository.VulnerabilityRepository;
import com.ua.rtmp.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ThreatModelRepository threatModelRepository;
    private final ComponentRepository componentRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ThreatModelService threatModelService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] exportToPdf(UUID threatModelId) throws IOException {
        log.info("exportToPdf started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("EXPORT", "THREAT_MODEL", threatModelId.toString());
        
        ThreatModel threatModel = threatModelRepository.findById(threatModelId)
                .orElseThrow(() -> {
                    log.error("exportToPdf failed: threat model not found: threatModelId={}", threatModelId);
                    return new ResourceNotFoundException("Threat model not found with id: " + threatModelId);
                });

        log.debug("Fetching data for PDF export: threatModelName={}", threatModel.getName());
        List<Component> components = componentRepository.findByThreatModelId(threatModelId);
        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByThreatModelId(threatModelId);
        
        log.debug("Retrieved data: componentsCount={}, vulnerabilitiesCount={}", components.size(), vulnerabilities.size());
        
        ThreatModelStatsDTO stats = threatModelService.getThreatModelStats(threatModelId);
        List<ThreatsByCategoryDTO> threatsByCategory = threatModelService.getThreatsByCategory(threatModelId);
        List<RiskDistributionDTO> riskDistribution = threatModelService.getRiskDistribution(threatModelId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        log.debug("Generating PDF document structure");
        addPdfHeader(document, threatModel);
        addPdfOverview(document, stats, threatsByCategory, riskDistribution);

        if (components.isEmpty() && vulnerabilities.isEmpty()) {
            log.warn("No data available for threat model: threatModelId={}", threatModelId);
            document.add(new Paragraph("No data available for this threat model.")
                    .setFontSize(12)
                    .setItalic()
                    .setMarginTop(20));
        } else {
            // Add page break before components section
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addPdfComponents(document, components, vulnerabilities);
        }

        document.close();
        byte[] pdfBytes = baos.toByteArray();
        log.info("exportToPdf completed successfully: threatModelId={}, pdfSize={} bytes", threatModelId, pdfBytes.length);
        return pdfBytes;
    }

    @Transactional(readOnly = true)
    public String exportToCsv(UUID threatModelId) throws IOException {
        log.info("exportToCsv started: threatModelId={}", threatModelId);
        LoggingUtil.setOperationContext("EXPORT", "THREAT_MODEL", threatModelId.toString());
        
        ThreatModel threatModel = threatModelRepository.findById(threatModelId)
                .orElseThrow(() -> {
                    log.error("exportToCsv failed: threat model not found: threatModelId={}", threatModelId);
                    return new ResourceNotFoundException("Threat model not found with id: " + threatModelId);
                });

        log.debug("Fetching data for CSV export: threatModelName={}", threatModel.getName());
        List<Component> components = componentRepository.findByThreatModelId(threatModelId);
        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByThreatModelId(threatModelId);
        
        log.debug("Retrieved data: componentsCount={}, vulnerabilitiesCount={}", components.size(), vulnerabilities.size());

        StringWriter sw = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Project Name", "Project Description", "Created At", "Component Name", 
                          "Component Description", "Threat Name", "STRIDE Category", "Likelihood", 
                          "Impact", "Risk Score", "Risk Level", "Status", "Mitigation Strategies")
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(sw, csvFormat)) {
            if (components.isEmpty() && vulnerabilities.isEmpty()) {
                log.warn("No data available for threat model: threatModelId={}", threatModelId);
                csvPrinter.printRecord(
                    threatModel.getName(),
                    threatModel.getDescription(),
                    threatModel.getCreatedAt() != null ? threatModel.getCreatedAt().format(DATE_FORMATTER) : "",
                    "No data available",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                );
            } else {
                int recordCount = 0;
                for (Component component : components) {
                    List<Vulnerability> componentVulnerabilities = vulnerabilities.stream()
                            .filter(v -> v.getComponent().getId().equals(component.getId()))
                            .toList();

                    if (componentVulnerabilities.isEmpty()) {
                        csvPrinter.printRecord(
                            threatModel.getName(),
                            threatModel.getDescription(),
                            threatModel.getCreatedAt() != null ? threatModel.getCreatedAt().format(DATE_FORMATTER) : "",
                            component.getName(),
                            component.getDescription(),
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        );
                        recordCount++;
                    } else {
                        for (Vulnerability vulnerability : componentVulnerabilities) {
                            csvPrinter.printRecord(
                                threatModel.getName(),
                                threatModel.getDescription(),
                                threatModel.getCreatedAt() != null ? threatModel.getCreatedAt().format(DATE_FORMATTER) : "",
                                component.getName(),
                                component.getDescription(),
                                vulnerability.getThreat() != null ? vulnerability.getThreat().getName() : "",
                                vulnerability.getThreat() != null && vulnerability.getThreat().getCategory() != null 
                                    ? vulnerability.getThreat().getCategory().name() : "",
                                vulnerability.getLikelihood(),
                                vulnerability.getImpact(),
                                vulnerability.getRiskScore(),
                                RiskLevel.fromRiskScore(vulnerability.getRiskScore()).name(),
                                vulnerability.getStatus(),
                                vulnerability.getMitigationStrategies() != null ? vulnerability.getMitigationStrategies() : ""
                            );
                            recordCount++;
                        }
                    }
                }
                log.debug("CSV records generated: {}", recordCount);
            }
        }

        String csvContent = sw.toString();
        log.info("exportToCsv completed successfully: threatModelId={}, csvSize={} bytes", threatModelId, csvContent.length());
        return csvContent;
    }

    private void addPdfHeader(Document document, ThreatModel threatModel) {
        document.add(new Paragraph("Threat Model Export Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        document.add(new Paragraph("Project: " + threatModel.getName())
                .setFontSize(14)
                .setBold()
                .setMarginBottom(5));

        if (threatModel.getDescription() != null && !threatModel.getDescription().isEmpty()) {
            document.add(new Paragraph("Description: " + threatModel.getDescription())
                    .setFontSize(12)
                    .setMarginBottom(5));
        }

        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setItalic()
                .setMarginBottom(5));

        if (threatModel.getCreatedAt() != null) {
            document.add(new Paragraph("Created: " + threatModel.getCreatedAt().format(DATE_FORMATTER))
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(20));
        }
    }

    private void addPdfOverview(Document document, ThreatModelStatsDTO stats, 
                                List<ThreatsByCategoryDTO> threatsByCategory, 
                                List<RiskDistributionDTO> riskDistribution) {
        document.add(new Paragraph("Overview")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));

        // Summary Stats Section
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{23, 2, 23, 2, 23, 2, 23}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        statsTable.addCell(createStatsCell("Total Components", String.valueOf(stats.getTotalComponents()), new DeviceRgb(59, 130, 246)));
        statsTable.addCell(new Cell().setBorder(null)); // spacer
        statsTable.addCell(createStatsCell("Active Threats", String.valueOf(stats.getActiveThreats()), new DeviceRgb(234, 179, 8)));
        statsTable.addCell(new Cell().setBorder(null)); // spacer
        statsTable.addCell(createStatsCell("High Risk Threats", String.valueOf(stats.getHighRiskThreats()), new DeviceRgb(239, 68, 68)));
        statsTable.addCell(new Cell().setBorder(null)); // spacer
        statsTable.addCell(createStatsCell("Mitigated Threats", String.valueOf(stats.getMitigatedThreats()), new DeviceRgb(34, 197, 94)));

        document.add(statsTable);

        // Risk Dashboards Section Header
        document.add(new Paragraph("Risk Dashboards")
                .setFontSize(16)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10));

        // Two column layout for charts
        Table chartsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Left column: Threats by STRIDE Category (Pie Chart representation)
        Cell categoryCell = new Cell()
                .add(createCategoryChart(threatsByCategory))
                .setPadding(10)
                .setBorder(null);
        chartsTable.addCell(categoryCell);

        // Right column: Risk Distribution (Bar Chart representation)
        Cell riskCell = new Cell()
                .add(createRiskDistributionChart(riskDistribution))
                .setPadding(10)
                .setBorder(null);
        chartsTable.addCell(riskCell);

        document.add(chartsTable);
    }

    private Table createCategoryChart(List<ThreatsByCategoryDTO> threatsByCategory) {
        Table chartTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();

        chartTable.addCell(new Cell()
                .add(new Paragraph("Threats by STRIDE Category")
                        .setFontSize(14)
                        .setBold())
                .setBackgroundColor(new DeviceRgb(249, 250, 251))
                .setPadding(8)
                .setBorder(null));

        long total = threatsByCategory.stream().mapToLong(ThreatsByCategoryDTO::getCount).sum();

        if (total == 0) {
            chartTable.addCell(new Cell()
                    .add(new Paragraph("No data available")
                            .setFontSize(10)
                            .setItalic()
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(20)
                    .setBorder(null));
        } else {
            for (ThreatsByCategoryDTO category : threatsByCategory) {
                float percentage = (float) category.getCount() / total * 100;
                DeviceRgb color = getCategoryColor(category.getCategory());

                // Create visual percentage bar
                Table barTable = new Table(UnitValue.createPercentArray(new float[]{percentage, 100 - percentage}))
                        .useAllAvailableWidth()
                        .setMarginTop(5)
                        .setMarginBottom(5);

                // Colored portion of the bar
                Cell coloredBar = new Cell()
                        .add(new Paragraph(""))
                        .setHeight(20)
                        .setBackgroundColor(color)
                        .setBorder(null);
                barTable.addCell(coloredBar);

                // Gray portion of the bar (if not 100%)
                if (percentage < 100) {
                    Cell grayBar = new Cell()
                            .add(new Paragraph(""))
                            .setHeight(20)
                            .setBackgroundColor(new DeviceRgb(229, 231, 235))
                            .setBorder(null);
                    barTable.addCell(grayBar);
                }

                // Category info row
                Table infoTable = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}))
                        .useAllAvailableWidth()
                        .setMarginTop(2);

                Cell nameCell = new Cell()
                        .add(new Paragraph(formatCategoryName(category.getCategory()))
                                .setFontSize(10))
                        .setPadding(3)
                        .setBorder(null);

                Cell countCell = new Cell()
                        .add(new Paragraph(String.valueOf(category.getCount()))
                                .setFontSize(10)
                                .setBold())
                        .setPadding(3)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(null);

                Cell percentCell = new Cell()
                        .add(new Paragraph(String.format("%.1f%%", percentage))
                                .setFontSize(10))
                        .setPadding(3)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorder(null);

                infoTable.addCell(nameCell);
                infoTable.addCell(countCell);
                infoTable.addCell(percentCell);

                // Add bar and info to main table
                chartTable.addCell(new Cell()
                        .add(barTable)
                        .add(infoTable)
                        .setPadding(5)
                        .setBorder(null));
            }

            // Total
            chartTable.addCell(new Cell()
                    .add(new Paragraph("Total: " + total)
                            .setFontSize(12)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(8)
                    .setMarginTop(10)
                    .setBackgroundColor(new DeviceRgb(243, 244, 246))
                    .setBorder(null));
        }

        return chartTable;
    }

    private Table createRiskDistributionChart(List<RiskDistributionDTO> riskDistribution) {
        Table chartTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();

        chartTable.addCell(new Cell()
                .add(new Paragraph("Risk Distribution")
                        .setFontSize(14)
                        .setBold())
                .setBackgroundColor(new DeviceRgb(249, 250, 251))
                .setPadding(8)
                .setBorder(null));

        long maxCount = riskDistribution.stream().mapToLong(RiskDistributionDTO::getCount).max().orElse(1);
        long total = riskDistribution.stream().mapToLong(RiskDistributionDTO::getCount).sum();

        if (total == 0) {
            chartTable.addCell(new Cell()
                    .add(new Paragraph("No data available")
                            .setFontSize(10)
                            .setItalic()
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(20)
                    .setBorder(null));
        } else {
            // Sort by risk level order
            String[] riskOrder = {"Critical", "High", "Medium", "Low", "Minimal"};
            for (String riskLevelStr : riskOrder) {
                RiskDistributionDTO risk = riskDistribution.stream()
                        .filter(r -> r.getRiskLevel().equalsIgnoreCase(riskLevelStr))
                        .findFirst()
                        .orElse(new RiskDistributionDTO(riskLevelStr, 0L));

                RiskLevel level = RiskLevel.valueOf(risk.getRiskLevel().toUpperCase());
                DeviceRgb color = getRiskColor(level);
                float barWidth = maxCount > 0 ? (float) risk.getCount() / maxCount * 100 : 0;

                Table itemTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 3}))
                        .useAllAvailableWidth()
                        .setMarginTop(5);

                // Risk level name
                Cell nameCell = new Cell()
                        .add(new Paragraph(risk.getRiskLevel())
                                .setFontSize(10)
                                .setBold())
                        .setPadding(5)
                        .setBackgroundColor(color)
                        .setBorder(null);

                // Count
                Cell countCell = new Cell()
                        .add(new Paragraph(String.valueOf(risk.getCount()))
                                .setFontSize(10)
                                .setBold())
                        .setPadding(5)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(null);

                // Visual bar
                Table barTable = new Table(UnitValue.createPercentArray(new float[]{barWidth, 100 - barWidth}))
                        .useAllAvailableWidth()
                        .setMarginTop(2)
                        .setMarginBottom(2);

                if (barWidth > 0) {
                    barTable.addCell(new Cell()
                            .add(new Paragraph(""))
                            .setHeight(15)
                            .setBackgroundColor(color)
                            .setBorder(null));
                }
                if (barWidth < 100) {
                    barTable.addCell(new Cell()
                            .add(new Paragraph(""))
                            .setHeight(15)
                            .setBackgroundColor(new DeviceRgb(243, 244, 246))
                            .setBorder(null));
                }

                Cell barCell = new Cell()
                        .add(barTable)
                        .setPadding(5)
                        .setBorder(null);

                itemTable.addCell(nameCell);
                itemTable.addCell(countCell);
                itemTable.addCell(barCell);

                chartTable.addCell(new Cell()
                        .add(itemTable)
                        .setPadding(0)
                        .setBorder(null));
            }
        }

        return chartTable;
    }

    private DeviceRgb getCategoryColor(StrideCategory category) {
        return switch (category) {
            case SPOOFING -> new DeviceRgb(239, 68, 68);
            case TAMPERING -> new DeviceRgb(249, 115, 22);
            case REPUDIATION -> new DeviceRgb(234, 179, 8);
            case INFORMATION_DISCLOSURE -> new DeviceRgb(34, 197, 94);
            case DENIAL_OF_SERVICE -> new DeviceRgb(59, 130, 246);
            case ELEVATION_OF_PRIVILEGE -> new DeviceRgb(139, 92, 246);
        };
    }

    private String formatCategoryName(StrideCategory category) {
        return switch (category) {
            case SPOOFING -> "Spoofing";
            case TAMPERING -> "Tampering";
            case REPUDIATION -> "Repudiation";
            case INFORMATION_DISCLOSURE -> "Information Disclosure";
            case DENIAL_OF_SERVICE -> "Denial of Service";
            case ELEVATION_OF_PRIVILEGE -> "Elevation of Privilege";
        };
    }

    private Cell createStatsCell(String label, String value, DeviceRgb accentColor) {
        Paragraph labelPara = new Paragraph(label)
                .setFontSize(10)
                .setBold()
                .setMarginBottom(5)
                .setFontColor(new DeviceRgb(75, 85, 99));
        
        Paragraph valuePara = new Paragraph(value)
                .setFontSize(24)
                .setBold()
                .setFontColor(accentColor)
                .setTextAlignment(TextAlignment.CENTER);
        
        Cell cell = new Cell()
                .add(labelPara)
                .add(valuePara)
                .setBackgroundColor(new DeviceRgb(249, 250, 251))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(accentColor, 2))
                .setPadding(15)
                .setTextAlignment(TextAlignment.CENTER);
        
        return cell;
    }

    private void addPdfComponents(Document document, List<Component> components, List<Vulnerability> allVulnerabilities) {
        for (Component component : components) {
            document.add(new Paragraph("Component: " + component.getName())
                    .setFontSize(14)
                    .setBold()
                    .setBackgroundColor(new DeviceRgb(240, 240, 240))
                    .setPadding(5)
                    .setMarginTop(15)
                    .setMarginBottom(5));

            if (component.getDescription() != null && !component.getDescription().isEmpty()) {
                document.add(new Paragraph(component.getDescription())
                        .setFontSize(11)
                        .setMarginBottom(10));
            }

            List<Vulnerability> componentVulnerabilities = allVulnerabilities.stream()
                    .filter(v -> v.getComponent().getId().equals(component.getId()))
                    .toList();

            if (componentVulnerabilities.isEmpty()) {
                document.add(new Paragraph("No threats identified for this component.")
                        .setFontSize(10)
                        .setItalic()
                        .setMarginBottom(10));
            } else {
                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 1, 1, 1, 2, 2, 4}))
                        .useAllAvailableWidth()
                        .setMarginBottom(10);

                addTableHeader(table, "Threat", "STRIDE", "Likelihood", "Impact", "Risk Score", "Risk Level", "Status", "Mitigation");

                for (Vulnerability vulnerability : componentVulnerabilities) {
                    String threatName = vulnerability.getThreat() != null ? vulnerability.getThreat().getName() : "Unknown";
                    String stride = vulnerability.getThreat() != null && vulnerability.getThreat().getCategory() != null 
                            ? vulnerability.getThreat().getCategory().name().substring(0, 1) 
                            : "";
                    RiskLevel riskLevel = RiskLevel.fromRiskScore(vulnerability.getRiskScore());
                    String mitigation = vulnerability.getMitigationStrategies() != null 
                            ? vulnerability.getMitigationStrategies() 
                            : "None";

                    table.addCell(createCell(threatName, false));
                    table.addCell(createCell(stride, false));
                    table.addCell(createCell(String.valueOf(vulnerability.getLikelihood()), false));
                    table.addCell(createCell(String.valueOf(vulnerability.getImpact()), false));
                    table.addCell(createCell(String.valueOf(vulnerability.getRiskScore()), false));
                    table.addCell(createCell(riskLevel.name(), false, getRiskColor(riskLevel)));
                    table.addCell(createCell(vulnerability.getStatus().name(), false));
                    table.addCell(createCell(mitigation, false));
                }

                document.add(table);
            }
        }
    }

    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(createCell(header, true));
        }
    }

    private Cell createCell(String content, boolean isHeader) {
        return createCell(content, isHeader, null);
    }

    private Cell createCell(String content, boolean isHeader, DeviceRgb backgroundColor) {
        Cell cell = new Cell().add(new Paragraph(content).setFontSize(isHeader ? 9 : 8));
        
        if (isHeader) {
            cell.setBackgroundColor(new DeviceRgb(200, 200, 200))
                .setBold();
        } else if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        
        cell.setPadding(3);
        return cell;
    }

    private DeviceRgb getRiskColor(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> new DeviceRgb(255, 0, 0);
            case HIGH -> new DeviceRgb(255, 165, 0);
            case MEDIUM -> new DeviceRgb(255, 255, 0);
            case LOW -> new DeviceRgb(173, 216, 230);
            default -> new DeviceRgb(240, 240, 240);
        };
    }
}
