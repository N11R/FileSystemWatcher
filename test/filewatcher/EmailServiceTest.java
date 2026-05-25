package filewatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for {@link EmailService}.
 *
 * <p>Tests cover credential loading, and sendEmail behavior
 * for invalid inputs. Actual email sending is not tested here
 * as it requires live Gmail credentials and network access,
 * which is outside the scope of unit testing.</p>
 *
 * @author Mariam Hussein
 * @version 1.0
 */
public class EmailServiceTest {

    /** The EmailService instance under test. */
    private EmailService emailService;

    /** A temporary CSV file used as a test attachment. */
    private File tempCsvFile;

    /**
     * Sets up the test environment before each test.
     * Creates a fresh EmailService and a temporary CSV file.
     */
    @BeforeEach
    void setUp() throws IOException {
        emailService = new EmailService();

        // create a temporary CSV file to use as attachment in tests
        tempCsvFile = File.createTempFile("test_report", ".csv");
        FileWriter fw = new FileWriter(tempCsvFile);
        fw.write("File Name,Extension,Path,Activity,Date/Time\n");
        fw.write("test.txt,.txt,/home/test.txt,CREATED,2026-05-21\n");
        fw.close();
    }

    /**
     * Tears down the test environment after each test.
     * Deletes the temporary CSV file if it exists.
     */
    @AfterEach
    void tearDown() {
        if (tempCsvFile != null && tempCsvFile.exists()) {
            tempCsvFile.delete();
        }
    }

    // CONSTRUCTOR TESTS

    /**
     * Tests that EmailService can be instantiated without throwing.
     * Even if config.properties is missing it should not crash.
     */
    @Test
    void testConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> new EmailService(),
                "EmailService constructor should not throw even if config is missing.");
    }

    /**
     * Tests that a second EmailService instance can be created.
     * Verifies the constructor is reusable.
     */
    @Test
    void testConstructorCreatesNewInstance() {
        EmailService es2 = new EmailService();
        assertNotNull(es2,
                "A second EmailService instance should be created successfully.");
    }

    //
    // sendEmail() INPUT VALIDATION TESTS
    //

    /**
     * Tests that sendEmail returns false when recipient is null.
     * No email should be sent for null input.
     */
    @Test
    void testSendEmail_nullRecipient_returnsFalse() {
        boolean result = emailService.sendEmail(null, tempCsvFile);
        assertFalse(result,
                "sendEmail should return false for null recipient.");
    }

    /**
     * Tests that sendEmail returns false when recipient is empty string.
     */
    @Test
    void testSendEmail_emptyRecipient_returnsFalse() {
        boolean result = emailService.sendEmail("", tempCsvFile);
        assertFalse(result,
                "sendEmail should return false for empty recipient.");
    }

    /**
     * Tests that sendEmail returns false when recipient is blank (spaces only).
     */
    @Test
    void testSendEmail_blankRecipient_returnsFalse() {
        boolean result = emailService.sendEmail("   ", tempCsvFile);
        assertFalse(result,
                "sendEmail should return false for blank recipient.");
    }

    /**
     * Tests that sendEmail returns false when attachment file is null.
     */
    @Test
    void testSendEmail_nullAttachment_returnsFalse() {
        boolean result = emailService.sendEmail("test@example.com", null);
        assertFalse(result,
                "sendEmail should return false for null attachment.");
    }

    /**
     * Tests that sendEmail returns false when attachment file does not exist.
     */
    @Test
    void testSendEmail_nonExistentFile_returnsFalse() {
        File fakeFile = new File("/nonexistent/path/fake.csv");
        boolean result = emailService.sendEmail("test@example.com", fakeFile);
        assertFalse(result,
                "sendEmail should return false for non-existent file.");
    }

    /**
     * Tests that sendEmail returns false when both recipient and
     * attachment are null.
     */
    @Test
    void testSendEmail_nullRecipientAndNullFile_returnsFalse() {
        boolean result = emailService.sendEmail(null, null);
        assertFalse(result,
                "sendEmail should return false when both inputs are null.");
    }

    /**
     * Tests that sendEmail returns false when credentials are not
     * configured (no config.properties file present).
     * Since no real Gmail credentials exist in the test environment,
     * the send attempt should fail gracefully and return false.
     */
    @Test
    void testSendEmail_noCredentials_returnsFalse() {
        boolean result = emailService.sendEmail("test@example.com", tempCsvFile);
        assertFalse(result,
                "sendEmail should return false when no credentials are configured.");
    }

    /**
     * Tests that sendEmail does not throw an exception even when
     * credentials are missing and a send is attempted.
     */
    @Test
    void testSendEmail_noCredentials_doesNotThrow() {
        assertDoesNotThrow(() ->
                        emailService.sendEmail("test@example.com", tempCsvFile),
                "sendEmail should never throw — errors should be caught internally.");
    }

    // 
    // ATTACHMENT FILE TESTS
    // 

    /**
     * Tests that the temporary CSV file used in tests actually exists.
     * Verifies the test setup is correct.
     */
    @Test
    void testTempCsvFileExists() {
        assertTrue(tempCsvFile.exists(),
                "Temporary CSV file should exist for attachment tests.");
    }

    /**
     * Tests that sendEmail handles an empty CSV file without throwing.
     */
    @Test
    void testSendEmail_emptyCsvFile_doesNotThrow(@TempDir Path tempDir)
            throws IOException {
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        emptyFile.createNewFile();

        assertDoesNotThrow(() ->
                        emailService.sendEmail("test@example.com", emptyFile),
                "sendEmail should not throw for an empty CSV file.");
    }
}
