package filewatcher;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

/**
 * MainForm is the primary application window for the File System Watcher.
 * It lets the user choose a directory and extension filter, start and stop
 * monitoring, view captured file events in real time, and save those events
 * to the database. It also provides menu access to the query window and the
 * about dialog.
 *
 * <p>This class is the View layer of the application. All file-watching logic
 * is delegated to {@link FileWatcher}, all persistence to
 * {@link DatabaseHandler}, and all querying/exporting/emailing to
 * {@link QueryEngine}.</p>
 *
 * <p><b>Keyboard shortcuts (extra credit):</b> the main action buttons are
 * bound to cross-platform accelerators using the native menu-shortcut
 * modifier — Command on macOS, Control on Windows/Linux:</p>
 * <ul>
 *   <li>Cmd/Ctrl+S — Start monitoring</li>
 *   <li>Cmd/Ctrl+T — Stop monitoring</li>
 *   <li>Cmd/Ctrl+D — Write events to the database</li>
 * </ul>
 *
 * @author  Mariam Hussein &amp; Nasra Hussein
 * @version 1.1
 * @see     FileWatcher
 * @see     DatabaseHandler
 * @see     QueryEngine
 * @see     QueryForm
 */
public class MainForm extends JFrame {

    // ─────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────

    /**
     * The platform's native menu-shortcut modifier mask.
     * Resolves to the Command key on macOS and the Control key on
     * Windows/Linux, so accelerators always match user expectations.
     */
    private static final int MENU_MASK =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    /**
     * Human-readable name of the shortcut modifier for the current platform.
     * Used when building button labels (e.g. "Cmd+S" on macOS, "Ctrl+S"
     * on Windows/Linux).
     */
    private static final String MOD_LABEL =
            (MENU_MASK == InputEvent.META_DOWN_MASK) ? "Cmd" : "Ctrl";

    // ─────────────────────────────────────────────────────────────
    //  GUI COMPONENTS
    // ─────────────────────────────────────────────────────────────

    /** Text field where the user types the directory path to monitor. */
    private final JTextField txtDirectory;

    /** Editable dropdown for choosing (or typing) the extension filter. */
    private final JComboBox<String> cmbExtension;

    /** Starts monitoring the selected directory. */
    private final JButton btnStart;

    /** Stops the active monitoring session. */
    private final JButton btnStop;

    /** Saves the captured (pending) events to the database. */
    private final JButton btnWriteDB;

    /** Backing model for the on-screen list of captured file events. */
    private final DefaultListModel<String> listModel;

    // ─────────────────────────────────────────────────────────────
    //  BUSINESS LOGIC
    // ─────────────────────────────────────────────────────────────

    /** The active file watcher; recreated each time monitoring starts. */
    private FileWatcher fileWatcher;

    /** Handles all database persistence; created once at startup. */
    private final DatabaseHandler databaseHandler;

    /** Service layer used by the query window; created once at startup. */
    private final QueryEngine queryEngine;

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════

