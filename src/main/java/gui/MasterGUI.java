package gui;

import agents.MasterAgent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;

public class MasterGUI extends JFrame {
    private MasterAgent myAgent;
    private JTextArea logArea;
    private JPanel buttonPanel;

    public MasterGUI(MasterAgent agent) {
        super("Car Rental Management System");
        this.myAgent = agent;
        setupGUI();
    }

    private void setupGUI() {
        // Set window properties
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        // Main container with padding
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // Header Panel
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // Center Panel with Buttons
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);

        // Log Panel at bottom
        mainPanel.add(createLogPanel(), BorderLayout.SOUTH);

        // Set window size and position
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("Car Rental Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Select an operation to continue");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Create buttons with icons and descriptions
        String[][] buttonInfo = {
                {"Registration", "Manage customer registrations and profiles", "1"},
                {"Reservations", "Book and manage car reservations", "2"},
                {"Vehicle Management", "Manage vehicle inventory and maintenance", "3"},
                {"Payments", "Process payments and handle billing", "4"}
        };

        for (int i = 0; i < buttonInfo.length; i++) {
            gbc.gridx = i % 2;
            gbc.gridy = i / 2;
            centerPanel.add(createModuleButton(buttonInfo[i][0], buttonInfo[i][1], Integer.parseInt(buttonInfo[i][2])), gbc);
        }

        return centerPanel;
    }

    private JPanel createModuleButton(String title, String description, int choice) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout(5, 5));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        buttonPanel.setBackground(Color.WHITE);

        // Create main button with hover effect
        JButton button = new JButton(title);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setPreferredSize(new Dimension(200, 100));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(220, 220, 220));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(240, 240, 240));
            }
        });

        // Add action listener
        button.addActionListener(e -> {
            updateLog("Opening " + title + " module...");
            myAgent.processRequest(choice);
        });

        // Add description label
        JLabel descLabel = new JLabel("<html><div style='text-align: center;'>" + description + "</div></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setBorder(new EmptyBorder(5, 0, 0, 0));

        buttonPanel.add(button, BorderLayout.CENTER);
        buttonPanel.add(descLabel, BorderLayout.SOUTH);

        return buttonPanel;
    }

    private JPanel createLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "System Log",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 12)
        ));

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JButton clearButton = new JButton("Clear Log");
        clearButton.addActionListener(e -> logArea.setText(""));

        logPanel.add(scrollPane, BorderLayout.CENTER);
        logPanel.add(clearButton, BorderLayout.EAST);

        return logPanel;
    }

    public void updateLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", new java.util.Date());
            logArea.append(timestamp + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}