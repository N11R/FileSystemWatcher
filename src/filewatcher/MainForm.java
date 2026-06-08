package filewatcher;


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

public class MainForm extends JFrame {

    // GUI components
    private final JTextField txtDirectory;
    private final JComboBox<String> cmbExtension;
    private final JButton btnStart;
    private final JButton btnStop;
    private final JButton btnWriteDB;
    private final DefaultListModel<String> listModel;

    // Logic
    private FileWatcher fileWatcher;
    private final DatabaseHandler databaseHandler;
    private final QueryEngine queryEngine;

    // constructor
    public MainForm() {
        // step 1 - window title
        setTitle("File System Watcher");

        // step 2 - window size
        setSize(700, 500);

        // step 3 - initialize components
        txtDirectory = new JTextField(30);
        btnStart = new JButton("Start");
        btnStop = new JButton("Stop");
        btnWriteDB = new JButton("Write to DB");
        listModel = new DefaultListModel<>();
        JList<String> lstEvents = new JList<>(listModel);
        cmbExtension = new JComboBox<>();
        cmbExtension.setEditable(true);
        cmbExtension.addItem("All Files");
        cmbExtension.addItem(".txt");
        cmbExtension.addItem(".java");
        cmbExtension.addItem(".pdf");
        cmbExtension.addItem(".png");
        cmbExtension.addItem(".docx");

        // step 4 - set initial button states
        btnStop.setEnabled(false);
        btnWriteDB.setEnabled(false);

        // step 5 - add button listeners
        btnStart.addActionListener(e -> startMonitoring());
        btnStop.addActionListener(e -> stopMonitoring());
        btnWriteDB.addActionListener(e -> writeToDatabase());

        // step 6 - build menu bar
        setJMenuBar(buildMenuBar());

        // step 7 - set layout and add components
        setLayout(new BorderLayout(10, 10));

        // top panel: extension dropdown, directory field, start/stop buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topPanel.add(new JLabel("Extension:"));
        topPanel.add(cmbExtension);
        topPanel.add(new JLabel("Directory:"));
        topPanel.add(txtDirectory);
        topPanel.add(btnStart);
        topPanel.add(btnStop);

        // center panel: event list
        JScrollPane scrollPane = new JScrollPane(lstEvents);
        scrollPane.setBorder(BorderFactory.createTitledBorder("File Events"));

        // bottom panel: write to DB button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottomPanel.add(btnWriteDB);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // step 8 - handle unsaved changes on exit
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // step 9 - initialize database handler once at startup
        databaseHandler = new DatabaseHandler();
        if (!databaseHandler.openConnection()) {
            JOptionPane.showMessageDialog(null, "Failed to connect to database.");
        }
        databaseHandler.createTableIfNotExists();
        queryEngine = new QueryEngine(databaseHandler, new ReportGenerator(), new EmailService());

        // step 10 - show window
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // builds the menu bar
    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> handleExit());
        fileMenu.add(exitItem);

        // File System Watcher menu
        JMenu fswMenu = new JMenu("File System Watcher");
        JMenuItem startItem = new JMenuItem("Start");
        JMenuItem stopItem = new JMenuItem("Stop");
        startItem.addActionListener(e -> startMonitoring());
        stopItem.addActionListener(e -> stopMonitoring());
        fswMenu.add(startItem);
        fswMenu.add(stopItem);

        // Database menu
        JMenu dbMenu = new JMenu("Database");
        JMenuItem writeItem = new JMenuItem("Write to Database");
        JMenuItem queryItem = new JMenuItem("Query");
        writeItem.addActionListener(e -> writeToDatabase());
        queryItem.addActionListener(e -> new QueryForm(queryEngine));
        dbMenu.add(writeItem);
        dbMenu.add(queryItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> new AboutDialog(this));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(fswMenu);
        menuBar.add(dbMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    // starts monitoring the selected directory
    private void startMonitoring() {
        String directory = txtDirectory.getText().trim();
        String extension = (String) cmbExtension.getSelectedItem();

        if (directory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a directory path.");
            return;
        }

        fileWatcher = new FileWatcher(directory, extension);

        // run monitoring on a background thread so GUI stays responsive
        Thread watchThread = new Thread(() -> {
            try {
                fileWatcher.startMonitoring();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // poll for new events and display them in the list
        Timer eventPoller = new Timer(500, e -> refreshEventList());
        eventPoller.start();

        watchThread.start();

        // update button states
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnWriteDB.setEnabled(false);

        JOptionPane.showMessageDialog(this, "Monitoring started for: " + directory);
    }

    // stops monitoring
    private void stopMonitoring() {
        if (fileWatcher != null) {
            try {
                fileWatcher.stopMonitoring();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error stopping watcher: " + ex.getMessage());
            }
        }

        // update button states
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnWriteDB.setEnabled(fileWatcher != null && fileWatcher.hasUnsavedEvents());

        JOptionPane.showMessageDialog(this, "Monitoring stopped.");
    }

    // refreshes the event list box with latest pending events
    private void refreshEventList() {
        if (fileWatcher == null) return;
        List<FileEvent> events = fileWatcher.getPendingEvents();
        listModel.clear();
        for (FileEvent event : events) {
            listModel.addElement(event.toString());
        }
        btnWriteDB.setEnabled(fileWatcher.hasUnsavedEvents());
    }

    // writes pending events to the database
    private void writeToDatabase() {
        if (fileWatcher == null || !fileWatcher.hasUnsavedEvents()) {
            JOptionPane.showMessageDialog(this, "No pending events to save.");
            return;
        }

        int saved = databaseHandler.saveEvents(fileWatcher.getPendingEvents());
        fileWatcher.clearPendingEvents();
        btnWriteDB.setEnabled(false);
        JOptionPane.showMessageDialog(this, saved + " event(s) saved to database.");
    }

    // handles exit with unsaved changes check
    private void handleExit() {
        if (fileWatcher != null && fileWatcher.hasUnsavedEvents()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved events. Write to database before exiting?",
                    "Unsaved Events",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                writeToDatabase();
                databaseHandler.closeConnection();
                dispose();
            } else if (choice == JOptionPane.NO_OPTION) {
                databaseHandler.closeConnection();
                dispose();
            }
            // CANCEL_OPTION: do nothing, return to app

        } else {
            databaseHandler.closeConnection();
            dispose();
        }
    }
}