    /**
     * Builds the main window, wires up all components, listeners, and
     * keyboard shortcuts, initializes the database connection, and makes
     * the window visible.
     */
    public MainForm() {
        // step 1 - window title
        setTitle("File System Watcher");

        // step 2 - window size
        setSize(820, 500);

        // step 3 - initialize components
        txtDirectory = new JTextField(18);
        btnStart = new JButton("Start (" + MOD_LABEL + "+S)");
        btnStop = new JButton("Stop (" + MOD_LABEL + "+T)");
        btnWriteDB = new JButton("Write to DB (" + MOD_LABEL + "+D)");
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

        // Tooltips on the interactive controls
        txtDirectory.setToolTipText("Enter the full path of the folder to monitor");
        cmbExtension.setToolTipText("Select a file extension to watch, or 'All Files'");
        btnStart.setToolTipText("Begin monitoring the directory (" + MOD_LABEL + "+S)");
        btnStop.setToolTipText("Stop monitoring (" + MOD_LABEL + "+T)");
        btnWriteDB.setToolTipText("Save captured events to the database (" + MOD_LABEL + "+D)");

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

        // step 7b - cross-platform keyboard shortcuts (extra credit).
        // Registered on the root pane so they fire regardless of focus.
        bindShortcut(btnStart, KeyEvent.VK_S);    // Cmd/Ctrl+S - Start
        bindShortcut(btnStop, KeyEvent.VK_T);     // Cmd/Ctrl+T - sTop
        bindShortcut(btnWriteDB, KeyEvent.VK_D);  // Cmd/Ctrl+D - write to DB

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

    // ═════════════════════════════════════════════════════════════
    //  KEYBOARD SHORTCUT HELPER (EXTRA CREDIT)
    // ═════════════════════════════════════════════════════════════

    /**
     * Binds a keystroke to a button using the window's root pane, so the
     * shortcut fires regardless of which component currently holds keyboard
     * focus. Uses the native menu-shortcut modifier ({@link #MENU_MASK}):
     * Command on macOS, Control on Windows/Linux.
     *
     * <p>The triggered action calls {@link AbstractButton#doClick()} only
     * when the button is enabled, mirroring a real mouse click. This means
     * shortcuts for currently-disabled buttons (e.g. Stop before monitoring
     * has started) correctly do nothing.</p>
     *
     * @param button  the button to activate when the keystroke is pressed
     * @param keyCode the key to bind, e.g. {@link KeyEvent#VK_S}
     */
    private void bindShortcut(final JButton button, final int keyCode) {
        final KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, MENU_MASK);
        final String actionKey = "mainform_action_" + keyCode;

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(stroke, actionKey);
        getRootPane().getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (button.isEnabled()) {
                    button.doClick();
                }
            }
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  MENU BAR
    // ═════════════════════════════════════════════════════════════

    /**
     * Builds the application menu bar with File, File System Watcher,
     * Database, and Help menus.
     *
     * @return the fully assembled {@link JMenuBar}
     */
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

    // ═════════════════════════════════════════════════════════════
    //  MONITORING CONTROL
    // ═════════════════════════════════════════════════════════════

    /**
     * Starts monitoring the directory entered by the user.
     *
     * <p>Validates the directory field, translates the "All Files" choice
     * into a {@code null} (no-filter) value, creates a {@link FileWatcher},
     * and runs it on a background thread so the GUI stays responsive. A Swing
     * {@link Timer} polls for new events every 500&nbsp;ms and refreshes the
     * on-screen list.</p>
     */
    private void startMonitoring() {
        String directory = txtDirectory.getText().trim();
        String extension = (String) cmbExtension.getSelectedItem();

        if (directory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a directory path.");
            return;
        }

        // "All Files" means no extension filter — pass null so the watcher
        // captures every file event regardless of type.
        if (extension == null || extension.equals("All Files")) {
            extension = null;
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

    /**
     * Stops the active monitoring session and updates button states.
     * If there are unsaved events, the Write to DB button is re-enabled.
     */
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

    /**
     * Refreshes the on-screen event list with the watcher's current pending
     * events. Called every 500&nbsp;ms by the polling timer while monitoring
     * is active.
     */
    private void refreshEventList() {
        if (fileWatcher == null) return;
        List<FileEvent> events = fileWatcher.getPendingEvents();
        listModel.clear();
        for (FileEvent event : events) {
            listModel.addElement(event.toString());
        }
        btnWriteDB.setEnabled(fileWatcher.hasUnsavedEvents());
    }

    // ═════════════════════════════════════════════════════════════
    //  DATABASE
    // ═════════════════════════════════════════════════════════════

    /**
     * Writes the watcher's pending events to the database in a single batch,
     * then clears the pending list and disables the Write to DB button.
     */
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

    // ═════════════════════════════════════════════════════════════
    //  EXIT HANDLING
    // ═════════════════════════════════════════════════════════════

    /**
     * Handles application exit. If there are unsaved events, prompts the user
     * to save them first (Yes), discard and exit (No), or cancel and stay
     * (Cancel). Closes the database connection before disposing the window.
     */
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
