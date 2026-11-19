# User Activity Tracer

A Windows utility that records which applications are running and which are installed on a machine, storing time-stamped snapshots in MySQL for later auditing.

B.Tech. Computer Science and Engineering, University of Petroleum and Energy Studies, Dehradun. Minor Project II (2018), by Divya Arora and Shailendra, supervised by Dr Hitesh Kumar Sharma.

**Published as:** H. K. Sharma, Shailendra, D. Arora. "User Activity Tracer: An Application to Record User Activities on Windows Platform." *International Journal of Research and Analytical Reviews (IJRAR)*, 5(3), pp. 758–763, July 2018. IJRAR1903093.

## How it works

| Program | Source of data | Stored in |
|---|---|---|
| `running_processes` | `tasklist /FO csv`: image name, PID, session name, session number | table `rp`, with the snapshot date and time |
| `application` | `wmic product get name` and `wmic product get version` | table `ds` (name, version) |

Both programs run the Windows command through `ProcessBuilder`, parse its output, and insert each row over JDBC.

## Repository contents

| Path | Description |
|---|---|
| `code.docx` | Original source listing for both programs |
| `src/running_processes.java` | Running-process logger, extracted from `code.docx` and corrected (see below) |
| `src/application.java` | Installed-application logger, extracted from `code.docx` and corrected |
| `sql/schema.sql` | Database and tables matching the original INSERT statements |
| `README.md` | Original title-only README |

## Running it

Requirements: Windows, Java 11+, a MySQL server, and [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/).

```bat
mysql -u root < sql\schema.sql
javac -d out src\*.java
set DB_PASSWORD=your_password
java -cp out;mysql-connector-j.jar running_processes
java -cp out;mysql-connector-j.jar application
```

`DB_URL`, `DB_USER` and `DB_PASSWORD` default to the original settings (`jdbc:mysql://localhost:3306/sd`, `root`, empty password). `wmic` is deprecated and missing from some recent Windows 11 builds, so `application` needs a system where it is still available.

## Changes from the original listing

The code in `code.docx` is preserved as-is. The versions in `src/` fix these defects:

- **`running_processes`:** the read loop never ended. When `tasklist` output ran out, `readLine()` returned `null` and the program crashed with a `NullPointerException`.
- **`running_processes`:** the `INSERT` sat inside the per-token loop, so each process was written four times, three of them with incomplete fields. It also printed `a[i]` after incrementing `i`, which output `null`.
- **Both programs:** SQL was built by concatenating strings, so any application name containing an apostrophe broke the statement. Values are now bound as `PreparedStatement` parameters.
- **`application`:** SQL errors were silently swallowed; they are now reported. The `wmic` header lines are skipped.

Both files compile with `javac -Xlint:all` (JDK 21) without warnings, and the CSV parser was checked against sample `tasklist` output. They have not been run end-to-end against MySQL on Windows.
