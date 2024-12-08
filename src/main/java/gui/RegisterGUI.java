package gui;

import agents.MasterAgent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RegisterGUI extends JFrame {
    private MasterAgent myAgent;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField licenseField;
    private JTextArea addressArea;

    public RegisterGUI(MasterAgent agent) {
        super("Car Rental System - New User Registration");
        this.myAgent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("New User Registration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Reset gridwidth
        gbc.gridwidth = 1;

        // Add form fields
        int gridy = 1;

        // Account Information Section
        addSectionLabel(mainPanel, gbc, "Account Information", gridy++);

        addFormField(mainPanel, gbc, "Username:", usernameField = new JTextField(20), gridy++);
        addFormField(mainPanel, gbc, "Password:", passwordField = new JPasswordField(20), gridy++);
        addFormField(mainPanel, gbc, "Confirm Password:", confirmPasswordField = new JPasswordField(20), gridy++);

        // Personal Information Section
        addSectionLabel(mainPanel, gbc, "Personal Information", gridy++);

        addFormField(mainPanel, gbc, "First Name:", firstNameField = new JTextField(20), gridy++);
        addFormField(mainPanel, gbc, "Last Name:", lastNameField = new JTextField(20), gridy++);
        addFormField(mainPanel, gbc, "Email:", emailField = new JTextField(20), gridy++);
        addFormField(mainPanel, gbc, "Phone:", phoneField = new JTextField(20), gridy++);
        addFormField(mainPanel, gbc, "Driver's License:", licenseField = new JTextField(20), gridy++);

        // Address
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        mainPanel.add(new JLabel("Address:"), gbc);

        gbc.gridx = 1;
        addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(addressArea);
        mainPanel.add(scrollPane, gbc);

        // Buttons Panel
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(createButtonPanel(), gbc);

        // Add main panel to scroll pane
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setBorder(null);
        add(mainScrollPane, BorderLayout.CENTER);

        // Window settings
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addSectionLabel(JPanel panel, GridBagConstraints gbc, String text, int gridy) {
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        JLabel sectionLabel = new JLabel(text);
        sectionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(sectionLabel, gbc);
        gbc.gridwidth = 1;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String label, JComponent field, int gridy) {
        gbc.gridx = 0;
        gbc.gridy = gridy;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> register());

        JButton backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> backToLogin());

        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        return buttonPanel;
    }

    private void register() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        // Create user data map
        Map<String, String> userData = new HashMap<>();
        userData.put("username", usernameField.getText().trim());
        userData.put("password", new String(passwordField.getPassword()));
        userData.put("firstName", firstNameField.getText().trim());
        userData.put("lastName", lastNameField.getText().trim());
        userData.put("email", emailField.getText().trim());
        userData.put("phone", phoneField.getText().trim());
        userData.put("driversLicense", licenseField.getText().trim());
        userData.put("address", addressArea.getText().trim());

        if (myAgent.registerNewUser(userData)) {
            JOptionPane.showMessageDialog(this,
                    "Registration successful! Please login with your new account.",
                    "Registration Success",
                    JOptionPane.INFORMATION_MESSAGE);
            this.dispose(); // Close registration window
            // Login window should still be visible in the background
        }
    }

    private boolean validateInput() {
        // Username validation
        if (usernameField.getText().trim().length() < 4) {
            showError("Username must be at least 4 characters long");
            return false;
        }

        // Password validation
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        // Required fields validation
        if (firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                phoneField.getText().trim().isEmpty() ||
                licenseField.getText().trim().isEmpty() ||
                addressArea.getText().trim().isEmpty()) {
            showError("All fields are required");
            return false;
        }

        // Email validation
        if (!emailField.getText().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void backToLogin() {
        // Just close this window, login window should still be visible
        this.dispose();
    }
}