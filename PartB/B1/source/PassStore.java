import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PassStore {
    private static final String DELIM = "|";

    public static void addRecord(Path file, String passId, String regNo, String name, String routeNo,
                                 String boardingStop, String validityDate, double amountPaid) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
        String line = String.join(DELIM, passId, regNo, name, routeNo, boardingStop, validityDate, Double.toString(amountPaid));
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        }
    }

    public static String[] findById(Path file, String passId) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", -1);
                if (fields.length != 7) {
                    continue;
                }
                if (fields[0].equals(passId)) {
                    return fields;
                }
            }
        }
        return null;
    }

    public static List<String[]> listByRoute(Path file, String routeNo) throws IOException {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", -1);
                if (fields.length != 7) {
                    continue;
                }
                if (fields[3].equals(routeNo)) {
                    result.add(fields);
                }
            }
        }
        return result;
    }

    public static long countLinesUnbuffered(Path file) throws IOException {
        long count = 0L;
        try (InputStream in = Files.newInputStream(file)) {
            int value;
            while ((value = in.read()) != -1) {
                if (value == '\n') {
                    count++;
                }
            }
        }
        return count;
    }

    public static long countLinesBuffered(Path file) throws IOException {
        long count = 0L;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == '\n') {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void writeBenchmarkFile(Path file, long targetBytes) throws IOException {
        Files.deleteIfExists(file);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            StringBuilder builder = new StringBuilder();
            long written = 0;
            int lineNo = 1;
            while (written < targetBytes) {
                builder.setLength(0);
                builder.append("P").append(lineNo).append("|").append(200000000 + lineNo).append("|Name").append(lineNo)
                        .append("|7|Main Gate|2027-12-31|").append((lineNo * 10) % 5000).append(".0\n");
                byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
                if (written + bytes.length > targetBytes) {
                    out.write(bytes, 0, (int) (targetBytes - written));
                    break;
                }
                out.write(bytes);
                written += bytes.length;
                lineNo++;
            }
        }
    }

    private static void writeCsv(Path csvPath, List<String> rows) throws IOException {
        Files.createDirectories(csvPath.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(String.join(System.lineSeparator(), rows));
            writer.newLine();
        }
    }

    private static void benchmarkLineCounts(Path root) throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("mode,size_mb,avg_ms,min_ms,max_ms,reads");
        int[] sizes = {1, 10, 50};
        for (int sizeMb : sizes) {
            Path file = root.resolve("benchmark_" + sizeMb + "MB.txt");
            long targetBytes = sizeMb * 1024L * 1024L;
            writeBenchmarkFile(file, targetBytes);
            List<Long> unbuffered = new ArrayList<>();
            List<Long> buffered = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long start = System.nanoTime();
                long count1 = countLinesUnbuffered(file);
                unbuffered.add((System.nanoTime() - start) / 1_000_000L);
                long start2 = System.nanoTime();
                long count2 = countLinesBuffered(file);
                buffered.add((System.nanoTime() - start2) / 1_000_000L);
                if (count1 != count2) {
                    throw new IllegalStateException("Line counts differ for file at size " + sizeMb + "MB: " + count1 + " vs " + count2);
                }
            }
            long avgU = Math.round(unbuffered.stream().mapToLong(Long::longValue).average().orElse(0));
            long avgB = Math.round(buffered.stream().mapToLong(Long::longValue).average().orElse(0));
            long minU = unbuffered.stream().mapToLong(Long::longValue).min().orElse(0); 
            long maxU = unbuffered.stream().mapToLong(Long::longValue).max().orElse(0);
            long minB = buffered.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxB = buffered.stream().mapToLong(Long::longValue).max().orElse(0);
            rows.add("unbuffered," + sizeMb + "," + avgU + "," + minU + "," + maxU + "," + (targetBytes / 1));
            rows.add("buffered," + sizeMb + "," + avgB + "," + minB + "," + maxB + "," + (targetBytes / 8192L));
        }
        writeCsv(root.resolve("results.csv"), rows);
    }

    private static void benchmarkFindById(Path root) throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("records,avg_ms,min_ms,max_ms");
        int[] sizes = {1000, 5000, 10000, 50000, 100000};
        for (int n : sizes) {
            Path file = root.resolve("find_bench_" + n + ".txt");
            Files.deleteIfExists(file);
            for (int i = 0; i < n; i++) {
                addRecord(file, "P" + i, "19231100" + i, "Name" + i, "7", "Main Gate", "2027-12-31", 3500.0 + i);
            }
            List<Long> samples = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long start = System.nanoTime();
                findById(file, "P" + (n - 1));
                samples.add((System.nanoTime() - start) / 1_000_000L);
            }
            long avg = Math.round(samples.stream().mapToLong(Long::longValue).average().orElse(0));
            long min = samples.stream().mapToLong(Long::longValue).min().orElse(0);
            long max = samples.stream().mapToLong(Long::longValue).max().orElse(0);
            rows.add(n + "," + avg + "," + min + "," + max);
        }
        Files.writeString(root.resolve("find_results.csv"), String.join(System.lineSeparator(), rows) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get("PartB", "B1");
        Path file = root.resolve("pass_store_test.txt");
        Files.deleteIfExists(file);

        addRecord(file, "P1001", "192311001", "Ananya", "7", "MG Road", "2026-12-31", 3800.0);
        addRecord(file, "P1002", "192311002", "Bharat", "7", "Town Hall", "2026-12-31", 4200.0);
        addRecord(file, "P1003", "192311003", "Divya", "7", "Central", "2026-12-31", 3800.0);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            writer.write("BROKEN|FIELD|COUNT");
            writer.newLine();
        }

        String[] found = findById(file, "P1003");
        String[] missing = findById(file, "P9999");
        System.out.println(Arrays.toString(found));
        System.out.println(Arrays.toString(missing));
        System.out.println("Route 7 records: " + listByRoute(file, "7").size());

        benchmarkLineCounts(root);
        benchmarkFindById(root);
        System.out.println("B1 metrics saved to " + root.resolve("results.csv"));
        System.out.println("B1 find benchmark saved to " + root.resolve("find_results.csv"));
    }
}
