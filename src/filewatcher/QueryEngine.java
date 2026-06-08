package filewatcher;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QueryEngine is the orchestration service layer for the File System Watcher.
 * It coordinates {@link DatabaseHandler}, {@link ReportGenerator}, and
 * {@link EmailService} to execute queries, export results to CSV,
 * and email reports.
 *
 * <p>QueryEngine caches the results and metadata of the last executed query
 * so that subsequent export or email operations can use them without
 * re-querying the database.</p>
 *
 * <p><b>SRS Coverage:</b></p>
 * <ul>
 *   <li>FR-3.1 through FR-3.8 — Database querying by extension, date,
 *       activity, path, and clear.</li>
 *   <li>FR-4.1 through FR-4.8 — CSV export with metadata header.</li>
 *   <li>FR-5.1 through FR-5.8 — Email delivery of CSV reports.</li>
 * </ul>
 *
 * @author Nasra Hussein
 * @version 1.0
 */
public class QueryEngine {

    // ═══════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════

    /** The database handler used for all database operations. */
    private final DatabaseHandler dbHandler;

    /** The report generator used for CSV export and table formatting. */
    private final ReportGenerator reportGenerator;

    /** The email service used for sending CSV attachments. */
    private final EmailService emailService;

    /**
     * Cached results from the last executed query.
     * Allows export/email without re-querying.
     */
    private List<FileEvent> lastQueryResults;

    /**
     * Cached metadata string describing the last executed query.
     * Example: "Query Type: Extension | Parameter: .pdf"
     */
    private String lastQueryMeta;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Constructs a QueryEngine with the given service dependencies.
     *
     * @param db the {@link DatabaseHandler} for database operations
     * @param rg the {@link ReportGenerator} for CSV export and formatting
     * @param es the {@link EmailService} for sending email reports
     * @throws IllegalArgumentException if db or rg is null
     */
    public QueryEngine(final DatabaseHandler db,
                       final ReportGenerator rg,
                       final EmailService es) {
        if (db == null) {
            throw new IllegalArgumentException("DatabaseHandler cannot be null.");
        }
        if (rg == null) {
            throw new IllegalArgumentException("ReportGenerator cannot be null.");
        }
        this.dbHandler = db;
        this.reportGenerator = rg;
        this.emailService = es;
        this.lastQueryResults = new ArrayList<>();
        this.lastQueryMeta = "";
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Queries all file events that match the given file extension.
     * Results and metadata are cached for later export or email.
     *
     * <p><b>SRS:</b> FR-3.1 — query-by-extension function.</p>
     *
     * @param ext the file extension to search for (e.g., ".pdf", ".java")
     * @return a list of matching {@link FileEvent} objects; empty if none found
     * @throws IllegalArgumentException if ext is null or blank
     */
    public List<FileEvent> queryByExtension(final String ext) {
        if (ext == null || ext.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension cannot be null or empty.");
        }
        lastQueryResults = dbHandler.fetchByExtension(ext.trim());
        lastQueryMeta = "Query Type: Extension | Parameter: " + ext.trim();
        return lastQueryResults;
    }

    /**
     * Queries all file events within the given date/time range (inclusive).
     * Results and metadata are cached for later export or email.
     *
     * <p><b>SRS:</b> FR-3.2 — query-by-date-range function.</p>
     * <p><b>SRS:</b> FR-3.7 — start date must be before end date.</p>
     *
     * @param start the beginning of the date range (inclusive)
     * @param end   the end of the date range (inclusive)
     * @return a list of matching {@link FileEvent} objects; empty if none found
     * @throws IllegalArgumentException if start or end is null,
     *         or if start is after end
     */
    public List<FileEvent> queryByDateRange(final LocalDateTime start,
                                            final LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date must be before or equal to end date.");
        }
        lastQueryResults = dbHandler.fetchByDateRange(start, end);
        lastQueryMeta = "Query Type: Date Range | From: " + start + " To: " + end;
        return lastQueryResults;
    }

