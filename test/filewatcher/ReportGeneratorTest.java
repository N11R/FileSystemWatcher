package filewatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportGeneratorTest {

    // buildHeader() tests


    @Test
    void buildHeader_containsQueryType() {
        ReportGenerator rg = new ReportGenerator();
        String header = rg.buildHeader("ByDate", "2024-01-01");
        assertTrue(header.contains("ByDate"),
                "Header should contain the query type");
    }

    @Test
    void buildHeader_containsQueryParam() {
        ReportGenerator rg = new ReportGenerator();
        String header = rg.buildHeader("ByDate", "2024-01-01");
        assertTrue(header.contains("2024-01-01"),
                "Header should contain the query parameter");
    }

    @Test
    void buildHeader_containsExportDate() {
        ReportGenerator rg = new ReportGenerator();
        String header = rg.buildHeader("ByExtension", ".txt");
        assertTrue(header.contains("Export Date:"),
                "Header should contain an Export Date label");
    }

    @Test
    void buildHeader_emptyParams_returnsStringWithLabels() {
        ReportGenerator rg = new ReportGenerator();
        String header = rg.buildHeader("", "");
        assertTrue(header.contains("Query Type:"),
                "Header should still contain Query Type label even with empty params");
    }


    // formatToTable() tests

    @Test
    void formatToTable_emptyList_returnsEmptyString() {
        ReportGenerator rg = new ReportGenerator();
        String result = rg.formatToTable(new ArrayList<>());
        assertEquals("", result,
                "formatToTable with empty list should return empty string");
    }

    @Test
    void formatToTable_singleEvent_returnsOneLine() {
        ReportGenerator rg = new ReportGenerator();
        FileEvent event = new FileEvent("test.txt", ".txt", "/home/user",
                "ENTRY_CREATE", LocalDateTime.now());
        List<FileEvent> events = List.of(event);

        String result = rg.formatToTable(events);

        // should have exactly one newline (one row)
        assertEquals(1, result.lines().count(),
                "One event should produce one line");
    }

    @Test
    void formatToTable_multipleEvents_returnsMultipleLines() {
        ReportGenerator rg = new ReportGenerator();
        List<FileEvent> events = new ArrayList<>();
        events.add(new FileEvent("a.txt", ".txt", "/home", "ENTRY_CREATE", LocalDateTime.now()));
        events.add(new FileEvent("b.txt", ".txt", "/home", "ENTRY_MODIFY", LocalDateTime.now()));
        events.add(new FileEvent("c.txt", ".txt", "/home", "ENTRY_DELETE", LocalDateTime.now()));

        String result = rg.formatToTable(events);

        assertEquals(3, result.lines().count(),
                "Three events should produce three lines");
    }

    // -------------------------------------------------------
    // exportToCsv() tests
    // -------------------------------------------------------

    @Test
    void exportToCsv_createsFile(@TempDir Path tempDir) throws IOException {
        ReportGenerator rg = new ReportGenerator();
        String filePath = tempDir.resolve("output.csv").toString();
        List<FileEvent> events = new ArrayList<>();

        rg.exportToCsv(events, filePath);

        assertTrue(tempDir.resolve("output.csv").toFile().exists(),
                "exportToCsv should create a file at the given path");
    }

    @Test
    void exportToCsv_fileContainsCsvHeader(@TempDir Path tempDir) throws IOException {
        ReportGenerator rg = new ReportGenerator();
        String filePath = tempDir.resolve("output.csv").toString();

        rg.exportToCsv(new ArrayList<>(), filePath);

        String firstLine = new BufferedReader(new FileReader(filePath)).readLine();
        assertEquals("File Name,Extension,Path,Activity,Date/Time", firstLine,
                "First line of CSV should be the header row");
    }

    @Test
    void exportToCsv_withEvents_writesDataRows(@TempDir Path tempDir) throws IOException {
        ReportGenerator rg = new ReportGenerator();
        String filePath = tempDir.resolve("output.csv").toString();
        List<FileEvent> events = List.of(
                new FileEvent("notes.txt", ".txt", "/docs", "ENTRY_CREATE", LocalDateTime.now())
        );

        rg.exportToCsv(events, filePath);

        List<String> lines = new BufferedReader(new FileReader(filePath)).lines().toList();
        assertEquals(2, lines.size(),
                "CSV should have 1 header line + 1 data row");
    }

    @Test
    void exportToCsv_emptyList_onlyWritesHeader(@TempDir Path tempDir) throws IOException {
        ReportGenerator rg = new ReportGenerator();
        String filePath = tempDir.resolve("output.csv").toString();

        rg.exportToCsv(new ArrayList<>(), filePath);

        List<String> lines = new BufferedReader(new FileReader(filePath)).lines().toList();
        assertEquals(1, lines.size(),
                "CSV with no events should only have the header line");
    }
}
