/*
 * User Activity Tracer: record the applications running on a Windows machine.
 *
 * Extracted from code.docx (kept unchanged in the repository root). Changes from the
 * original listing:
 *  - the read loop stops at end of output (the original looped forever and threw a
 *    NullPointerException when readLine() returned null);
 *  - one row is inserted per process, after its four CSV fields are read (the original
 *    INSERT sat inside the token loop and ran once per token);
 *  - the CSV header line and surrounding quotes are skipped;
 *  - values are bound with PreparedStatement parameters instead of string concatenation;
 *  - database URL/user/password can be set with DB_URL, DB_USER, DB_PASSWORD
 *    (defaults match the original: jdbc:mysql://localhost:3306/sd, root, empty).
 *
 * Requires Windows (uses `tasklist`), a MySQL server, and MySQL Connector/J on the classpath.
 * Create the table with sql/schema.sql.
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class running_processes
{
    static String env(String name, String fallback)
    {
        String v = System.getenv(name);
        return v == null ? fallback : v;
    }

    /** Split one line of `tasklist /FO csv` output into its fields, without quotes. */
    static String[] parseCsvLine(String line)
    {
        String trimmed = line.trim();
        if (trimmed.startsWith("\"")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("\"")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed.split("\",\"", -1);
    }

    public static void main(String[] args) throws Exception
    {
        String url = env("DB_URL", "jdbc:mysql://localhost:3306/sd");
        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");

        Date now = new Date();
        String date1 = new SimpleDateFormat("dd-MM-yyyy").format(now);
        String time1 = new SimpleDateFormat("HH:mm:ss").format(now);

        // Image Name, PID, Session Name, Session#, Mem Usage
        ProcessBuilder builder = new ProcessBuilder("tasklist", "/FO", "csv");
        builder.redirectErrorStream(true);
        Process p = builder.start();

        String sql = "INSERT INTO rp (NAME, PID, SESSION, SESSIONH, TIME, DATE) VALUES (?, ?, ?, ?, ?, ?)";
        int rows = 0;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = conn.prepareStatement(sql);
             BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream())))
        {
            String line;
            boolean header = true;
            while ((line = r.readLine()) != null)
            {
                if (header) { header = false; continue; }
                if (line.trim().isEmpty()) continue;
                String[] a = parseCsvLine(line);
                if (a.length < 4) continue;
                for (int i = 0; i < 4; i++) statement.setString(i + 1, a[i]);
                statement.setString(5, time1);
                statement.setString(6, date1);
                rows += statement.executeUpdate();
            }
        }
        System.out.println("Recorded " + rows + " running processes at " + date1 + " " + time1);
    }
}
