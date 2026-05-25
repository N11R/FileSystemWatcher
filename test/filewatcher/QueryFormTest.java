package filewatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JUnit 5 test class for {@link QueryForm}.
 *
 * <p>Because {@code QueryForm} is a Swing GUI window, most of its behavior
 * involves user interaction (clicking buttons, typing in fields) that cannot
 * be verified through standard JUnit assertions without a UI testing
 * framework like AssertJ Swing or FEST.  These tests therefore focus on
 * what <em>can</em> be validated at the unit level:</p>
 * <ul>
 *   <li>Constructor input validation (null rejection).</li>
 *   <li>Successful construction when valid dependencies are provided.</li>
 * </ul>
 *
 * <p>The tests run in a headless-safe way: each test that creates a visible
 * window disposes it immediately after the assertion to avoid leaving
 * orphan frames on the screen or in CI environments.</p>
 *
 * @author  File System Watcher Team
 * @version 1.0
 * @see     QueryForm
 * @see     QueryEngine
 */
class QueryFormTest {

    /** The DatabaseHandler used to build a real QueryEngine for testing. */
    private DatabaseHandler dbHandler;

    /** A fully constructed QueryEngine passed into QueryForm. */
    private QueryEngine queryEngine;

    /** Holds a reference to the QueryForm so it can be disposed in tearDown. */
    private QueryForm queryForm;

    /** A fixed timestamp reused across test data. */
    private static final LocalDateTime SAMPLE_TIME =
            LocalDateTime.of(2026, 5, 7, 18, 30, 0);

    /**
     * Sets up the test environment before each test.
     * Creates a DatabaseHandler and QueryEngine with test data so
     * that QueryForm has a working back-end to talk to.
     */
    @BeforeEach
    void setUp() {
        dbHandler = new DatabaseHandler();
        dbHandler.openConnection();
        dbHandler.createTableIfNotExists();

        // Insert a handful of events so queries in the form have something to find
        dbHandler.saveEvent(new FileEvent(
                "readme.txt", ".txt",
                "/home/user/Documents/readme.txt",
                "CREATED", SAMPLE_TIME));
        dbHandler.saveEvent(new FileEvent(
                "report.pdf", ".pdf",
                "/home/user/Downloads/report.pdf",
                "MODIFIED", SAMPLE_TIME.plusHours(2)));

        ReportGenerator rg = new ReportGenerator();
        queryEngine = new QueryEngine(dbHandler, rg, null);
    }

    /**
     * Tears down the test environment after each test.
     * Disposes the QueryForm window (if one was created) and closes
     * the database connection.  Deletes the temp database file to
     * keep the workspace clean.
     */
    @AfterEach
    void tearDown() {
        // Dispose the form first so Swing releases its resources
        if (queryForm != null) {
            queryForm.dispose();
            queryForm = null;
        }

        dbHandler.closeConnection();

        // Remove the SQLite file that was created during the test
        final File dbFile = new File("filewatcher.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTOR VALIDATION TESTS
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies that passing {@code null} as the QueryEngine throws
     * an {@link IllegalArgumentException}.
     *
     * <p>This mirrors the defensive-programming pattern used in every
     * other class in the project (see {@code QueryEngine},
     * {@code FileWatcher}, etc.).</p>
     */
    @Test
    void testConstructorNullEngineThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                        new QueryForm(null),
                "Should throw when QueryEngine is null.");
    }

    /**
     * Verifies that construction succeeds when a valid QueryEngine
     * is provided.  The form should be non-null and visible after
     * the constructor returns.
     */
    @Test
    void testConstructorWithValidEngineSucceeds() {
        // Creating a QueryForm will pop open a window — we dispose it
        // immediately in tearDown, but we still verify it was built.
        queryForm = assertDoesNotThrow(() -> new QueryForm(queryEngine),
                "Constructor should not throw with a valid QueryEngine.");
        assertNotNull(queryForm,
                "QueryForm instance should not be null after construction.");
    }

    /**
     * Verifies that the QueryForm window title is set correctly
     * after construction.
     */
    @Test
    void testWindowTitleIsSet() {
        queryForm = new QueryForm(queryEngine);
        assertNotNull(queryForm.getTitle(),
                "Window title should not be null.");
        assert queryForm.getTitle().contains("Query")
                : "Window title should contain 'Query'.";
    }

    /**
     * Verifies that the QueryForm is visible after construction.
     * The SRS specifies that the form opens immediately when the user
     * clicks the Query button.
     */
    @Test
    void testFormIsVisibleAfterConstruction() {
        queryForm = new QueryForm(queryEngine);
        assert queryForm.isVisible()
                : "QueryForm should be visible after construction.";
    }

    /**
     * Verifies that calling dispose on the QueryForm does not throw.
     * This simulates the user clicking "Return to Main" or closing
     * the window.
     */
    @Test
    void testDisposeDoesNotThrow() {
        queryForm = new QueryForm(queryEngine);
        assertDoesNotThrow(() -> queryForm.dispose(),
                "Disposing the QueryForm should not throw.");
    }
}
