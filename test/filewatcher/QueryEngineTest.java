package filewatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test class for {@link QueryEngine}.
 *
 * <p>Tests cover all query methods, input validation,
 * caching behavior, CSV export, and database clearing.</p>
 *
 * <p>Uses a real SQLite database (in-memory via a temp file)
 * with pre-loaded test data for integration-level testing.</p>
 *
 * @author Nasra Hussein
 * @version 1.0
 */
class QueryEngineTest {

    /** The QueryEngine instance under test. */
    private QueryEngine queryEngine;

    /** The DatabaseHandler used by the QueryEngine. */
    private DatabaseHandler dbHandler;

    /** A fixed timestamp used in test events. */
    private static final LocalDateTime SAMPLE_TIME =
            LocalDateTime.of(2026, 5, 7, 18, 30, 0);

    /**
     * Sets up the test environment before each test.
     * Creates a DatabaseHandler, opens a connection, creates the table,
     * loads test data, and constructs the QueryEngine.
     */
    @BeforeEach
    void setUp() {
        dbHandler = new DatabaseHandler();
        dbHandler.openConnection();
        dbHandler.createTableIfNotExists();

        // Load test data
        dbHandler.saveEvent(new FileEvent(
                "document.txt", ".txt",
                "/Users/nasra/Documents/document.txt",
                "CREATED", SAMPLE_TIME));
        dbHandler.saveEvent(new FileEvent(
                "photo.jpg", ".jpg",
                "/Users/nasra/Pictures/photo.jpg",
                "MODIFIED",
                SAMPLE_TIME.plusHours(1)));
        dbHandler.saveEvent(new FileEvent(
                "report.pdf", ".pdf",
                "/Users/nasra/Downloads/report.pdf",
                "CREATED",
                SAMPLE_TIME.plusDays(1)));
        dbHandler.saveEvent(new FileEvent(
                "notes.txt", ".txt",
                "/Users/nasra/Documents/notes.txt",
                "DELETED",
                SAMPLE_TIME.plusDays(2)));

        final ReportGenerator rg = new ReportGenerator();
        queryEngine = new QueryEngine(dbHandler, rg, null);
    }

