package gui;

// MasterGUI.java
import agents.MasterAgent;

// MasterGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MasterGUI extends JFrame {
    private MasterAgent myAgent;
    private JTextArea logArea;

    public MasterGUI(MasterAgent agent) {
        super("Car Rental System - Master Control");
        this.myAgent = agent;

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title Panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Car Rental System Control Panel");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(titleLabel);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create buttons with icons (you can add icons later)
        addButton(buttonPanel, "Registration", "Manage customer registrations", 1);
        addButton(buttonPanel, "Reservations", "Manage car reservations", 2);
        addButton(buttonPanel, "Vehicle Management", "Manage vehicle inventory", 3);
        addButton(buttonPanel, "Payments", "Process payments and refunds", 4);

        // Log Panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("System Log"));
        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(scrollPane);

        // Add all panels to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        setContentPane(mainPanel);

        // Window closing behavior
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        // Basic frame settings
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void addButton(JPanel panel, String text, String tooltip, int choice) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(150, 60));
        button.setFont(new Font("Arial", Font.PLAIN, 14));

        button.addActionListener(e -> {
            updateLog("Selected: " + text);
            myAgent.processRequest(choice);
        });

        panel.add(button);
    }

    public void updateLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void display() {
        pack();
        centerOnScreen();
        setVisible(true);
    }

    private void centerOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
                (screenSize.width - getWidth()) / 2,
                (screenSize.height - getHeight()) / 2
        );
    }
}
