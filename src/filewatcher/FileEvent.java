package filewatcher;
import java.time.LocalDateTime;

    public class FileEvent {
        private final String fileName;
        private final String extension;
        private final String path;
        private final String activityType;
        private final LocalDateTime timeStamp;

        public FileEvent(String fileName, String extension, String path, String activityType, LocalDateTime timeStamp) {
            this.fileName = fileName;
            this.extension = extension;
            this.path = path;
            this.activityType = activityType;
            this.timeStamp = timeStamp;

        }
        public String getFileName() {
            return fileName;

        }
        public String getExtension() {
            return extension;
        }
        public String getPath() {
            return path;

        }
        public String getActivityType() {
            return activityType;
        }
        public LocalDateTime getTimeStamp() {
            return timeStamp;
        }
        public String getAsCsvRow(){
            return fileName + "," + extension + "," + path + "," + activityType + "," + timeStamp;
        }
        public String toString(){
            return "[" + activityType + "] " + fileName + " at " + timeStamp;
        }

    }


