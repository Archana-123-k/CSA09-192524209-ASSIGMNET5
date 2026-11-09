import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MigrationProgram {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3307/pass_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static List<String[]> readAndValidate(Path file) throws IOException {
        List<String[]> valid = new ArrayList<>();
        List<String> rejects = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    rejects.add(lineNo + ": empty line");
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                if (fields.length != 7) {
                    rejects.add(lineNo + ": wrong field count -> " + line);
                    continue;
                }
                try {
                    Double.parseDouble(fields[6]);
                } catch (NumberFormatException ex) {
                    rejects.add(lineNo + ": invalid amount -> " + line);
                    continue;
                }
                valid.add(fields);
            }
        }
        System.out.println("Loaded valid rows: " + valid.size());
        System.out.println("Rejected rows: " + rejects.size());
        for (String reject : rejects) {
            System.out.println("REJECT -> " + reject);
        }
        return valid;
    }

    private static void writeFailureReport(Path target, List<String> reasons) throws IOException {
        Files.createDirectories(target.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            for (String reason : reasons) {
                writer.write(reason);
                writer.newLine();
            }
        }
    }

    private static void migrate(Path inputFile, Path reportFile) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(JDBC_URL, "root", "")) {
            con.setAutoCommit(false);
            List<String[]> validRows = readAndValidate(inputFile);
            List<String> rejects = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)")) {
                for (String[] p : validRows) {
                    try {
                        ps.setString(1, p[0]);
                        ps.setString(2, p[1]);
                        ps.setString(3, p[2]);
                        ps.setInt(4, Integer.parseInt(p[3]));
                        ps.setDouble(5, Double.parseDouble(p[6]));
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        rejects.add("insert failure for " + p[0] + " => " + e.getMessage());
                    }
                }
            }
            if (!rejects.isEmpty()) {
                writeFailureReport(reportFile, rejects);
            }
            con.commit();
            System.out.println("Migration committed. Valid rows inserted = " + validRows.size());
        }
    }

    public static void printMetadata(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        System.out.println("Product: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        System.out.println("Driver: " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.println("URL: " + meta.getURL());
        System.out.println("User: " + meta.getUserName());
        try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
            System.out.println("Tables:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("TABLE_NAME"));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("PartB", "B4");
        Path input = dir.resolve("migration_input.txt");
        Path report = dir.resolve("failure_report.txt");
        Files.createDirectories(dir);

        String[] rows = {
                "P1001|192311001|Ananya|7|MG Road|2026-12-31|3800.0",
                "BROKEN|FIELD|COUNT",
                "P1002|192311002|Bharat|7|Town Hall|2026-12-31|bad",
                "",
                "P1003|192311003|Divya|7|Central|2026-12-31|4200.0"
        };
        try (BufferedWriter writer = Files.newBufferedWriter(input, StandardCharsets.UTF_8)) {
            for (String row : rows) {
                writer.write(row);
                writer.newLine();
            }
        }

        try (Connection con = DriverManager.getConnection(JDBC_URL, "root", "")) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS bus_pass (pass_id VARCHAR(30) PRIMARY KEY, reg_no VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, route_no INT NOT NULL, amount DOUBLE NOT NULL)");
                st.executeUpdate("TRUNCATE TABLE bus_pass");
            }
            printMetadata(con);
            migrate(input, report);
            try (Statement st = con.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass");
                rs.next();
                System.out.println("final row count = " + rs.getInt(1));
            }
        }
    }
}
