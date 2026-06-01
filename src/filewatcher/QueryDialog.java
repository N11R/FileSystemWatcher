package filewatcher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * QueryDialog provides a GUI window for querying the File System Watcher
 * database, exporting results to CSV, and emailing reports.
 *
 * <p>Supports four query types: by file extension, date range,
 * activity type, and directory path.</p>
 *
 * <p><b>SRS Coverage:</b></p>
 * <ul>
 *   <li>FR-3.1 through FR-3.4 — Query by extension, date, activity, path</li>
 *   <li>FR-3.8 — Clear database</li>
 *   <li>FR-4.1 through FR-4.8 — CSV export</li>
 *   <li>FR-5.1 through FR-5.8 — Email report</li>
 * </ul>
 *
 * @author Mariam Hussein & Nasra Hussein
 * @version 1.0
 */
public class QueryDialog extends JDialog {

    // ─────────────────────────────────────────────────────────────
    // CONSTANTS
    // ─────────────────────────────────────────────────────────────

    /** Column headers for the results table. */
    private static final String[] COLUMNS =
            {"File Name", "Extension", "Path", "Activity", "Timestamp"};

    /** Query type options shown in the dropdown. */
    private static final String[] QUERY_TYPES =
            {"By Extension", "By Activity", "By Path", "By Date Range"};

    // ─────────────────────────────────────────────────────────────
    // GUI COMPONENTS
    // ─────────────────────────────────────────────────────────────

    /** Dropdown to select query type. */
    private final JComboBox<String> cmbQueryType;

    /** Primary input field (extension, activity, or path). */
    private final JTextField txtParam;

    /** Label for the primary input field. */
    private final JLabel lblParam;

    /** Start date field, shown only for date range queries. */
    private final JTextField txtStartDate;

    /** End date field, shown only for date range queries. */
    private final JTextField txtEndDate;

    /** Panel that holds date range fields, toggled by query type. */
    private final JPanel datePanel;

    /** Table displaying query results. */
    private final JTable resultsTable;

    /** Table model backing the results table. */
    private final DefaultTableModel tableModel;

    /** Label showing result count or status messages. */
    private final JLabel lblStatus;

    /** Button to run the selected query. */
    private final JButton btnQuery;

    /** Button to export results to CSV. */
    private final JButton btnExport;

    /** Button to email the last exported CSV. */
    private final JButton btnEmail;

    /** Button to clear the entire database. */
    private final JButton btnClear;

    // ─────────────────────────────────────────────────────────────
    // STATE
    // ─────────────────────────────────────────────────────────────

    /** The QueryEngine used to run queries and export results. */
    private final QueryEngine queryEngine;

    /** The last CSV file exported, used for the email action. */
    private File lastExportedFile;

    // ─────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────

    /**
     * Constructs the QueryDialog.
     *
     * @param parent      the parent JFrame
     * @param queryEngine the QueryEngine to delegate queries to
     */
    public QueryDialog(final JFrame parent, final QueryEngine queryEngine) {
        super(parent, "Query Database", true);
        this.queryEngine = queryEngine;
        this.lastExportedFile = null;

        // ── initialize components ──────────────────────────────
        cmbQueryType = new JComboBox<>(QUERY_TYPES);
        txtParam     = new JTextField(20);
        lblParam     = new JLabel("Extension (e.g. .txt):");
        txtStartDate = new JTextField("2026-01-01T00:00:00", 16);
        txtEndDate   = new JTextField("2026-12-31T23:59:59", 16);
        lblStatus    = new JLabel("Enter a query and press Run.");
        btnQuery     = new JButton("Run Query");
        btnExport    = new JButton("Export CSV");
        btnEmail     = new JButton("Email Report");
        btnClear     = new JButton("Clear DB");

        // ── table setup ────────────────────────────────────────
        tableModel   = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // read-only table
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setRowHeight(22);

        // ── date range panel (hidden by default) ───────────────
        datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        datePanel.add(new JLabel("From:"));
        datePanel.add(txtStartDate);
        datePanel.add(new JLabel("To:"));
        datePanel.add(txtEndDate);
        datePanel.setVisible(false);

        // ── initial button states ──────────────────────────────
        btnExport.setEnabled(false);
        btnEmail.setEnabled(false);

        // ── wire up listeners ─────────────────────────────────
        cmbQueryType.addActionListener(this::onQueryTypeChanged);
        btnQuery.addActionListener(this::onRunQuery);
        btnExport.addActionListener(this::onExportCsv);
        btnEmail.addActionListener(this::onEmailReport);
        btnClear.addActionListener(this::onClearDatabase);

        // ── layout ─────────────────────────────────────────────
        setLayout(new BorderLayout(8, 8));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        // ── window settings ───────────────────────────────────
        setSize(800, 500);
        setMinimumSize(new Dimension(650, 400));
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // PANEL BUILDERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Builds the top panel containing query type selector and inputs.
     */
    private JPanel buildTopPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        // row 1: query type selector
        final JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        typeRow.add(new JLabel("Query Type:"));
        typeRow.add(cmbQueryType);
        panel.add(typeRow);

        // row 2: parameter input (switches between text field and date panel)
        final JPanel paramRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        paramRow.add(lblParam);
        paramRow.add(txtParam);
        paramRow.add(datePanel);
        paramRow.add(btnQuery);
        panel.add(paramRow);

        return panel;
    }

    /**
     * Builds the center panel containing the scrollable results table.
     */
    private JPanel buildCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        final JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Results"));
        panel.add(scroll, BorderLayout.CENTER);

