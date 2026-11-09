import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UnsafeLoginDemo {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3307/pass_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("Unsafe SQL: " + sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next();
    }

    public static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND pwd = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, id);
        ps.setString(2, pwd);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public static void validatePassFile(Path file) throws IOException {
        System.out.println("Validating bus-pass file: " + file);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    System.out.println("Reject line " + lineNo + ": empty line");
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                if (fields.length != 7) {
                    System.out.println("Reject line " + lineNo + ": wrong field count = " + fields.length);
                    continue;
                }
                try {
                    Double.parseDouble(fields[6]);
                } catch (NumberFormatException ex) {
                    System.out.println("Reject line " + lineNo + ": non-numeric amount => " + fields[6]);
                    continue;
                }
                System.out.println("Accept line " + lineNo + ": " + fields[0]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(JDBC_URL, "root", "")) {
            Statement st = con.createStatement();
            st.executeUpdate("DROP TABLE IF EXISTS users");
            st.executeUpdate("CREATE TABLE users (id VARCHAR(30) PRIMARY KEY, pwd VARCHAR(30) NOT NULL)");
            st.executeUpdate("INSERT INTO users (id, pwd) VALUES ('admin', 'pass123')");

            String attackId = "admin' -- ";
            String attackPwd = "x";
            System.out.println("Unsafe login test");
            System.out.println("unsafeLogin(...) = " + unsafeLogin(con, attackId, attackPwd));
            System.out.println("safeLogin(...) = " + safeLogin(con, attackId, attackPwd));

            Path file = Paths.get("PartB", "B2", "damaged_passes.txt");
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), false))) {
                writer.write("P1001|192311001|Ananya|7|MG Road|2026-12-31|3800.0");
                writer.newLine();
                writer.write("BROKEN|FIELD");
                writer.newLine();
                writer.write("");
                writer.newLine();
                writer.write("P1003|192311003|Divya|7|Central|2026-12-31|bad");
                writer.newLine();
            }
            validatePassFile(file);
        }
    }
}
