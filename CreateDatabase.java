package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateDatabase {
    // You can override these with environment variables when running:
    // MYSQL_HOST, MYSQL_PORT, MYSQL_ADMIN_USER, MYSQL_ADMIN_PASS
    private static final String HOST = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
    private static final String PORT = System.getenv().getOrDefault("MYSQL_PORT", "3306");
    private static final String ADMIN_USER = System.getenv().getOrDefault("MYSQL_ADMIN_USER", "root");
    private static final String ADMIN_PASS = System.getenv().getOrDefault("MYSQL_ADMIN_PASS", "1234");

    public static void main(String[] args) {
        // Connect without selecting a default database
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            HOST, PORT
        );

        String[] statements = new String[] {
            // Create DB
            "CREATE DATABASE IF NOT EXISTS `mcqdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
            // Switch to DB
            "USE `mcqdb`",
            // Create user (requires admin privileges)
            "CREATE USER IF NOT EXISTS 'mcq'@'localhost' IDENTIFIED BY 'mcqpass'",
            "GRANT ALL PRIVILEGES ON `mcqdb`.* TO 'mcq'@'localhost'",
            "FLUSH PRIVILEGES",
            // Create table
            "CREATE TABLE IF NOT EXISTS `mcq_item` (" +
            "  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT," +
            "  question     VARCHAR(1000)   NOT NULL," +
            "  optionA      VARCHAR(500)    NOT NULL," +
            "  optionB      VARCHAR(500)    NOT NULL," +
            "  optionC      VARCHAR(500)    NOT NULL," +
            "  optionD      VARCHAR(500)    NOT NULL," +
            "  answer       CHAR(1)         NOT NULL," +
            "  created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  CONSTRAINT pk_mcq_item PRIMARY KEY (id)," +
            "  CONSTRAINT chk_answer_valid CHECK (answer IN ('A','B','C','D'))" +
            ") ENGINE=InnoDB"
        };

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ADMIN_USER, ADMIN_PASS);
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                System.out.println("Executing: " + oneLine(sql));
                stmt.execute(sql);
            }
            System.out.println("\n✅ Setup complete. Database `mcqdb` and table `mcq_item` are ready, user 'mcq'@'localhost' has privileges.");
        } catch (SQLException e) {
            System.err.println("❌ SQL error:");
            while (e != null) {
                System.err.printf("- SQLState: %s | ErrorCode: %d | Message: %s%n",
                        e.getSQLState(), e.getErrorCode(), e.getMessage());
                e = e.getNextException();
            }
            System.exit(1);
        }
    }

    private static String oneLine(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }
}

