import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateDb {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/pidev";
        String user = "root";
        String password = "";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE user ADD COLUMN ban_until DATETIME DEFAULT NULL");
            System.out.println("SUCCESS");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
