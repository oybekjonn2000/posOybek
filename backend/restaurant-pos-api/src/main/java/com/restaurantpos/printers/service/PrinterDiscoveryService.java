package com.restaurantpos.printers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpos.printers.dto.PrinterDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrinterDiscoveryService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Discovers all real Windows printers installed on this host system.
     * Uses Java Print Service API as primary authority, optionally enriched with Windows WMI details.
     */
    public List<PrinterDto.AvailablePrinterDto> discoverPrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        String defaultName = defaultService != null ? defaultService.getName() : null;

        Map<String, WindowsPrinterInfo> winInfoMap = queryWindowsPrinterInfo();

        List<PrinterDto.AvailablePrinterDto> result = new ArrayList<>();

        for (PrintService ps : services) {
            String name = ps.getName();
            boolean isDefault = defaultName != null && defaultName.equalsIgnoreCase(name);

            WindowsPrinterInfo winInfo = winInfoMap.get(name.toLowerCase());
            if (winInfo != null && winInfo.isDefault) {
                isDefault = true;
            }

            String driver = winInfo != null && winInfo.driverName != null ? winInfo.driverName : "Standart Windows Driver";
            String status = determineStatus(ps, winInfo);

            result.add(PrinterDto.AvailablePrinterDto.builder()
                    .systemPrinterName(name)
                    .displayName(name)
                    .driverName(driver)
                    .isDefault(isDefault)
                    .status(status)
                    .build());
        }

        // Sort: default printer first, then alphabetically
        result.sort((p1, p2) -> {
            if (p1.isDefault() && !p2.isDefault()) return -1;
            if (!p1.isDefault() && p2.isDefault()) return 1;
            return p1.getSystemPrinterName().compareToIgnoreCase(p2.getSystemPrinterName());
        });

        log.info("Discovered {} real Windows printers on system: {}", result.size(),
                result.stream().map(PrinterDto.AvailablePrinterDto::getSystemPrinterName).collect(Collectors.toList()));

        return result;
    }

    /**
     * Verifies if a printer with the given system name is physically installed in Windows.
     */
    public boolean isPrinterInstalledInWindows(String systemPrinterName) {
        if (systemPrinterName == null || systemPrinterName.isBlank()) {
            return false;
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            if (ps.getName().trim().equalsIgnoreCase(systemPrinterName.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks up the Java PrintService corresponding to the given Windows printer name.
     */
    public Optional<PrintService> findPrintService(String systemPrinterName) {
        if (systemPrinterName == null || systemPrinterName.isBlank()) {
            return Optional.empty();
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            if (ps.getName().trim().equalsIgnoreCase(systemPrinterName.trim())) {
                return Optional.of(ps);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks current operational status of a specific Windows printer.
     */
    public String checkPrinterStatus(String systemPrinterName) {
        Optional<PrintService> psOpt = findPrintService(systemPrinterName);
        if (psOpt.isEmpty()) {
            return "NOT_FOUND";
        }

        Map<String, WindowsPrinterInfo> winInfoMap = queryWindowsPrinterInfo();
        WindowsPrinterInfo winInfo = winInfoMap.get(systemPrinterName.toLowerCase());

        return determineStatus(psOpt.get(), winInfo);
    }

    private String determineStatus(PrintService ps, WindowsPrinterInfo winInfo) {
        if (winInfo != null) {
            if (winInfo.workOffline) {
                return "OFFLINE";
            }
            if (winInfo.printerStatus != null) {
                // Win32_Printer status: 3=Idle/Normal, 4=Printing, 5=Warmup, 6=Stopped, 7=Offline
                if (winInfo.printerStatus == 7 || winInfo.printerStatus == 6) {
                    return "OFFLINE";
                }
                if (winInfo.printerStatus == 3 || winInfo.printerStatus == 4) {
                    return "ONLINE";
                }
            }
        }

        AttributeSet attrs = ps.getAttributes();
        PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) attrs.get(PrinterIsAcceptingJobs.class);
        if (accepting != null) {
            if (accepting == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
                return "OFFLINE";
            } else if (accepting == PrinterIsAcceptingJobs.ACCEPTING_JOBS) {
                return "ONLINE";
            }
        }

        return "UNKNOWN";
    }

    /**
     * Safely queries Windows WMI for driver names and extended status.
     * Times out quickly if command fails or takes more than 2.5 seconds.
     */
    private Map<String, WindowsPrinterInfo> queryWindowsPrinterInfo() {
        Map<String, WindowsPrinterInfo> map = new HashMap<>();

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return map;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-Command",
                    "Get-CimInstance Win32_Printer | Select-Object Name, DriverName, PrinterStatus, PortName, Default, WorkOffline | ConvertTo-Json"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(2500, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Windows printer WMI query timed out after 2.5s");
                return map;
            }

            String json = sb.toString().trim();
            if (json.isEmpty()) return map;

            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    parsePrinterNode(node, map);
                }
            } else if (root.isObject()) {
                parsePrinterNode(root, map);
            }

        } catch (Exception e) {
            log.warn("Could not query Windows printer WMI info: {}", e.getMessage());
        }

        return map;
    }

    private void parsePrinterNode(JsonNode node, Map<String, WindowsPrinterInfo> map) {
        String name = node.hasNonNull("Name") ? node.get("Name").asText() : null;
        if (name == null || name.isBlank()) return;

        WindowsPrinterInfo info = new WindowsPrinterInfo();
        info.name = name;
        info.driverName = node.hasNonNull("DriverName") ? node.get("DriverName").asText() : null;
        info.portName = node.hasNonNull("PortName") ? node.get("PortName").asText() : null;
        info.isDefault = node.hasNonNull("Default") && node.get("Default").asBoolean();
        info.workOffline = node.hasNonNull("WorkOffline") && node.get("WorkOffline").asBoolean();
        info.printerStatus = node.hasNonNull("PrinterStatus") ? node.get("PrinterStatus").asInt() : null;

        map.put(name.toLowerCase(), info);
    }

    private static class WindowsPrinterInfo {
        String name;
        String driverName;
        String portName;
        boolean isDefault;
        boolean workOffline;
        Integer printerStatus;
    }
}
