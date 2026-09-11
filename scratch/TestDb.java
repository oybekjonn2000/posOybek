import java.sql.*;

public class TestDb {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5433/pos", "pos_user", "123")) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, name, connection_type, windows_printer_name, deleted_at FROM printers");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("Row: " + rs.getString("id") + " | " + rs.getString("name") + " | " + rs.getString("connection_type") + " | " + rs.getString("windows_printer_name") + " | del:" + rs.getString("deleted_at"));
            }
            System.out.println("Total rows in printers: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
