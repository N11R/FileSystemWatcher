package filewatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test class for {@link DatabaseHandler}.
 *
 * <p>Tests cover all public methods including connection management,
 * table creation, event saving (single and batch), all query types,
 * and database clearing.</p>
 *
 * <p>Each test uses a temporary SQLite database file that is deleted
 * after the test completes to ensure test isolation.</p>
 *
 * @author Nasra Hussein
 * @version 1.0
 */
class DatabaseHandlerTest {

    /** The DatabaseHandler instance under test. */
    private DatabaseHandler dbHandler;

    /** Path to the temporary test database file. */
    private static final String TEST_DB = "test_filewatcher.db";

    /** A reusable sample FileEvent for testing. */
    private FileEvent sampleEvent;

    /** A fixed timestamp used in test events. */
    private static final LocalDateTime SAMPLE_TIME =
            LocalDateTime.of(2026, 5, 7, 18, 30, 0);

    /**
     * Sets up the test environment before each test.
     * Creates a fresh DatabaseHandler, opens a connection,
     * and creates the table.
     */
    @BeforeEach
    void setUp() {
        dbHandler = new DatabaseHandler();
        assertTrue(dbHandler.openConnection(),
                "Database connection should open successfully.");
        dbHandler.createTableIfNotExists();

        sampleEvent = new FileEvent(
                "document.txt",
                ".txt",
                "/Users/nasra/Documents/document.txt",
                "CREATED",
                SAMPLE_TIME
        );
    }

