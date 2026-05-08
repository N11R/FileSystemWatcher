package filewatcher;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {

    // constructor
    public AboutDialog(JFrame parent) {

        // step 1 - set up as modal dialog
        super(parent, "About File System Watcher", true);

        // step 2 - window size
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setResizable(false);

        // step 3 - build content
        setLayout(new BorderLayout());

        // title label
        JLabel titleLabel = new JLabel("File System Watcher", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // center panel - version, developers, usage info
        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setBackground(getBackground());
        infoText.setFont(new Font("Arial", Font.PLAIN, 13));
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        infoText.setText(
                "Version: 1.0\n\n" +
                        "Developers: Mariam Hussein & Nasra Hussein\n" +
                        "TCSS 360 — University of Washington Tacoma\n\n" +
                        "Usage:\n" +
                        "1. Enter a directory path and select a file extension.\n" +
                        "2. Click Start to begin monitoring for file events.\n" +
                        "3. Click Write to DB to save events to the database.\n" +
                        "4. Use Query to search and export logged events."
        );
        add(infoText, BorderLayout.CENTER);

        // close button
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel();
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        btnPanel.add(btnClose);
        add(btnPanel, BorderLayout.SOUTH);

        // step 4 - show dialog
        setVisible(true);
    }
}