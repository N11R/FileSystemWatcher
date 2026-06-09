package filewatcher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * QueryForm is the database query window for the File System Watcher application.
 * It provides a graphical interface that lets the user search stored file events
 * by extension, date range, activity type, or file path, and then optionally
 * export the results to CSV or email them as an attachment.
 *
 * <p>The form is opened from {@link MainForm} when the user clicks the
 * "Query" toolbar button or selects Database &gt; Query from the menu bar.
 * It delegates all query logic, CSV generation, and email delivery to a
 * {@link QueryEngine} instance, keeping this class focused purely on
 * presentation and user interaction.</p>
 *
 * <h3>Layout</h3>
 * The window is divided into three vertical sections:
 * <ol>
 *   <li><b>Query Controls (top)</b> — four tabbed panels for each query type,
 *       each with its own input fields and a "Run Query" button.</li>
 *   <li><b>Results Table (center)</b> — a scrollable {@link JTable} that
 *       displays matching {@link FileEvent} records with columns for
 *       File Name, Extension, Path, Activity, and Date/Time.</li>
 *   <li><b>Action Bar (bottom)</b> — buttons for Export to CSV, Email Results,
 *       Clear Database, and Return to Main, plus a status label showing
 *       the result count.</li>
 * </ol>
 *
 * <p><b>Extra Credit Features:</b></p>
 * <ul>
 *   <li>Cross-platform keyboard shortcuts on the main action buttons, using
 *       the native menu-shortcut modifier (Command on macOS, Control on
 *       Windows/Linux) so the accelerators fire reliably on every platform.</li>
 *   <li>Tooltips on every interactive control, each showing the correct
 *       platform-specific shortcut.</li>
 *   <li>Clickable column headers for sorting results.</li>
 *   <li>Status bar showing the result count instead of a popup for empty
 *       results.</li>
 *   <li>Full Javadoc with SRS traceability on all members.</li>
 * </ul>
 *
 * <p><b>SRS Coverage:</b></p>
 * <ul>
 *   <li>FR-3.1 — Query by extension</li>
 *   <li>FR-3.2 — Query by date range</li>
 *   <li>FR-3.3 — Query by activity type</li>
 *   <li>FR-3.4 — Query by path</li>
 *   <li>FR-3.8 — Clear database</li>
 *   <li>FR-4.x — Export to CSV</li>
 *   <li>FR-5.x — Email CSV report</li>
 *   <li>Section 4.1.2 — QueryForm UI specification</li>
 * </ul>
 *
 * @author  Nasra Hussein
 * @version 1.1
 * @see     QueryEngine
 * @see     MainForm
 * @see     FileEvent
 */
public class QueryForm extends JFrame {

    // ─────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────

    /** Column headers shown in the results table, matching the SRS specification. */
    private static final String[] TABLE_COLUMNS = {
            "File Name", "Extension", "Path", "Activity", "Date/Time"
    };

    /** Pre-populated file extensions offered in the extension combo box. */
    private static final String[] DEFAULT_EXTENSIONS = {
            ".txt", ".java", ".pdf", ".png", ".docx"
    };

    /** Activity types available for the activity query combo box. */
    private static final String[] ACTIVITY_TYPES = {
            "CREATED", "MODIFIED", "DELETED", "RENAMED"
    };

    /** Standard date-time display format used when rendering timestamps in the table. */
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * The platform's native menu-shortcut modifier mask.
     * Resolves to the Command key on macOS and the Control key on
     * Windows/Linux, so accelerators always match user expectations.
     */
    private static final int MENU_MASK =
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    /**
     * Human-readable name of the shortcut modifier for the current platform.
     * Used when building tooltip text (e.g. "Cmd+E" on macOS, "Ctrl+E" on Windows).
     */
    private static final String MOD_LABEL =
            (MENU_MASK == InputEvent.META_DOWN_MASK) ? "Cmd" : "Ctrl";

    // ─────────────────────────────────────────────────────────────
    //  GUI COMPONENTS — Query Inputs
    // ─────────────────────────────────────────────────────────────

    /** Dropdown listing common extensions; the user can also type a custom one. */
    private final JComboBox<String> cmbExtension;

    /** Text field where the user enters a partial or full directory path to search. */
    private final JTextField txtPath;

    /** Spinner for choosing the start date of a date-range query. */
    private final JSpinner spnStartDate;

    /** Spinner for choosing the end date of a date-range query. */
    private final JSpinner spnEndDate;

    /** Dropdown listing the four supported activity types (created/modified/deleted/renamed). */
    private final JComboBox<String> cmbActivity;

