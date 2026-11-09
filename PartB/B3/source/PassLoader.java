import java.io.BufferedWriter;
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
import java.util.ArrayList;
import java.util.List;

public class PassLoader {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3307/pass_test?rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static void resetTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    public static void insertStatement(Connection con, List<String[]> rows) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String[] p : rows) {
                String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ('" + p[0] + "', '" + p[1] + "', '" + p[2] + "', " + Integer.parseInt(p[3]) + ", " + Double.parseDouble(p[4]) + ")";
                st.executeUpdate(sql);
            }
        }
    }

    public static void insertPrepared(Connection con, List<String[]> rows) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String[] p : rows) {
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                ps.setDouble(5, Double.parseDouble(p[4]));
                ps.executeUpdate();
            }
        }
    }

    public static void insertBatch(Connection con, List<String[]> rows) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String[] p : rows) {
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                ps.setDouble(5, Double.parseDouble(p[4]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public static int loadAll(Connection con, List<String[]> rows) throws SQLException {
        try {
            con.setAutoCommit(false);
            String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            int n = 0;
            for (String[] p : rows) {
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                ps.setDouble(5, Double.parseDouble(p[4]));
                ps.addBatch();
                if (++n % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            con.commit();
            return n;
        } catch (SQLException | RuntimeException e) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
        }
    }

    private static List<String[]> generateRows(int count) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new String[] {
                "P" + i,
                "19231100" + String.format("%03d", i),
                "Name" + i,
                String.valueOf(7 + (i % 4)),
                String.valueOf(3500.0 + i)
            });
        }
        return rows;
    }

    public static long benchmark(String name, Connection con, List<String[]> rows, java.util.function.Consumer<Connection> runner) throws Exception {
        resetTable(con);
        long start = System.nanoTime();
        runner.accept(con);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        System.out.println(name + " -> " + elapsedMs + " ms");
        return elapsedMs;
    }

    public static void printColumnInfo(Connection con) throws SQLException {
        ResultSet rs = con.createStatement().executeQuery("SELECT * FROM bus_pass WHERE 1 = 0");
        java.sql.ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            System.out.println(md.getColumnName(i) + " => " + md.getColumnTypeName(i));
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(JDBC_URL, "root", "")) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS bus_pass (pass_id VARCHAR(30) PRIMARY KEY, reg_no VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, route_no INT NOT NULL, amount DOUBLE NOT NULL)");
            }

            System.out.println("Column metadata:");
            printColumnInfo(con);

            List<Integer> sizes = List.of(1000, 10000, 100000);
            List<String> csv = new ArrayList<>();
            csv.add("mode,rows,elapsed_ms");

            for (int n : sizes) {
                List<String[]> rows = generateRows(n);
                long s = benchmark("Statement", con, rows, c -> {
                    try {
                        insertStatement(c, rows);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                csv.add("statement," + n + "," + s);

                long p = benchmark("PreparedStatement", con, rows, c -> {
                    try {
                        insertPrepared(c, rows);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                csv.add("prepared," + n + "," + p);

                long b = benchmark("PreparedStatement+Batch", con, rows, c -> {
                    try {
                        insertBatch(c, rows);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                csv.add("batch," + n + "," + b);
            }

            Path results = Paths.get("PartB", "B3", "results.csv");
            Files.createDirectories(results.getParent());
            Files.writeString(results, String.join(System.lineSeparator(), csv) + System.lineSeparator(), StandardCharsets.UTF_8);

            System.out.println("Batch transaction test:");
            try (Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE bus_pass");
            }
            List<String[]> goodRows = generateRows(3);
            List<String[]> badRows = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String[] r = i == 2 ? new String[] {"BAD", "19231100X", "BadName", "7", "bbb"} : goodRows.get(i % goodRows.size());
                badRows.add(r);
            }
            try {
                loadAll(con, badRows);
            } catch (RuntimeException e) {
                System.out.println("Transaction rolled back due to invalid amount");
            }
            try (Statement st = con.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass");
                rs.next();
                System.out.println("rows after invalid batch = " + rs.getInt(1));
            }
        }
    }
}
