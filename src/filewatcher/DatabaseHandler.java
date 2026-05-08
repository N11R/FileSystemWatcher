package filewatcher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQLite database operations for the File System Watcher.
 * Provides methods to connect, create tables, save file events,
 * query by various criteria, and clear the database.
 *
 * <p>Implements requirements FR-2.1 through FR-2.7.</p>
 *
 * @author Nasra Hussein
 * @version 1.0
 */
public class DatabaseHandler {

    /** JDBC connection string pointing to the SQLite database file. */
    private final String connectionString = "jdbc:sqlite:filewatcher.db";

    /** Active database connection. */
    private Connection connection;

    /** Formatter for storing and parsing timestamps in the database. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructs a new DatabaseHandler with default settings.
     */
    public DatabaseHandler() {

    }

    /**
     * Opens a connection to the SQLite database.
     * Creates the database file if it does not already exist.
     *
     * @return true if the connection was successful, false otherwise
     */
    public boolean openConnection() {
        try {
            connection = DriverManager.getConnection(connectionString);
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    /**
     * Closes the active database connection.
     * Does nothing if the connection is already closed or null.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }
    }

    /**
     * Creates the file_activity table if it does not already exist.
     * Called on first launch to initialize the database schema.
     *
     * <p>Implements FR-2.3.</p>
     */
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS file_activity ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "file_name TEXT NOT NULL, "
                + "file_extension TEXT, "
                + "file_path TEXT NOT NULL, "
                + "activity_type TEXT NOT NULL, "
                + "activity_time DATETIME NOT NULL"
                + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Failed to create table: " + e.getMessage());
        }
    }

    /**
     * Saves a single FileEvent to the database.
     *
     * <p>Implements FR-2.2.</p>
     *
     * @param event the FileEvent to save
     * @return true if the event was saved successfully, false otherwise
     */
    public boolean saveEvent(FileEvent event) {
        String sql = "INSERT INTO file_activity "
                + "(file_name, file_extension, file_path, activity_type, activity_time) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, event.getFileName());
            pstmt.setString(2, event.getExtension());
            pstmt.setString(3, event.getPath());
            pstmt.setString(4, event.getActivityType());
            pstmt.setString(5, event.getTimeStamp().format(FORMATTER));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to save event: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves a list of FileEvents to the database in batch.
     * Called when the user clicks the "Write to Database" button.
     *
     * <p>Implements FR-2.4.</p>
     *
     * @param events the list of FileEvents to save
     * @return the number of events successfully saved
     */
    public int saveEvents(List<FileEvent> events) {
        int saved = 0;
        for (FileEvent event : events) {
            if (saveEvent(event)) {
                saved++;
            }
        }
        return saved;
    }

    /**
     * Retrieves all file events from the database.
     *
     * @return a list of all FileEvents, or an empty list if none exist
     */
    public List<FileEvent> fetchAll() {
        String sql = "SELECT * FROM file_activity";
        return executeQuery(sql);
    }

    /**
     * Retrieves file events filtered by file extension.
     *
     * <p>Implements FR-3.1.</p>
     *
     * @param extension the file extension to filter by (e.g., ".pdf")
     * @return a list of matching FileEvents, or an empty list if none match
     */
    public List<FileEvent> fetchByExtension(String extension) {
        String sql = "SELECT * FROM file_activity WHERE file_extension = ?";
        return executeQueryWithParam(sql, extension);
    }

    /**
     * Retrieves file events within a specified date and time range.
     *
     * <p>Implements FR-3.2.</p>
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate   the end of the date range (inclusive)
     * @return a list of matching FileEvents, or an empty list if none match
     */
    public List<FileEvent> fetchByDateRange(LocalDateTime startDate,
                                            LocalDateTime endDate) {
        String sql = "SELECT * FROM file_activity "
                + "WHERE activity_time BETWEEN ? AND ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, startDate.format(FORMATTER));
            pstmt.setString(2, endDate.format(FORMATTER));
            return extractEvents(pstmt.executeQuery());
        } catch (SQLException e) {
            System.err.println("Failed to fetch by date: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves file events filtered by activity type.
     *
     * <p>Implements FR-3.3.</p>
     *
     * @param activity the activity type to filter by
     *                 (e.g., "created", "modified", "deleted")
     * @return a list of matching FileEvents, or an empty list if none match
     */
    public List<FileEvent> fetchByActivity(String activity) {
        String sql = "SELECT * FROM file_activity WHERE activity_type = ?";
        return executeQueryWithParam(sql, activity);
    }

    /**
     * Retrieves file events whose file path contains the specified string.
     *
     * <p>Implements FR-3.4.</p>
     *
     * @param path the path or partial path to search for
     * @return a list of matching FileEvents, or an empty list if none match
     */
    public List<FileEvent> fetchByPath(String path) {
        String sql = "SELECT * FROM file_activity WHERE file_path LIKE ?";
        return executeQueryWithParam(sql, "%" + path + "%");
    }

    /**
     * Deletes all records from the file_activity table.
     *
     * <p>Implements FR-3.8.</p>
     *
     * @return true if the database was cleared successfully, false otherwise
     */
    public boolean clearDatabase() {
        String sql = "DELETE FROM file_activity";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to clear database: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a SQL query with no parameters and returns the results.
     *
     * @param sql the SQL SELECT statement to execute
     * @return a list of FileEvents from the result set
     */
    private List<FileEvent> executeQuery(String sql) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return extractEvents(rs);
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Executes a SQL query with one string parameter and returns the results.
     *
     * @param sql   the SQL SELECT statement with one ? placeholder
     * @param param the value to bind to the placeholder
     * @return a list of FileEvents from the result set
     */
    private List<FileEvent> executeQueryWithParam(String sql, String param) {
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, param);
            return extractEvents(pstmt.executeQuery());
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Converts a ResultSet into a list of FileEvent objects.
     *
     * @param rs the ResultSet to process
     * @return a list of FileEvents extracted from the result set
     * @throws SQLException if a database access error occurs
     */
    private List<FileEvent> extractEvents(ResultSet rs) throws SQLException {
        List<FileEvent> events = new ArrayList<>();
        while (rs.next()) {
            FileEvent event = new FileEvent(
                    rs.getString("file_name"),
                    rs.getString("file_extension"),
                    rs.getString("file_path"),
                    rs.getString("activity_type"),
                    LocalDateTime.parse(
                            rs.getString("activity_time"), FORMATTER)
            );
            events.add(event);
        }
        return events;
    }
}