    /**
     * Queries all file events that match the given activity type.
     * Results and metadata are cached for later export or email.
     *
     * <p><b>SRS:</b> FR-3.3 — query-by-activity function.</p>
     *
     * @param activity the activity type (e.g., "CREATED", "MODIFIED",
     *                 "DELETED", "RENAMED")
     * @return a list of matching {@link FileEvent} objects; empty if none found
     * @throws IllegalArgumentException if activity is null or blank
     */
    public List<FileEvent> queryByActivity(final String activity) {
        if (activity == null || activity.trim().isEmpty()) {
            throw new IllegalArgumentException("Activity type cannot be null or empty.");
        }
        lastQueryResults = dbHandler.fetchByActivity(activity.trim());
        lastQueryMeta = "Query Type: Activity | Parameter: " + activity.trim();
        return lastQueryResults;
    }

    /**
     * Queries all file events whose path contains the given search term.
     * Wraps the path in SQL LIKE wildcards (%) for partial matching.
     * Results and metadata are cached for later export or email.
     *
     * <p><b>SRS:</b> FR-3.4 — query-by-path function.</p>
     *
     * @param path the path search term (e.g., "Documents")
     * @return a list of matching {@link FileEvent} objects; empty if none found
     * @throws IllegalArgumentException if path is null or blank
     */
    public List<FileEvent> queryByPath(final String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty.");
        }
        lastQueryResults = dbHandler.fetchByPath(path.trim());
        lastQueryMeta = "Query Type: Path | Parameter: " + path.trim();
        return lastQueryResults;
    }

    // ═══════════════════════════════════════════════════════════════
    // CACHE ACCESSORS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the cached results from the most recent query.
     *
     * @return the last query results, or an empty list if no query
     *         has been executed yet
     */
    public List<FileEvent> getLastQueryResults() {
        return lastQueryResults;
    }

    /**
     * Returns the metadata string describing the most recent query.
     *
     * @return the last query metadata, or an empty string if no query
     *         has been executed yet
     */
    public String getLastQueryMeta() {
        return lastQueryMeta;
    }

    // ═══════════════════════════════════════════════════════════════
    // EXPORT & EMAIL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Exports the cached query results to a CSV file with a metadata header.
     * The file name will have ".csv" appended if not already present.
     *
     * <p><b>SRS:</b> FR-4.1 through FR-4.8 — CSV export with metadata.</p>
     *
     * @param fileName the desired file name for the CSV export
     * @return the created {@link File}, or {@code null} if export failed
     * @throws IllegalArgumentException if fileName is null or blank
     * @throws IllegalStateException    if no query has been executed yet
     */
    public File saveResultsToCsv(final String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
        if (lastQueryResults.isEmpty()) {
            throw new IllegalStateException(
                    "No query results to export. Run a query first.");
        }

        // Append .csv if not present (FR-4.3)
        String safeName = fileName.trim();
        if (!safeName.toLowerCase().endsWith(".csv")) {
            safeName = safeName + ".csv";
        }

        try {
            reportGenerator.exportToCsv(lastQueryResults, safeName);
            return new File(safeName);
        } catch (IOException e) {
            System.err.println("Failed to export CSV: " + e.getMessage());
            return null;
        }
    }

    /**
     * Emails the given CSV file to the specified recipient using
     * the configured {@link EmailService}.
     *
     * <p><b>SRS:</b> FR-5.1 through FR-5.8 — Email delivery of CSV reports.</p>
     *
     * @param recipient the email address of the recipient
     * @param file      the CSV file to attach
     * @return {@code true} if the email was sent successfully,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if recipient is null/blank or file is null
     */
    public boolean emailResults(final String recipient, final File file) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty.");
        }
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        if (emailService == null) {
            System.err.println("EmailService is not configured.");
            return false;
        }
        return emailService.sendEmail(recipient.trim(), file);
    }

    /**
     * Clears all records from the database.
     *
     * <p><b>SRS:</b> FR-3.8 — Clear Database option.</p>
     *
     * @return {@code true} if the database was cleared successfully,
     *         {@code false} otherwise
     */
    public boolean clearDatabase() {
        final boolean result = dbHandler.clearDatabase();
        if (result) {
            lastQueryResults = new ArrayList<>();
            lastQueryMeta = "";
        }
        return result;
    }
}
