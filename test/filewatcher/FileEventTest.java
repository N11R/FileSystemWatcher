package filewatcher;

import filewatcher.FileEvent;
import filewatcher.FileWatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@Nested
class FileEventTest {


    @Test
    public void testGetFileName() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx", "docx", "C:/docs/report.docx", "Created", time );

        assertEquals("report.docx", event.getFileName());
    }

    @Test
    public void testGetExtension() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx", "docx", "C:/docs/report.docx", "Created", time );
        assertEquals("docx", event.getExtension());
    }

    @Test
    public void testGetFilePath() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx",  "docx", "C:/docs/report.docx", "Created", time );
        assertEquals("C:/docs/report.docx", event.getPath());
    }


    @Test
    public void testGetActivityType() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx", "docx", "C:/docs/report.docx", "Created", time );
        assertEquals("Created", event.getActivityType());
    }
    @Test
    public void testGetTimestamp() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx",  "docx", "C:/docs/report.docx", "Created", time );
        assertEquals(time, event.getTimeStamp());
    }
    @Test
    public void testGetAsCsvRow() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx", "docx", "C:/docs/report.docx", "Created", time );
        assertEquals("report.docx,docx,C:/docs/report.docx,Created,2026-04-28T14:30", event.getAsCsvRow());
    }
    @Test
    public void testToString() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 28, 14, 30, 0);
        FileEvent event = new FileEvent("report.docx", "docx", "C:/docs/report.docx", "Created", time );
        assertEquals("[Created} report.docx at 2026-04-28T14:30", event.toString());
    }


}