    // ─────────────────────────────────────────────────────────────
    //  GUI COMPONENTS — Results & Actions
    // ─────────────────────────────────────────────────────────────

    /** The table model backing the results grid; cleared and repopulated after each query. */
    private final DefaultTableModel tableModel;

    /** Export button — enabled only after a query returns at least one result. */
    private final JButton btnExportCsv;

    /** Email button — enabled only after a CSV file has been successfully generated. */
    private final JButton btnEmailResults;

    /** Status label at the bottom showing the number of results found. */
    private final JLabel lblStatus;

    // ─────────────────────────────────────────────────────────────
    //  BUSINESS LOGIC
    // ─────────────────────────────────────────────────────────────

    /** Orchestrates queries, CSV export, and email; injected via the constructor. */
    private final QueryEngine queryEngine;

    /**
     * Holds the most recently exported CSV file so the email button can attach it.
     * Set to {@code null} until the user successfully exports results.
     */
    private File lastExportedCsv;

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═════════════════════════════════════════════════════════════

    /**
     * Creates and displays the QueryForm window.
     *
     * <p>The form takes ownership of the provided {@link QueryEngine} for
     * the duration of its lifecycle.  It builds the full Swing UI, wires
     * up all event listeners, and makes itself visible on screen.</p>
     *
     * @param engine the {@link QueryEngine} that this form will use for
     *               querying, exporting, and emailing results.
     *               Must not be {@code null}.
     * @throws IllegalArgumentException if {@code engine} is {@code null}
     */
    public QueryForm(final QueryEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("QueryEngine cannot be null.");
        }
        this.queryEngine = engine;
        this.lastExportedCsv = null;

        // --- Window setup ---
        setTitle("File System Watcher \u2014 Query Database");
        setSize(850, 600);
        setMinimumSize(new Dimension(700, 450));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Initialize all input components ---
        cmbExtension = createExtensionComboBox();
        txtPath = new JTextField(20);
        txtPath.setToolTipText("Enter a folder name or partial path to search for");
        spnStartDate = createDateSpinner();
        spnStartDate.setToolTipText("Pick the start date/time for the search range");
        spnEndDate = createDateSpinner();
        spnEndDate.setToolTipText("Pick the end date/time for the search range");
        cmbActivity = new JComboBox<>(ACTIVITY_TYPES);
        cmbActivity.setToolTipText("Choose the type of file event to search for");

