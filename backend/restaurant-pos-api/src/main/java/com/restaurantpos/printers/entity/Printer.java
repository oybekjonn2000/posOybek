package com.restaurantpos.printers.entity;

import com.restaurantpos.tenants.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printers")
@Getter
@Setter
public class Printer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "model", length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 30)
    private ConnectionType connectionType = ConnectionType.WINDOWS;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "port")
    private Integer port = 9100;

    @Column(name = "windows_printer_name")
    private String windowsPrinterName;

    public String getSystemPrinterName() {
        return this.windowsPrinterName;
    }

    public void setSystemPrinterName(String systemPrinterName) {
        this.windowsPrinterName = systemPrinterName;
    }

    @Column(name = "paper_width", nullable = false)
    private int paperWidth = 80; // 58 or 80 mm

    @Column(name = "character_encoding", nullable = false, length = 50)
    private String characterEncoding = "UTF-8";

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private PrinterPurpose purpose = PrinterPurpose.KITCHEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PrinterStatus status = PrinterStatus.ONLINE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "auto_print", nullable = false)
    private boolean autoPrint = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fallback_printer_id")
    private Printer fallbackPrinter;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_successful_print_at")
    private Instant lastSuccessfulPrintAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ConnectionType {
        WINDOWS, USB, NETWORK, TCPIP
    }

    public enum PrinterPurpose {
        KITCHEN, CASHIER
    }

    public enum PrinterStatus {
        ONLINE, OFFLINE, NOT_FOUND, ERROR, UNKNOWN
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
