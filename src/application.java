/*
 * User Activity Tracer: record installed applications and their versions.
 *
 * Extracted from code.docx (kept unchanged in the repository root). Changes from the
 * original listing:
 *  - values are bound with PreparedStatement parameters instead of string concatenation
 *    (application names containing an apostrophe broke the original INSERT);
 *  - the "Name"/"Version" header lines and blank lines are skipped;
 *  - SQL errors are reported instead of being silently swallowed;
 *  - database URL/user/password can be set with DB_URL, DB_USER, DB_PASSWORD
 *    (defaults match the original).
 *
 * Requires Windows with `wmic` (deprecated on recent Windows 11 builds), a MySQL server,
 * and MySQL Connector/J on the classpath. Create the table with sql/schema.sql.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class application
{
    static String env(String name, String fallback)
    {
        String v = System.getenv(name);
        return v == null ? fallback : v;
    }

    public static void main(String[] args) throws IOException, SQLException
    {
        ProcessBuilder builder = new ProcessBuilder("wmic", "product", "get", "name");
        ProcessBuilder builder1 = new ProcessBuilder("wmic", "product", "get", "version");
        builder.redirectErrorStream(true);
        builder1.redirectErrorStream(true);
        Process p = builder.start();
        Process p1 = builder1.start();

        String url = env("DB_URL", "jdbc:mysql://localhost:3306/sd");
        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");

        int uploaded = 0;
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = conn.prepareStatement("INSERT INTO ds (Name, Version) VALUES (?, ?)");
             BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
             BufferedReader r1 = new BufferedReader(new InputStreamReader(p1.getInputStream())))
        {
            while (true)
            {
                String line = r.readLine();
                String line1 = r1.readLine();
                if (line == null && line1 == null) break;
                line = line == null ? "" : line.trim();
                line1 = line1 == null ? "" : line1.trim();
                if (line.equals("Name") || line1.equals("Version")) continue;   // wmic header
                if (line.isEmpty() && line1.isEmpty()) continue;
                System.out.println(line + "--" + line1);
                try
                {
                    statement.setString(1, line);
                    statement.setString(2, line1);
                    if (statement.executeUpdate() > 0)
                    {
                        uploaded++;
                        System.out.println("uploaded");
                    }
                }
                catch (SQLException e)
                {
                    System.err.println("Could not insert '" + line + "': " + e.getMessage());
                }
            }
        }
        System.out.println(uploaded + " applications recorded");
    }
}