        // --- Build the results table with a non-editable model ---
        tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                // Prevent accidental in-table editing; this is a read-only view.
                return false;
            }
        };
        JTable resultsTable = new JTable(tableModel);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultsTable.getTableHeader().setReorderingAllowed(false);

        // Extra credit: allow users to click column headers to sort results
        resultsTable.setAutoCreateRowSorter(true);

        // --- Action buttons along the bottom with icons and tooltips ---
        // Export and Email get cross-platform accelerators (see bindShortcut).
        // Clear Database is intentionally left click-only: it is destructive,
        // and Cmd+C / Ctrl+C would collide with the universal "copy" shortcut.
        btnExportCsv = new JButton("\uD83D\uDCC4 Export to CSV");
        btnExportCsv.setToolTipText(
                "Save the current query results to a CSV file (" + MOD_LABEL + "+E)");
        btnExportCsv.setMnemonic(KeyEvent.VK_E);

        btnEmailResults = new JButton("\u2709 Email Results");
        btnEmailResults.setToolTipText(
                "Email the exported CSV file to a recipient (" + MOD_LABEL + "+M)");
        btnEmailResults.setMnemonic(KeyEvent.VK_M);

        JButton btnClearDb = new JButton("\uD83D\uDDD1 Clear Database");
        btnClearDb.setToolTipText("Permanently delete all stored file events");
        btnClearDb.setMnemonic(KeyEvent.VK_C);

        JButton btnReturn = new JButton("\u21A9 Return to Main");
        btnReturn.setToolTipText(
                "Close this window and go back to the main form (" + MOD_LABEL + "+B)");
        btnReturn.setMnemonic(KeyEvent.VK_B);

        // Disable export and email until the user actually runs a query
        btnExportCsv.setEnabled(false);
        btnEmailResults.setEnabled(false);

        // --- Status label to show result count ---
        lblStatus = new JLabel("Ready \u2014 select a tab and run a query.");
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // --- Assemble the three main sections ---
        add(buildQueryPanel(), BorderLayout.NORTH);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        add(buildBottomPanel(btnExportCsv, btnEmailResults, btnClearDb, btnReturn),
                BorderLayout.SOUTH);

        // --- Wire up action-bar listeners ---
        btnExportCsv.addActionListener(e -> onExportCsvClicked());
        btnEmailResults.addActionListener(e -> onEmailResultsClicked());
        btnClearDb.addActionListener(e -> onClearDatabaseClicked());
        btnReturn.addActionListener(e -> dispose());

        // --- Cross-platform keyboard accelerators (extra credit) ---
        // Registered as window-level key bindings so they fire no matter
        // which control currently has focus. The Swing mnemonics set above
        // remain for the underlined-letter hints on Windows/Linux.
        bindShortcut(btnExportCsv, KeyEvent.VK_E);
        bindShortcut(btnEmailResults, KeyEvent.VK_M);
        bindShortcut(btnReturn, KeyEvent.VK_B);

        // Closing the window should not terminate the whole application,
        // but we want to clean up nicely.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                onReturnToMainClicked();
            }
        });

        // Center the form on screen and show it
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════
    //  KEYBOARD SHORTCUT HELPER (EXTRA CREDIT)
    // ═════════════════════════════════════════════════════════════

    /**
     * Binds a keystroke to a button using the window's root pane, so the
     * shortcut fires regardless of which component holds focus. Uses the
     * native menu-shortcut modifier (Cmd on macOS, Ctrl on Windows/Linux).
     *
     * @param button  the button to activate when the keystroke is pressed
     * @param keyCode the key to bind, e.g. KeyEvent.VK_S
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
    //  PANEL BUILDERS — keep the constructor readable
    // ═════════════════════════════════════════════════════════════

    /**
     * Builds the top section of the form: a {@link JTabbedPane} with one tab
     * per query type (Extension, Date Range, Activity, Path).
     *
     * <p>Each tab contains the relevant input controls and a "Run Query"
     * button.  The Extension tab's Run button also gets a cross-platform
     * accelerator (Cmd/Ctrl+R).  This tabbed layout prevents the form from
     * feeling cluttered while still exposing every query type on a single
     * screen.</p>
     *
     * @return a fully wired panel ready to be added to the form
     */
    private JTabbedPane buildQueryPanel() {
        JTabbedPane tabs = new JTabbedPane();

        // --- Tab 1: Query by Extension ---
        JPanel extPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        extPanel.add(new JLabel("Extension:"));
        extPanel.add(cmbExtension);
        JButton btnQueryExt = new JButton("\u25B6 Run Query");
        btnQueryExt.setMnemonic(KeyEvent.VK_R);
        btnQueryExt.setToolTipText(
                "Search for all events with this extension (" + MOD_LABEL + "+R)");
        btnQueryExt.addActionListener(e -> onExtensionQueryClicked());
        bindShortcut(btnQueryExt, KeyEvent.VK_R);
        extPanel.add(btnQueryExt);
        tabs.addTab("By Extension", extPanel);

        // --- Tab 2: Query by Date Range ---
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        datePanel.add(new JLabel("Start:"));
        datePanel.add(spnStartDate);
        datePanel.add(new JLabel("End:"));
        datePanel.add(spnEndDate);
        JButton btnQueryDate = new JButton("\u25B6 Run Query");
        btnQueryDate.setToolTipText("Search for events between the two dates");
        btnQueryDate.addActionListener(e -> onDateRangeQueryClicked());
        datePanel.add(btnQueryDate);
        tabs.addTab("By Date Range", datePanel);

        // --- Tab 3: Query by Activity ---
        JPanel actPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actPanel.add(new JLabel("Activity:"));
        actPanel.add(cmbActivity);
        JButton btnQueryAct = new JButton("\u25B6 Run Query");
        btnQueryAct.setToolTipText("Search for events with this activity type");
        btnQueryAct.addActionListener(e -> onActivityQueryClicked());
        actPanel.add(btnQueryAct);
        tabs.addTab("By Activity", actPanel);

        // --- Tab 4: Query by Path ---
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pathPanel.add(new JLabel("Path contains:"));
        pathPanel.add(txtPath);
        JButton btnQueryPath = new JButton("\u25B6 Run Query");
        btnQueryPath.setToolTipText("Search for events whose file path contains this text");
        btnQueryPath.addActionListener(e -> onPathQueryClicked());
        pathPanel.add(btnQueryPath);
        tabs.addTab("By Path", pathPanel);

        // Keyboard shortcuts for tab navigation (Alt+1..4 on Windows/Linux).
        // Left as mnemonics; menu-modifier+digit would clash with OS/browser
        // tab conventions on some platforms.
        tabs.setMnemonicAt(0, KeyEvent.VK_1);
        tabs.setMnemonicAt(1, KeyEvent.VK_2);
        tabs.setMnemonicAt(2, KeyEvent.VK_3);
        tabs.setMnemonicAt(3, KeyEvent.VK_4);

        return tabs;
    }

    /**
     * Builds the bottom section containing the action buttons and a status
     * label that shows how many results the last query returned.
     *
     * @param export the "Export to CSV" button
     * @param email  the "Email Results" button
     * @param clear  the "Clear Database" button
     * @param back   the "Return to Main" button
     * @return a laid-out panel for the south region of the form
     */
    private JPanel buildBottomPanel(final JButton export, final JButton email,
                                    final JButton clear, final JButton back) {
        // Stack the button bar above the status label using a BorderLayout.
        JPanel wrapper = new JPanel(new BorderLayout());

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttonBar.add(export);
        buttonBar.add(email);
        buttonBar.add(clear);
        buttonBar.add(back);

        wrapper.add(buttonBar, BorderLayout.CENTER);
        wrapper.add(lblStatus, BorderLayout.SOUTH);

        return wrapper;
    }

    // ═════════════════════════════════════════════════════════════
    //  COMPONENT FACTORY HELPERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Creates the extension combo box pre-populated with the most common
     * file types from the SRS.  The combo box is editable so users can
     * type any extension they need.
     *
     * @return a ready-to-use {@link JComboBox} of extension strings
     */
    private JComboBox<String> createExtensionComboBox() {
        JComboBox<String> combo = new JComboBox<>(DEFAULT_EXTENSIONS);
        combo.setEditable(true);
        combo.setToolTipText("Select a file extension or type your own");
        return combo;
    }

    /**
     * Creates a date spinner configured with a calendar-style editor.
     * The spinner defaults to today's date and lets the user pick any
     * date by clicking the up/down arrows or typing directly.
     *
     * @return a {@link JSpinner} backed by a {@link SpinnerDateModel}
     */
    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd HH:mm"));
        return spinner;
    }

    // ═════════════════════════════════════════════════════════════
    //  QUERY EVENT HANDLERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Handles the "Run Query" action on the Extension tab.
     *
     * <p>Reads the selected or typed extension from {@link #cmbExtension},
     * passes it to {@link QueryEngine#queryByExtension(String)}, and
     * refreshes the results table.  Shows an error dialog if the input
     * is blank or the query engine rejects it.</p>
     */
    private void onExtensionQueryClicked() {
        String ext = (String) cmbExtension.getSelectedItem();
        if (ext == null || ext.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select or type a file extension.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<FileEvent> results = queryEngine.queryByExtension(ext.trim());
            displayResults(results);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the "Run Query" action on the Date Range tab.
     *
     * <p>Converts the two spinner values into {@link LocalDateTime} objects
     * and delegates to {@link QueryEngine#queryByDateRange(LocalDateTime, LocalDateTime)}.
     * The query engine validates that the start date is not after the end date;
     * any validation failure is shown to the user in an error dialog.</p>
     */
    private void onDateRangeQueryClicked() {
        // Convert the java.util.Date from the spinner into LocalDateTime.
        java.util.Date startRaw = (java.util.Date) spnStartDate.getValue();
        java.util.Date endRaw = (java.util.Date) spnEndDate.getValue();

        LocalDateTime start = new java.sql.Timestamp(startRaw.getTime())
                .toLocalDateTime();
        LocalDateTime end = new java.sql.Timestamp(endRaw.getTime())
                .toLocalDateTime();

        try {
            List<FileEvent> results = queryEngine.queryByDateRange(start, end);
            displayResults(results);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the "Run Query" action on the Activity tab.
     *
     * <p>Reads the selected activity type (CREATED, MODIFIED, DELETED,
     * or RENAMED) from {@link #cmbActivity} and passes it to
     * {@link QueryEngine#queryByActivity(String)}.</p>
     */
    private void onActivityQueryClicked() {
        String activity = (String) cmbActivity.getSelectedItem();
        if (activity == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an activity type.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<FileEvent> results = queryEngine.queryByActivity(activity);
            displayResults(results);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the "Run Query" action on the Path tab.
     *
     * <p>Reads the user-entered path fragment from {@link #txtPath} and
     * delegates to {@link QueryEngine#queryByPath(String)}.  The query
     * engine wraps the term in SQL LIKE wildcards internally.</p>
     */
    private void onPathQueryClicked() {
        String path = txtPath.getText();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a path or part of a path to search.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<FileEvent> results = queryEngine.queryByPath(path.trim());
            displayResults(results);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  ACTION-BAR EVENT HANDLERS
    // ═════════════════════════════════════════════════════════════

    /**
     * Prompts the user for a file name and exports the current query
     * results to a CSV file through {@link QueryEngine#saveResultsToCsv(String)}.
     *
     * <p>On success the {@link #btnEmailResults} button is enabled so the
     * user can immediately email the generated file.  If the export fails
     * (I/O error, empty results, etc.) an error dialog is shown.</p>
     */
    private void onExportCsvClicked() {
        String fileName = JOptionPane.showInputDialog(this,
                "Enter the CSV file name:",
                "Export to CSV", JOptionPane.PLAIN_MESSAGE);

        if (fileName == null || fileName.trim().isEmpty()) {
            // User cancelled the dialog or left it blank — do nothing.
            return;
        }

        try {
            File csv = queryEngine.saveResultsToCsv(fileName.trim());
            if (csv != null) {
                lastExportedCsv = csv;
                btnEmailResults.setEnabled(true);
                lblStatus.setText("Exported to: " + csv.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Results exported to: " + csv.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Export failed. Check file permissions and try again.",
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Prompts the user for an email address and sends the last exported
     * CSV file as an attachment through {@link QueryEngine#emailResults(String, File)}.
     *
     * <p>This button is only enabled after a successful CSV export, so
     * {@link #lastExportedCsv} is guaranteed to be non-null when we
     * reach this handler.</p>
     */
    private void onEmailResultsClicked() {
        if (lastExportedCsv == null) {
            JOptionPane.showMessageDialog(this,
                    "Please export to CSV first before emailing.",
                    "No CSV Available", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String recipient = JOptionPane.showInputDialog(this,
                "Enter the recipient email address:",
                "Email Results", JOptionPane.PLAIN_MESSAGE);

        if (recipient == null || recipient.trim().isEmpty()) {
            return;
        }

        try {
            boolean sent = queryEngine.emailResults(recipient.trim(), lastExportedCsv);
            if (sent) {
                lblStatus.setText("Report emailed to " + recipient.trim());
                JOptionPane.showMessageDialog(this,
                        "Report emailed successfully to " + recipient.trim(),
                        "Email Sent", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to send email. Verify that the email service is configured.",
                        "Email Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Email Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Asks for confirmation and then clears every record from the database
     * via {@link QueryEngine#clearDatabase()}.
     *
     * <p>Because this operation is irreversible, a YES/NO confirmation
     * dialog is shown first.  On success the results table is also
     * cleared so the UI stays consistent with the now-empty database.</p>
     */
    private void onClearDatabaseClicked() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will permanently delete all stored file events.\n"
                        + "Are you sure you want to clear the database?",
                "Confirm Clear Database", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean cleared = queryEngine.clearDatabase();
        if (cleared) {
            tableModel.setRowCount(0);
            btnExportCsv.setEnabled(false);
            btnEmailResults.setEnabled(false);
            lastExportedCsv = null;
            lblStatus.setText("Database cleared successfully.");
            JOptionPane.showMessageDialog(this,
                    "Database cleared successfully.",
                    "Database Cleared", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to clear the database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Disposes the QueryForm window and returns focus to the MainForm.
     * Called by the "Return to Main" button and by the window-close event.
     */
    private void onReturnToMainClicked() {
        dispose();
    }

    // ═════════════════════════════════════════════════════════════
    //  RESULTS TABLE HELPER
    // ═════════════════════════════════════════════════════════════

    /**
     * Clears the results table and repopulates it with the given list of
     * {@link FileEvent} objects.
     *
     * <p>Each event is rendered as a single row with five columns matching
     * {@link #TABLE_COLUMNS}.  After populating, the method updates the
     * Export button state and the status bar with the result count.</p>
     *
     * @param events the query results to display; may be empty but not null
     */
    private void displayResults(final List<FileEvent> events) {
        // Wipe previous results so old data never lingers on screen
        tableModel.setRowCount(0);

        for (FileEvent event : events) {
            tableModel.addRow(new Object[]{
                    event.getFileName(),
                    event.getExtension(),
                    event.getPath(),
                    event.getActivityType(),
                    event.getTimeStamp().format(DISPLAY_FORMAT)
            });
        }

        // Only allow CSV export when there is something to export
        btnExportCsv.setEnabled(!events.isEmpty());

        // Reset email state because the old CSV no longer matches these results
        btnEmailResults.setEnabled(false);
        lastExportedCsv = null;

        // Update the status bar with result count instead of a popup
        if (events.isEmpty()) {
            lblStatus.setText("No matching events found.");
        } else {
            lblStatus.setText("Found " + events.size()
                    + " result(s). Click column headers to sort.");
        }
    }
}
