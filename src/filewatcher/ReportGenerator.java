package filewatcher;

import java.time.LocalDateTime;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class ReportGenerator {

    //methods
    public String formatToTable(List<FileEvent> events) {
        String result = "";
        for (FileEvent event : events) {
            result += event.getAsCsvRow() + "\n";
        }
        return result;

    }

    public void exportToCsv(List<FileEvent> events, String fileName) throws IOException {
    // step 1 - create header
        String header = "File Name,Extension,Path,Activity,Date/Time\n";

    // step 2 and 3 - write to file
        FileWriter fw = new FileWriter(fileName);
        fw.write(header);
        fw.write(formatToTable(events));
        fw.close();
    }

    public String buildHeader(String queryType, String queryParam) {
        return "Query Type: " + queryType + "\n" +
                "Query Parameter: " + queryParam + "\n" +
                "Export Date: " + LocalDateTime.now().toString() + "\n";
    }
}
