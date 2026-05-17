package filewatcher;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileWatcherTest {

    /**
     * Tests that a new FileWatcher has no pending events.
     */
    @Test
    void getPendingEvents_newWatcher_returnsEmptyList() {
        FileWatcher fw = new FileWatcher("/some/path", ".txt");
        assertTrue(fw.getPendingEvents().isEmpty());
    }

    /**
     * Tests that a new FileWatcher has no unsaved events.
     */
    @Test
    void hasUnsavedEvents_newWatcher_returnsFalse() {
        FileWatcher fw = new FileWatcher("/some/path", ".txt");
        assertFalse(fw.hasUnsavedEvents());
    }

    /**
     * Tests that stopMonitoring does not throw an exception.
     */
    @Test
    void stopMonitoring_doesNotThrowException() {
        FileWatcher fw = new FileWatcher("/some/path", ".txt");
        assertDoesNotThrow(fw::stopMonitoring);
    }

    /**
     * Tests that startMonitoring detects a file creation event.
     */
    @Test
    void startMonitoring_detectsFileCreation(@TempDir Path tempDir) throws Exception {
        FileWatcher fw = new FileWatcher(tempDir.toString(), ".txt");

        Thread watchThread = new Thread(() -> {
            try {
                fw.startMonitoring();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        watchThread.start();

        Thread.sleep(1000);
        Files.createFile(tempDir.resolve("test.txt"));
        Thread.sleep(2000);

        fw.stopMonitoring();
        watchThread.join();

        assertFalse(fw.getPendingEvents().isEmpty());
    }


}