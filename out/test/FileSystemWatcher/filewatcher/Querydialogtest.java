package filewatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for {@link QueryDialog}.
 *
 * <p>Because QueryDialog is a Swing modal dialog, these tests
 * exercise the underlying {@link QueryEngine} that QueryDialog
 * delegates to, verifying the full query-export-email pipeline
 * that the dialog exposes to the user.</p>
 *
 * <p>GUI construction is not tested directly here since modal
 * dialogs block the test thread. The business logic reachable
 * through QueryEngine is fully covered.</p>
 *
 * @author Mariam Hussein & Nasra Hussein
 * @version 1.0
 */
class QueryDialogTest {

    // ─────────────────────────────────────────────────────────────
    // FIELDS
    // ─────────────────────────────────────────────────────────────

    /** DatabaseHandler shared across tests. */
    private DatabaseHandler dbHandler;

    /** ReportGenerator shared across tests. */
    private ReportGenerator reportGenerator;

    /** QueryEngine that QueryDialog delegates to. */
    private QueryEngine queryEngine;

    /** Fixed timestamp used across test events. */
    private static final LocalDateTime BASE_TIME =
            LocalDateTime.of(2026, 5, 10, 9, 0, 0);

    // ─────────────────────────────────────────────────────────────
    // SETUP / TEARDOWN
    // ─────────────────────────────────────────────────────────────

    /**
     * Opens a fresh database, seeds test data, and builds the QueryEngine
     * before each test. EmailService is null because email sending is
     * outside the scope of these tests.
     */
    @BeforeEach
    void setUp() {
        dbHandler = new DatabaseHandler();
        dbHandler.openConnection();
        dbHandler.createTableIfNotExists();

        // seed 5 varied events
        dbHandler.saveEvent(new FileEvent(
                "report.pdf", ".pdf",
                "/home/user/docs/report.pdf",
                "CREATED", BASE_TIME));
        dbHandler.saveEvent(new FileEvent(
                "notes.txt", ".txt",
                "/home/user/docs/notes.txt",
                "MODIFIED", BASE_TIME.plusHours(1)));
        dbHandler.saveEvent(new FileEvent(
                "photo.jpg", ".jpg",
                "/home/user/pictures/photo.jpg",
                "CREATED", BASE_TIME.plusHours(2)));
        dbHandler.saveEvent(new FileEvent(
                "archive.zip", ".zip",
                "/home/user/downloads/archive.zip",
                "DELETED", BASE_TIME.plusDays(1)));
        dbHandler.saveEvent(new FileEvent(
                "readme.txt", ".txt",
                "/home/user/docs/readme.txt",
                "CREATED", BASE_TIME.plusDays(2)));

        reportGenerator = new ReportGenerator();
        queryEngine = new QueryEngine(dbHandler, reportGenerator, null);
    }

    /**
     * Closes the database connection and deletes the test database file
     * after each test.
     */
    @AfterEach
    void tearDown() {
        dbHandler.closeConnection();
        new File("filewatcher.db").delete();

        // clean up any CSV files created during tests
        new File("dialog_test.csv").delete();
        new File("dialog_test.csv.csv").delete();
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BY EXTENSION
    // ─────────────────────────────────────────────────────────────

    /**
     * QueryEngine.queryByExtension returns the correct count,
     * which is what the dialog's results table would display.
     */
    @Test
    void queryByExtension_txtFiles_returnsTwoResults() {
        final var results = queryEngine.queryByExtension(".txt");
        assertEquals(2, results.size(),
                "Dialog table should show 2 .txt events.");
    }

    /**
     * QueryEngine.queryByExtension returns empty for an extension
     * not in the database, so the dialog would show 'No results found'.
     */
    @Test
    void queryByExtension_unknownExtension_returnsEmpty() {
        final var results = queryEngine.queryByExtension(".docx");
        assertTrue(results.isEmpty(),
                "Dialog should show no results for .docx.");
    }

    /**
     * QueryEngine.queryByExtension throws for blank input,
     * which the dialog catches and shows as an error message.
     */
    @Test
    void queryByExtension_blankInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.queryByExtension("  "),
                "Dialog should display an error for blank extension.");
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BY ACTIVITY
    // ─────────────────────────────────────────────────────────────

