-- Tables used by src/running_processes.java and src/application.java.
-- Column names follow the INSERT statements in the original code (code.docx).
CREATE DATABASE IF NOT EXISTS sd;
USE sd;

CREATE TABLE IF NOT EXISTS rp (
    NAME     VARCHAR(260),   -- image name, e.g. chrome.exe
    PID      VARCHAR(16),
    SESSION  VARCHAR(64),    -- session name, e.g. Console
    SESSIONH VARCHAR(16),    -- session number
    TIME     VARCHAR(8),     -- HH:mm:ss when the snapshot was taken
    DATE     VARCHAR(10)     -- dd-MM-yyyy
);

CREATE TABLE IF NOT EXISTS ds (
    Name    VARCHAR(512),
    Version VARCHAR(64)
);
