package at.technikum_wien;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class Database {
    // Datenbank-Verbindungsdetails (ohne Passwort)
    private static final String URL = "jdbc:postgresql://localhost:5432/mctg";
    private static final String USER = "mctg_user1";
    private static final String PASSWORD = ""; // Passwort weglassen, falls keins gesetzt ist

    static {
        try {
            // PostgreSQL JDBC-Treiber laden
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stellt eine Verbindung zur Datenbank her und gibt das Connection-Objekt zurück.
     *
     * @return Connection zur Datenbank
     * @throws SQLException Wenn die Verbindung fehlschlägt
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String getCurrentDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT current_database()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