    /**
     * Tears down the test environment after each test.
     * Closes the connection and deletes the temporary database file.
     */
    @AfterEach
    void tearDown() {
        dbHandler.closeConnection();
        final File dbFile = new File(TEST_DB);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        // Also try to delete the default db file
        final File defaultDb = new File("filewatcher.db");
        if (defaultDb.exists()) {
            defaultDb.delete();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONNECTION TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests that openConnection returns true on success.
     */
    @Test
    void testOpenConnectionReturnsTrue() {
        // Connection already opened in setUp, close and reopen
        dbHandler.closeConnection();
        assertTrue(dbHandler.openConnection(),
                "openConnection should return true on success.");
    }

    /**
     * Tests that closeConnection does not throw an exception.
     */
    @Test
    void testCloseConnectionNoException() {
        dbHandler.closeConnection();
        // Calling close again should not throw
        dbHandler.closeConnection();
    }

    // ═══════════════════════════════════════════════════════════════
    // CREATE TABLE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests that createTableIfNotExists can be called multiple times
     * without error (IF NOT EXISTS behavior).
     */
    @Test
    void testCreateTableCalledTwiceNoError() {
        dbHandler.createTableIfNotExists();
        dbHandler.createTableIfNotExists();
        // If no exception, test passes
        final List<FileEvent> events = dbHandler.fetchAll();
        assertNotNull(events, "fetchAll should return a non-null list.");
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE EVENT TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests saving a single event returns true.
     */
    @Test
    void testSaveEventReturnsTrue() {
        assertTrue(dbHandler.saveEvent(sampleEvent),
                "saveEvent should return true for a valid event.");
    }

    /**
     * Tests that a saved event can be retrieved with fetchAll.
     */
    @Test
    void testSaveEventThenFetchAll() {
        dbHandler.saveEvent(sampleEvent);
        final List<FileEvent> events = dbHandler.fetchAll();
        assertEquals(1, events.size(),
                "fetchAll should return 1 event after saving 1.");
        assertEquals("document.txt", events.get(0).getFileName(),
                "File name should match the saved event.");
    }

    /**
     * Tests that saved event fields match the original.
     */
    @Test
    void testSaveEventFieldsMatch() {
        dbHandler.saveEvent(sampleEvent);
        final FileEvent fetched = dbHandler.fetchAll().get(0);

        assertEquals(sampleEvent.getFileName(), fetched.getFileName(),
                "File name should match.");
        assertEquals(sampleEvent.getExtension(), fetched.getExtension(),
                "Extension should match.");
        assertEquals(sampleEvent.getPath(), fetched.getPath(),
                "Path should match.");
        assertEquals(sampleEvent.getActivityType(), fetched.getActivityType(),
                "Activity type should match.");
        assertEquals(sampleEvent.getTimeStamp(), fetched.getTimeStamp(),
                "Timestamp should match.");
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE EVENTS (BATCH) TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests saving multiple events at once.
     */
    @Test
    void testSaveEventsReturnCount() {
        final List<FileEvent> events = createTestEvents();
        final int saved = dbHandler.saveEvents(events);
        assertEquals(3, saved,
                "saveEvents should return 3 for 3 valid events.");
    }

    /**
     * Tests that all batch-saved events are retrievable.
     */
    @Test
    void testSaveEventsThenFetchAll() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> fetched = dbHandler.fetchAll();
        assertEquals(3, fetched.size(),
                "fetchAll should return 3 events after batch save.");
    }

    /**
     * Tests saving an empty list returns zero.
     */
    @Test
    void testSaveEventsEmptyList() {
        final int saved = dbHandler.saveEvents(new ArrayList<>());
        assertEquals(0, saved,
                "saveEvents with empty list should return 0.");
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH ALL TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests fetchAll on an empty database returns empty list.
     */
    @Test
    void testFetchAllEmptyDatabase() {
        final List<FileEvent> events = dbHandler.fetchAll();
        assertNotNull(events, "fetchAll should never return null.");
        assertTrue(events.isEmpty(),
                "fetchAll should return empty list for empty database.");
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH BY EXTENSION TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests querying by extension returns matching events.
     */
    @Test
    void testFetchByExtensionFindsMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByExtension(".txt");
        assertEquals(1, results.size(),
                "Should find 1 .txt event.");
        assertEquals(".txt", results.get(0).getExtension(),
                "Extension should be .txt.");
    }

    /**
     * Tests querying by extension with no matches returns empty list.
     */
    @Test
    void testFetchByExtensionNoMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByExtension(".xyz");
        assertTrue(results.isEmpty(),
                "Should return empty list for non-existent extension.");
    }

    /**
     * Tests querying by extension returns multiple matches.
     */
    @Test
    void testFetchByExtensionMultipleMatches() {
        dbHandler.saveEvent(sampleEvent);
        dbHandler.saveEvent(new FileEvent(
                "notes.txt", ".txt", "/home/notes.txt",
                "MODIFIED", SAMPLE_TIME.plusHours(1)));
        final List<FileEvent> results = dbHandler.fetchByExtension(".txt");
        assertEquals(2, results.size(),
                "Should find 2 .txt events.");
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH BY DATE RANGE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests querying by date range returns events within range.
     */
    @Test
    void testFetchByDateRangeFindsMatch() {
        dbHandler.saveEvents(createTestEvents());
        final LocalDateTime start = LocalDateTime.of(2026, 5, 7, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 5, 7, 23, 59, 59);
        final List<FileEvent> results = dbHandler.fetchByDateRange(start, end);
        assertEquals(2, results.size(),
                "Should find 2 events on May 7.");
    }

    /**
     * Tests querying by date range with no matches returns empty list.
     */
    @Test
    void testFetchByDateRangeNoMatch() {
        dbHandler.saveEvents(createTestEvents());
        final LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2025, 1, 2, 0, 0, 0);
        final List<FileEvent> results = dbHandler.fetchByDateRange(start, end);
        assertTrue(results.isEmpty(),
                "Should return empty list for date range with no events.");
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH BY ACTIVITY TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests querying by activity type returns matching events.
     */
    @Test
    void testFetchByActivityFindsMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByActivity("CREATED");
        assertEquals(2, results.size(),
                "Should find 2 CREATED events.");
    }

    /**
     * Tests querying by activity type with no matches returns empty list.
     */
    @Test
    void testFetchByActivityNoMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByActivity("RENAMED");
        assertTrue(results.isEmpty(),
                "Should return empty list for RENAMED when none exist.");
    }

    // ═══════════════════════════════════════════════════════════════
    // FETCH BY PATH TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests querying by path returns matching events.
     */
    @Test
    void testFetchByPathFindsMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByPath("%Documents%");
        assertEquals(1, results.size(),
                "Should find 1 event with Documents in path.");
    }

    /**
     * Tests querying by path with no matches returns empty list.
     */
    @Test
    void testFetchByPathNoMatch() {
        dbHandler.saveEvents(createTestEvents());
        final List<FileEvent> results = dbHandler.fetchByPath("%nonexistent%");
        assertTrue(results.isEmpty(),
                "Should return empty for non-matching path.");
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEAR DATABASE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests clearing the database removes all records.
     */
    @Test
    void testClearDatabaseRemovesAll() {
        dbHandler.saveEvents(createTestEvents());
        assertEquals(3, dbHandler.fetchAll().size(),
                "Should have 3 events before clearing.");

        assertTrue(dbHandler.clearDatabase(),
                "clearDatabase should return true.");

        assertTrue(dbHandler.fetchAll().isEmpty(),
                "fetchAll should return empty after clearing.");
    }

    /**
     * Tests clearing an already empty database returns true.
     */
    @Test
    void testClearEmptyDatabaseReturnsTrue() {
        assertTrue(dbHandler.clearDatabase(),
                "clearDatabase on empty DB should return true.");
    }

    // ═══════════════════════════════════════════════════════════════
    // EDGE CASE TESTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tests saving an event with a null extension (file has no extension).
     */
    @Test
    void testSaveEventNullExtension() {
        final FileEvent noExt = new FileEvent(
                "README", null, "/home/README",
                "CREATED", SAMPLE_TIME);
        assertTrue(dbHandler.saveEvent(noExt),
                "Should save event with null extension.");
        final List<FileEvent> results = dbHandler.fetchAll();
        assertEquals(1, results.size(),
                "Should retrieve 1 event.");
    }

    /**
     * Tests saving and retrieving events with special characters in file names.
     */
    @Test
    void testSaveEventSpecialCharacters() {
        final FileEvent special = new FileEvent(
                "O'Reilly's file (copy).txt", ".txt",
                "/home/O'Reilly's file (copy).txt",
                "CREATED", SAMPLE_TIME);
        assertTrue(dbHandler.saveEvent(special),
                "Should save event with special characters.");
        final FileEvent fetched = dbHandler.fetchAll().get(0);
        assertEquals("O'Reilly's file (copy).txt", fetched.getFileName(),
                "File name with special characters should match.");
    }

    /**
     * Tests saving multiple events and fetching by different criteria.
     */
    @Test
    void testMultipleSavesAndQueries() {
        dbHandler.saveEvents(createTestEvents());

        assertEquals(3, dbHandler.fetchAll().size(),
                "fetchAll should return 3.");
        assertEquals(1, dbHandler.fetchByExtension(".pdf").size(),
                "Should find 1 .pdf event.");
        assertEquals(1, dbHandler.fetchByActivity("DELETED").size(),
                "Should find 1 DELETED event.");
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates a list of three diverse test FileEvents.
     *
     * @return a list containing 3 FileEvent objects with
     *         different extensions, paths, activities, and times
     */
    private List<FileEvent> createTestEvents() {
        final List<FileEvent> events = new ArrayList<>();
        events.add(new FileEvent(
                "document.txt", ".txt",
                "/Users/nasra/Documents/document.txt",
                "CREATED", SAMPLE_TIME));
        events.add(new FileEvent(
                "photo.jpg", ".jpg",
                "/Users/nasra/Pictures/photo.jpg",
                "CREATED",
                SAMPLE_TIME.plusMinutes(30)));
        events.add(new FileEvent(
                "report.pdf", ".pdf",
                "/Users/nasra/Downloads/report.pdf",
                "DELETED",
                SAMPLE_TIME.plusDays(1)));
        return events;
    }
}