    /**
     * Tears down the test environment after each test.
     */
    @AfterEach
    void tearDown() {
        dbHandler.closeConnection();
        final File dbFile = new File("filewatcher.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
        // Clean up any CSV files created during tests
        final File csv = new File("test_export.csv");
        if (csv.exists()) {
            csv.delete();
        }
    }



    /**
     * Tests that the constructor throws when DatabaseHandler is null.
     */
    @Test
    void testConstructorNullDbThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        new QueryEngine(null, new ReportGenerator(), null),
                "Should throw for null DatabaseHandler.");
    }

    /**
     * Tests that the constructor throws when ReportGenerator is null.
     */
    @Test
    void testConstructorNullReportGeneratorThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        new QueryEngine(dbHandler, null, null),
                "Should throw for null ReportGenerator.");
    }



    /**
     * Tests queryByExtension returns matching events.
     */
    @Test
    void testQueryByExtensionFindsMatches() {
        final List<FileEvent> results = queryEngine.queryByExtension(".txt");
        assertEquals(2, results.size(),
                "Should find 2 .txt events.");
    }

    /**
     * Tests queryByExtension returns empty for non-existent extension.
     */
    @Test
    void testQueryByExtensionNoMatch() {
        final List<FileEvent> results = queryEngine.queryByExtension(".xyz");
        assertTrue(results.isEmpty(),
                "Should return empty for .xyz extension.");
    }

    /**
     * Tests queryByExtension throws for null input.
     */
    @Test
    void testQueryByExtensionNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByExtension(null),
                "Should throw for null extension.");
    }

    /**
     * Tests queryByExtension throws for blank input.
     */
    @Test
    void testQueryByExtensionBlankThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByExtension("   "),
                "Should throw for blank extension.");
    }



    /**
     * Tests queryByDateRange returns events within range.
     */
    @Test
    void testQueryByDateRangeFindsMatches() {
        final LocalDateTime start = LocalDateTime.of(2026, 5, 7, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 7, 23, 59, 59);
        final List<FileEvent> results = queryEngine.queryByDateRange(start, end);
        assertEquals(2, results.size(),
                "Should find 2 events on May 7.");
    }

    /**
     * Tests queryByDateRange returns empty when no events in range.
     */
    @Test
    void testQueryByDateRangeNoMatch() {
        final LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0, 0);
        final List<FileEvent> results = queryEngine.queryByDateRange(start, end);
        assertTrue(results.isEmpty(),
                "Should return empty for date range with no events.");
    }

    /**
     * Tests queryByDateRange throws when start is after end.
     */
    @Test
    void testQueryByDateRangeStartAfterEndThrows() {
        final LocalDateTime start = LocalDateTime.of(2026, 5, 10, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 1, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByDateRange(start, end),
                "Should throw when start is after end.");
    }

    /**
     * Tests queryByDateRange throws for null start.
     */
    @Test
    void testQueryByDateRangeNullStartThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByDateRange(null, SAMPLE_TIME),
                "Should throw for null start date.");
    }

    /**
     * Tests queryByDateRange throws for null end.
     */
    @Test
    void testQueryByDateRangeNullEndThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByDateRange(SAMPLE_TIME, null),
                "Should throw for null end date.");
    }



    /**
     * Tests queryByActivity returns matching events.
     */
    @Test
    void testQueryByActivityFindsMatches() {
        final List<FileEvent> results = queryEngine.queryByActivity("CREATED");
        assertEquals(2, results.size(),
                "Should find 2 CREATED events.");
    }

    /**
     * Tests queryByActivity returns empty for non-existent type.
     */
    @Test
    void testQueryByActivityNoMatch() {
        final List<FileEvent> results = queryEngine.queryByActivity("RENAMED");
        assertTrue(results.isEmpty(),
                "Should return empty for RENAMED.");
    }

    /**
     * Tests queryByActivity throws for null input.
     */
    @Test
    void testQueryByActivityNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByActivity(null),
                "Should throw for null activity.");
    }



    /**
     * Tests queryByPath returns matching events.
     */
    @Test
    void testQueryByPathFindsMatches() {
        final List<FileEvent> results = queryEngine.queryByPath("Documents");
        assertEquals(2, results.size(),
                "Should find 2 events with Documents in path.");
    }

    /**
     * Tests queryByPath returns empty for non-matching path.
     */
    @Test
    void testQueryByPathNoMatch() {
        final List<FileEvent> results = queryEngine.queryByPath("nonexistent");
        assertTrue(results.isEmpty(),
                "Should return empty for non-matching path.");
    }

    /**
     * Tests queryByPath throws for null input.
     */
    @Test
    void testQueryByPathNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.queryByPath(null),
                "Should throw for null path.");
    }



    /**
     * Tests that getLastQueryResults returns cached results.
     */
    @Test
    void testCacheUpdatedAfterQuery() {
        queryEngine.queryByExtension(".pdf");
        final List<FileEvent> cached = queryEngine.getLastQueryResults();
        assertEquals(1, cached.size(),
                "Cached results should contain 1 event.");
    }

    /**
     * Tests that getLastQueryMeta returns the metadata string.
     */
    @Test
    void testCacheMetaUpdatedAfterQuery() {
        queryEngine.queryByActivity("DELETED");
        final String meta = queryEngine.getLastQueryMeta();
        assertTrue(meta.contains("Activity"),
                "Meta should contain query type.");
        assertTrue(meta.contains("DELETED"),
                "Meta should contain the parameter.");
    }

    /**
     * Tests that cache updates with each new query.
     */
    @Test
    void testCacheUpdatesOnNewQuery() {
        queryEngine.queryByExtension(".txt");
        assertEquals(2, queryEngine.getLastQueryResults().size(),
                "First query: 2 .txt events.");

        queryEngine.queryByExtension(".pdf");
        assertEquals(1, queryEngine.getLastQueryResults().size(),
                "Second query: 1 .pdf event.");
    }

    /**
     * Tests that initial cache is empty.
     */
    @Test
    void testInitialCacheEmpty() {
        assertTrue(queryEngine.getLastQueryResults().isEmpty(),
                "Initial cache should be empty.");
        assertEquals("", queryEngine.getLastQueryMeta(),
                "Initial meta should be empty string.");
    }



    /**
     * Tests saveResultsToCsv creates a file.
     */
    @Test
    void testSaveResultsToCsvCreatesFile() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("test_export.csv");
        assertNotNull(csv, "Should return a File object.");
        assertTrue(csv.exists(), "CSV file should exist on disk.");
    }

    /**
     * Tests saveResultsToCsv appends .csv if missing.
     */
    @Test
    void testSaveResultsToCsvAppendsCsvExtension() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("test_export");
        assertNotNull(csv, "Should return a File object.");
        assertTrue(csv.getName().endsWith(".csv"),
                "File name should end with .csv.");
        csv.delete();
    }

    /**
     * Tests saveResultsToCsv throws when no query has been run.
     */
    @Test
    void testSaveResultsToCsvNoQueryThrows() {
        assertThrows(IllegalStateException.class, () ->
                        queryEngine.saveResultsToCsv("test.csv"),
                "Should throw when no query results exist.");
    }

    /**
     * Tests saveResultsToCsv throws for null file name.
     */
    @Test
    void testSaveResultsToCsvNullNameThrows() {
        queryEngine.queryByExtension(".txt");
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.saveResultsToCsv(null),
                "Should throw for null file name.");
    }



    /**
     * Tests clearDatabase removes all records.
     */
    @Test
    void testClearDatabaseRemovesAllEvents() {
        assertEquals(4, dbHandler.fetchAll().size(),
                "Should have 4 events before clearing.");

        assertTrue(queryEngine.clearDatabase(),
                "clearDatabase should return true.");

        assertTrue(dbHandler.fetchAll().isEmpty(),
                "Database should be empty after clearing.");
    }

    /**
     * Tests clearDatabase also clears the cache.
     */
    @Test
    void testClearDatabaseClearsCache() {
        queryEngine.queryByExtension(".txt");
        assertFalse(queryEngine.getLastQueryResults().isEmpty(),
                "Cache should not be empty after query.");

        queryEngine.clearDatabase();
        assertTrue(queryEngine.getLastQueryResults().isEmpty(),
                "Cache should be empty after clearing database.");
        assertEquals("", queryEngine.getLastQueryMeta(),
                "Meta should be empty after clearing database.");
    }



    /**
     * Tests emailResults returns false when EmailService is null.
     */
    @Test
    void testEmailResultsNullServiceReturnsFalse() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("test_export.csv");
        assertFalse(queryEngine.emailResults("test@test.com", csv),
                "Should return false when EmailService is null.");
    }

    /**
     * Tests emailResults throws for null recipient.
     */
    @Test
    void testEmailResultsNullRecipientThrows() {
        queryEngine.queryByExtension(".txt");
        final File csv = queryEngine.saveResultsToCsv("test_export.csv");
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.emailResults(null, csv),
                "Should throw for null recipient.");
    }

    /**
     * Tests emailResults throws for null file.
     */
    @Test
    void testEmailResultsNullFileThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        queryEngine.emailResults("test@test.com", null),
                "Should throw for null file.");
    }
}