    /**
     * Querying by CREATED should return the three events seeded with
     * that activity type.
     */
    @Test
    void queryByActivity_created_returnsThreeResults() {
        final var results = queryEngine.queryByActivity("CREATED");
        assertEquals(3, results.size(),
                "Dialog table should show 3 CREATED events.");
    }

    /**
     * Querying by DELETED returns the single seeded deleted event.
     */
    @Test
    void queryByActivity_deleted_returnsOneResult() {
        final var results = queryEngine.queryByActivity("DELETED");
        assertEquals(1, results.size(),
                "Dialog table should show 1 DELETED event.");
    }

    /**
     * Querying by RENAMED returns empty since no such events exist,
     * triggering the 'No results found' status label.
     */
    @Test
    void queryByActivity_renamed_returnsEmpty() {
        final var results = queryEngine.queryByActivity("RENAMED");
        assertTrue(results.isEmpty(),
                "Dialog should show no results for RENAMED.");
    }

    /**
     * Null activity throws, which the dialog catches as an error.
     */
    @Test
    void queryByActivity_nullInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.queryByActivity(null),
                "Dialog should display an error for null activity.");
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BY PATH
    // ─────────────────────────────────────────────────────────────

    /**
     * Querying by 'docs' matches all three events under /home/user/docs/.
     */
    @Test
    void queryByPath_docs_returnsThreeResults() {
        final var results = queryEngine.queryByPath("docs");
        assertEquals(3, results.size(),
                "Dialog table should show 3 events with 'docs' in path.");
    }

    /**
     * Querying by 'pictures' returns the single photo event.
     */
    @Test
    void queryByPath_pictures_returnsOneResult() {
        final var results = queryEngine.queryByPath("pictures");
        assertEquals(1, results.size(),
                "Dialog table should show 1 event with 'pictures' in path.");
    }

    /**
     * Querying by a non-matching path returns empty.
     */
    @Test
    void queryByPath_noMatch_returnsEmpty() {
        final var results = queryEngine.queryByPath("nonexistent");
        assertTrue(results.isEmpty(),
                "Dialog should show no results for non-matching path.");
    }

    /**
     * Blank path throws, which the dialog catches and displays as an error.
     */
    @Test
    void queryByPath_blankInput_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.queryByPath(""),
                "Dialog should display an error for blank path.");
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY BY DATE RANGE
    // ─────────────────────────────────────────────────────────────

    /**
     * Date range covering the first day returns the three events
     * seeded within that day.
     */
    @Test
    void queryByDateRange_firstDay_returnsThreeResults() {
        final var start = BASE_TIME.withHour(0).withMinute(0).withSecond(0);
        final var end   = BASE_TIME.withHour(23).withMinute(59).withSecond(59);
        final var results = queryEngine.queryByDateRange(start, end);
        assertEquals(3, results.size(),
                "Dialog table should show 3 events on the first day.");
    }

    /**
     * Date range covering only the second day returns the archive event.
     */
    @Test
    void queryByDateRange_secondDay_returnsOneResult() {
        final var start = BASE_TIME.plusDays(1).withHour(0);
        final var end   = BASE_TIME.plusDays(1).withHour(23).withMinute(59);
        final var results = queryEngine.queryByDateRange(start, end);
        assertEquals(1, results.size(),
                "Dialog table should show 1 event on the second day.");
    }

    /**
     * Start after end throws, which the dialog catches as a validation error.
     */
    @Test
    void queryByDateRange_startAfterEnd_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.queryByDateRange(
                        BASE_TIME.plusDays(5), BASE_TIME),
                "Dialog should show error when start is after end.");
    }

    /**
     * Null start throws, which the dialog catches as a validation error.
     */
    @Test
    void queryByDateRange_nullStart_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.queryByDateRange(null, BASE_TIME),
                "Dialog should show error for null start date.");
    }

    // ─────────────────────────────────────────────────────────────
    // EXPORT CSV (button behaviour)
    // ─────────────────────────────────────────────────────────────

    /**
     * After a successful query, saveResultsToCsv creates a file on disk,
     * which is what the Export button does after the file chooser returns.
     */
    @Test
    void exportCsv_afterQuery_createsFile() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("dialog_test.csv");
        assertNotNull(csv, "Export should return a non-null File.");
        assertTrue(csv.exists(), "CSV file should exist after export.");
    }

    /**
     * Exporting with no prior query throws IllegalStateException,
     * which the dialog catches and shows as an error.
     */
    @Test
    void exportCsv_noQueryFirst_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> queryEngine.saveResultsToCsv("dialog_test.csv"),
                "Dialog should show error when exporting with no query results.");
    }

    /**
     * The export appends .csv automatically when the user omits it,
     * matching the dialog's file chooser default behaviour.
     */
    @Test
    void exportCsv_missingExtension_appendsCsv() {
        queryEngine.queryByActivity("CREATED");
        final File csv = queryEngine.saveResultsToCsv("dialog_test");
        assertNotNull(csv);
        assertTrue(csv.getName().endsWith(".csv"),
                "File name should end with .csv.");
        csv.delete();
    }

    // ─────────────────────────────────────────────────────────────
    // EMAIL RESULTS (button behaviour)
    // ─────────────────────────────────────────────────────────────

    /**
     * Calling emailResults with no EmailService configured returns false,
     * so the dialog shows a failure message rather than crashing.
     */
    @Test
    void emailResults_nullEmailService_returnsFalse() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("dialog_test.csv");
        assertFalse(queryEngine.emailResults("someone@example.com", csv),
                "Email button should show failure when EmailService is null.");
    }

    /**
     * Calling emailResults with a null recipient throws,
     * which the dialog catches before even trying to send.
     */
    @Test
    void emailResults_nullRecipient_throwsIllegalArgument() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("dialog_test.csv");
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.emailResults(null, csv),
                "Dialog should show error for null recipient.");
    }

    /**
     * Calling emailResults with a null file throws,
     * matching the guard in the dialog before the file chooser result is used.
     */
    @Test
    void emailResults_nullFile_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.emailResults("someone@example.com", null),
                "Dialog should show error for null file.");
    }

    // ─────────────────────────────────────────────────────────────
    // CLEAR DATABASE (button behaviour)
    // ─────────────────────────────────────────────────────────────

    /**
     * clearDatabase removes all rows, so the dialog's table would
     * show zero results after the user confirms the clear action.
     */
    @Test
    void clearDatabase_removesAllRecords() {
        assertEquals(5, dbHandler.fetchAll().size(),
                "Should have 5 events before clearing.");
        assertTrue(queryEngine.clearDatabase(),
                "Clear should return true.");
        assertTrue(dbHandler.fetchAll().isEmpty(),
                "Database should be empty after clear.");
    }

    /**
     * clearDatabase also resets the cache, so the status label and
     * Export button in the dialog would be reset too.
     */
    @Test
    void clearDatabase_resetsCachedResults() {
        queryEngine.queryByExtension(".txt");
        assertFalse(queryEngine.getLastQueryResults().isEmpty(),
                "Cache should have results before clear.");

        queryEngine.clearDatabase();

        assertTrue(queryEngine.getLastQueryResults().isEmpty(),
                "Cache should be empty after clear.");
        assertEquals("", queryEngine.getLastQueryMeta(),
                "Meta should be empty after clear.");
    }
}