        // status bar below the table
        final JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        lblStatus.setForeground(Color.DARK_GRAY);
        statusBar.add(lblStatus);
        panel.add(statusBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Builds the bottom panel containing export, email, and clear buttons.
     */
    private JPanel buildBottomPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnClear.setForeground(new Color(180, 30, 30));
        panel.add(btnClear);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnExport);
        panel.add(btnEmail);
        final JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // EVENT HANDLERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Adjusts the input fields when the query type changes.
     */
    private void onQueryTypeChanged(final ActionEvent e) {
        final String selected = (String) cmbQueryType.getSelectedItem();
        final boolean isDateRange = "By Date Range".equals(selected);

        txtParam.setVisible(!isDateRange);
        datePanel.setVisible(isDateRange);

        switch (selected) {
            case "By Extension" -> lblParam.setText("Extension (e.g. .txt):");
            case "By Activity"  -> lblParam.setText("Activity (e.g. CREATED):");
            case "By Path"      -> lblParam.setText("Path contains:");
            case "By Date Range"-> lblParam.setText("Date Range:");
        }

        // force layout refresh
        lblParam.getParent().revalidate();
        lblParam.getParent().repaint();
    }

    /**
     * Runs the selected query and populates the results table.
     */
    private void onRunQuery(final ActionEvent e) {
        final String type = (String) cmbQueryType.getSelectedItem();
        List<FileEvent> results;

        try {
            results = switch (type) {
                case "By Extension"  -> queryEngine.queryByExtension(txtParam.getText());
                case "By Activity"   -> queryEngine.queryByActivity(txtParam.getText());
                case "By Path"       -> queryEngine.queryByPath(txtParam.getText());
                case "By Date Range" -> runDateRangeQuery();
                default -> throw new IllegalStateException("Unknown query type.");
            };
        } catch (IllegalArgumentException ex) {
            showError("Invalid input: " + ex.getMessage());
            return;
        } catch (DateTimeParseException ex) {
            showError("Date format must be: yyyy-MM-ddTHH:mm:ss");
            return;
        }

        populateTable(results);

        final int count = results.size();
        lblStatus.setText(count == 0
                ? "No results found."
                : count + " result(s) found.");

        btnExport.setEnabled(count > 0);
        btnEmail.setEnabled(false); // reset until a new export is done
        lastExportedFile = null;
    }

    /**
     * Parses the date fields and runs a date range query.
     */
    private List<FileEvent> runDateRangeQuery() {
        final LocalDateTime start = LocalDateTime.parse(txtStartDate.getText().trim());
        final LocalDateTime end   = LocalDateTime.parse(txtEndDate.getText().trim());
        return queryEngine.queryByDateRange(start, end);
    }

    /**
     * Exports the current query results to a CSV file chosen by the user.
     */
    private void onExportCsv(final ActionEvent e) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV Report");
        chooser.setSelectedFile(new File("fsw_report.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final String path = chooser.getSelectedFile().getAbsolutePath();
        final File exported = queryEngine.saveResultsToCsv(path);

        if (exported != null && exported.exists()) {
            lastExportedFile = exported;
            btnEmail.setEnabled(true);
            lblStatus.setText("Exported to: " + exported.getName());
        } else {
            showError("Export failed. Check file path and permissions.");
        }
    }

    /**
     * Emails the last exported CSV to a recipient entered by the user.
     */
    private void onEmailReport(final ActionEvent e) {
        if (lastExportedFile == null || !lastExportedFile.exists()) {
            showError("No exported file found. Please export first.");
            return;
        }

        final String recipient = JOptionPane.showInputDialog(
                this,
                "Enter recipient email address:",
                "Email Report",
                JOptionPane.PLAIN_MESSAGE
        );

        if (recipient == null || recipient.trim().isEmpty()) {
            return; // user cancelled
        }

        final boolean sent = queryEngine.emailResults(recipient.trim(), lastExportedFile);

        if (sent) {
            JOptionPane.showMessageDialog(this,
                    "Report sent to: " + recipient.trim(),
                    "Email Sent",
                    JOptionPane.INFORMATION_MESSAGE);
            lblStatus.setText("Report emailed to: " + recipient.trim());
        } else {
            showError("Failed to send email. Check config.properties and network.");
        }
    }

    /**
     * Prompts the user to confirm, then clears all records from the database.
     */
    private void onClearDatabase(final ActionEvent e) {
        final int confirm = JOptionPane.showConfirmDialog(
                this,
                "This will permanently delete all records. Continue?",
                "Clear Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        final boolean cleared = queryEngine.clearDatabase();

        if (cleared) {
            tableModel.setRowCount(0);
            btnExport.setEnabled(false);
            btnEmail.setEnabled(false);
            lastExportedFile = null;
            lblStatus.setText("Database cleared.");
        } else {
            showError("Failed to clear database.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Populates the results table with the given list of FileEvents.
     *
     * @param events the list of FileEvents to display
     */
    private void populateTable(final List<FileEvent> events) {
        tableModel.setRowCount(0); // clear existing rows
        for (final FileEvent ev : events) {
            tableModel.addRow(new Object[]{
                    ev.getFileName(),
                    ev.getExtension(),
                    ev.getPath(),
                    ev.getActivityType(),
                    ev.getTimeStamp()
            });
        }
    }

    /**
     * Shows an error dialog with the given message.
     *
     * @param message the error message to display
     */
    private void showError(final String message) {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
        lblStatus.setText("Error: " + message);
    }
}