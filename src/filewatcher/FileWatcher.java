package filewatcher;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
public class FileWatcher {


    // fields
    private final String  directoryPath;
    private final String extensionFilter;
    private final List<FileEvent> pendingEvents = new ArrayList<>();
    private boolean isActive = false;
    private WatchService watcher;


    //constructor
    public FileWatcher(String directoryPath, String extensionFilter) {
        this.directoryPath =  directoryPath;
        this.extensionFilter = extensionFilter;
    }


    // methods

    public List<FileEvent> getPendingEvents() {
        return pendingEvents;
    }
    public boolean hasUnsavedEvents() {
        return !pendingEvents.isEmpty();
    }
    public void stopMonitoring() throws IOException {
        isActive = false;
        if (watcher != null) {
            watcher.close();
        }
    }
    public void startMonitoring() throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        // step 2 - register the directory
        Path path = Paths.get(directoryPath);
        path.register(watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

// step 3 - set active
        isActive = true;
        // step 4 - keep watching while active
        while (isActive) {
            WatchKey key = watcher.poll();
            if (key == null) continue;

            // step 5 - process each event found
            for (WatchEvent<?> event : key.pollEvents()) {
                String fileName = event.context().toString();
                String activityType = event.kind().name();

                // only process if extension matches
                if (extensionFilter == null ||
                        fileName.endsWith(extensionFilter)) {
                    FileEvent fe = new FileEvent(
                            fileName,
                            extensionFilter,
                            directoryPath,
                            activityType
                    );
                    pendingEvents.add(fe);
                }
            }
            key.reset();
        }
    }

}
