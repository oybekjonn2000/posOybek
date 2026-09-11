import javax.print.*;
import java.nio.charset.StandardCharsets;

public class TestPrinters {
    public static void main(String[] args) {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService target = null;
            for (PrintService ps : services) {
                if (ps.getName().contains("POS DEMO")) {
                    target = ps;
                    break;
                }
            }
            if (target != null) {
                System.out.println("Printing to: " + target.getName());
                DocPrintJob job = target.createPrintJob();
                String testStr = "=== TEST PRINT ===\nHello POS\n\n\n";
                byte[] bytes = testStr.getBytes(StandardCharsets.UTF_8);
                Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, null);
                System.out.println("Print job submitted successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
