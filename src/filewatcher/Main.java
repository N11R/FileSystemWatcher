package filewatcher;

public class Main {
    public static void main(String[] args) {
        try {
            // load the SQLite driver before the app starts
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite driver not found: " + e.getMessage());
        }
        new MainForm();
    }
}