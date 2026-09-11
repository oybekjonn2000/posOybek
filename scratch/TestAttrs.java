import javax.print.*;
import javax.print.attribute.*;

public class TestAttrs {
    public static void main(String[] args) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            System.out.println("=== Service: " + ps.getName() + " ===");
            AttributeSet attrs = ps.getAttributes();
            for (Attribute attr : attrs.toArray()) {
                System.out.println("  " + attr.getCategory().getName() + " -> " + attr.getName() + ": " + attr);
            }
        }
    }
}
