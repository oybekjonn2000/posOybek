package com.restaurantpos.printers.service;

import com.restaurantpos.common.exception.PosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class WindowsPrintService {

    private final PrinterDiscoveryService printerDiscoveryService;

    /**
     * Sends raw thermal ESC/POS bytes or text document directly to the real Windows
     * printer.
     *
     * @param systemPrinterName Exact Windows printer name (e.g. "EPSON TM-T20III"
     *                          or "POS DEMO PRINTER")
     * @param rawData           ESC/POS byte sequence
     * @param textContent       Text version for non-AUTOSENSE fallback (desktop /
     *                          PDF printers)
     */
    public void print(String systemPrinterName, byte[] rawData, String textContent) {
        if (systemPrinterName == null || systemPrinterName.isBlank()) {
            throw PosException.badRequest("Windows printer nomi ko'rsatilmagan");
        }

        PrintService printService = printerDiscoveryService.findPrintService(systemPrinterName)
                .orElseThrow(() -> PosException.notFound(
                        "Windows tizimida '" + systemPrinterName + "' nomli printer topilmadi. " +
                                "Printer o'chirilgan yoki nomi o'zgargan bo'lishi mumkin."));

        // Check printer status / accepting jobs
        PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) printService.getAttributes()
                .get(PrinterIsAcceptingJobs.class);
        if (accepting == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
            throw PosException.badRequest(
                    "'" + systemPrinterName
                            + "' printeri hozirda yangi buyurtmalarni qabul qilmayapti (Status: OFFLINE yoki Spooler to'xtatilgan)");
        }

        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(new Copies(1));

        DocPrintJob job = printService.createPrintJob();

        // 1. First priority: Raw ESC/POS bytes via AUTOSENSE (standard thermal POS
        // printing)
        if (rawData != null && rawData.length > 0
                && printService.isDocFlavorSupported(DocFlavor.BYTE_ARRAY.AUTOSENSE)) {
            try {
                Doc doc = new SimpleDoc(rawData, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, pras);
                log.info("Successfully dispatched {} raw ESC/POS bytes to Windows printer '{}'", rawData.length,
                        systemPrinterName);
                return;
            } catch (PrintException pe) {
                log.warn("Direct AUTOSENSE print failed on '{}': {}. Attempting Printable fallback...",
                        systemPrinterName, pe.getMessage());
            }
        }

        // 2. Fallback: Printable (Graphics2D text rendering for standard
        // laser/inkjet/PDF printers)
        if (textContent != null && !textContent.isBlank()
                && printService.isDocFlavorSupported(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
            try {
                TextPrintable printable = new TextPrintable(textContent);
                Doc doc = new SimpleDoc(printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
                job.print(doc, pras);
                log.info("Successfully rendered printable document to Windows printer '{}'", systemPrinterName);
                return;
            } catch (PrintException pe) {
                log.error("Printable fallback failed on '{}': {}", systemPrinterName, pe.getMessage(), pe);
                throw PosException.internalError("Windows printerga ma'lumot yuborishda xatolik: " + pe.getMessage());
            }
        }

        // 3. If neither supported or both failed
        try {
            Doc doc = new SimpleDoc(rawData, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, pras);
            log.info("Force-dispatched raw bytes to Windows printer '{}'", systemPrinterName);
        } catch (PrintException pe) {
            log.error("Fatal print spooler error on '{}': {}", systemPrinterName, pe.getMessage(), pe);
            throw PosException.internalError("Windows print spooler xatosi: " + pe.getMessage());
        }
    }

    /**
     * Helper Printable implementation for non-thermal / standard Windows printers
     */
    private static class TextPrintable implements Printable {
        private final String text;

        public TextPrintable(String text) {
            this.text = text;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g2d.setColor(Color.BLACK);

            FontMetrics fm = g2d.getFontMetrics();
            int lineHeight = fm.getHeight();
            int y = lineHeight;

            String[] lines = text.split("\n");
            for (String line : lines) {
                // Strip control escape characters from display
                String cleanLine = line.replaceAll("[\\x00-\\x1F]", "");
                g2d.drawString(cleanLine, 5, y);
                y += lineHeight;
            }

            return PAGE_EXISTS;
        }
    }
}
