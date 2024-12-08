package gui;

import agents.RegistrationAgent;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RegistrationGUI extends JFrame {
    private RegistrationAgent myAgent;
    private JTextField customerIdField;  // Add this with other field declarations
    private String userRole;
    private Integer customerId;

    // Form Components
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField licenseField;
    private JTextArea addressArea;
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;
    private JButton registerButton;
    private JButton updateButton;
    private JButton deregisterButton;
    private JButton clearButton;
    private JSplitPane splitPane;

    public RegistrationGUI(RegistrationAgent agent, String role, Integer customerId) {
        super(role.equals("ADMIN") ? "Car Rental System - Customer Registration" : "My Profile");
        this.myAgent = agent;
        this.userRole = role;
        this.customerId = customerId;
        setupGUI();

        if ("USER".equals(role) && customerId != null) {
            loadCustomerProfile();
            setFormEditable(false);  // Initially set form to read-only
        }
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel with home button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton homeButton = new JButton("Home");
        homeButton.setPreferredSize(new Dimension(100, 30));
        homeButton.setFont(new Font("Arial", Font.PLAIN, 14));
        homeButton.addActionListener(e -> goToHome());
        topPanel.add(homeButton);
        add(topPanel, BorderLayout.NORTH);

        // Main split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);

        // Left panel - Customer Form
        JPanel leftPanel = createRegistrationPanel();  // Changed from createFormPanel
        leftPanel.setPreferredSize(new Dimension(500, 600));
        splitPane.setLeftComponent(leftPanel);

        // Right panel - Customers Table (only for admin)
        if ("ADMIN".equals(userRole)) {
            JPanel rightPanel = createCustomersPanel();
            rightPanel.setPreferredSize(new Dimension(600, 600));
            splitPane.setRightComponent(rightPanel);
        } else {
            // For regular users, show their profile details in a read-only format
            JPanel rightPanel = createCustomerDetailsPanel();
            rightPanel.setPreferredSize(new Dimension(600, 600));
            splitPane.setRightComponent(rightPanel);
        }

        add(splitPane, BorderLayout.CENTER);

        // Status Panel at bottom
        add(createStatusPanel(), BorderLayout.SOUTH);

        // Window settings
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setMinimumSize(new Dimension(1200, 800));

        // Pack and set divider
        pack();
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.5);
        });

        setLocationRelativeTo(null);

        // If user role, load profile data
        if ("USER".equals(userRole) && customerId != null) {
            loadCustomerProfile();
            setFormEditable(false);  // Initially set form to read-only
        }
    }

    // New method to create customer details panel for regular users
    private JPanel createCustomerDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("My Profile Details"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Create text area for displaying details
        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Arial", Font.PLAIN, 14));
        detailsArea.setMargin(new Insets(10, 10, 10, 10));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        refreshCustomerDetails(detailsArea);

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button panel at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton editButton = createStyledButton("Edit Profile");
        JButton refreshButton = createStyledButton("Refresh");

        editButton.addActionListener(e -> handleEditProfile());
        refreshButton.addActionListener(e -> {
            refreshCustomerDetails(detailsArea);
            updateStatus("Profile refreshed");
        });

        buttonPanel.add(editButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshCustomerDetails(JTextArea detailsArea) {
        Map<String, Object> customerData = myAgent.getCustomerDetails(customerId);
        if (!customerData.isEmpty()) {
            StringBuilder details = new StringBuilder();
            details.append("Customer ID: ").append(customerData.get("id")).append("\n\n");
            details.append("Name: ").append(customerData.get("firstName")).append(" ")
                    .append(customerData.get("lastName")).append("\n\n");
            details.append("Email: ").append(customerData.get("email")).append("\n\n");
            details.append("Phone: ").append(customerData.get("phone")).append("\n\n");
            details.append("Driver's License: ").append(customerData.get("driversLicense")).append("\n\n");
            details.append("Address: ").append(customerData.get("address")).append("\n\n");
            details.append("Status: ").append(customerData.get("status")).append("\n");

            detailsArea.setText(details.toString());
        }
    }

    // New method to handle edit profile functionality
    private void handleEditProfile() {
        if (updateButton.isEnabled()) {
            // Switching to view mode
            setFormEditable(false);
            updateButton.setEnabled(false);
            loadCustomerProfile(); // Reload original data
        } else {
            // Switching to edit mode
            setFormEditable(true);
            updateButton.setEnabled(true);
        }
    }

    // New method to control form editability
    private void setFormEditable(boolean editable) {
        firstNameField.setEditable(editable);
        lastNameField.setEditable(editable);
        emailField.setEditable(editable);
        phoneField.setEditable(editable);
        licenseField.setEditable(editable);
        addressArea.setEditable(editable);
    }

    private void loadCustomerProfile() {
        Map<String, Object> customerData = myAgent.getCustomerDetails(customerId);
        if (!customerData.isEmpty()) {
            firstNameField.setText((String)customerData.get("firstName"));
            lastNameField.setText((String)customerData.get("lastName"));
            emailField.setText((String)customerData.get("email"));
            phoneField.setText((String)customerData.get("phone"));
            licenseField.setText((String)customerData.get("driversLicense"));
            addressArea.setText((String)customerData.get("address"));
        }
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Customer Registration"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Form fields panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Form fields
        addLabelAndField(formPanel, gbc, 0, "First Name:", firstNameField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 1, "Last Name:", lastNameField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 2, "Email:", emailField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 3, "Phone:", phoneField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 4, "Driver's License:", licenseField = createStyledTextField());

        // Address
        JLabel addressLabel = createStyledLabel("Address:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formPanel.add(addressLabel, gbc);

        gbc.gridx = 1;
        addressArea = new JTextArea(4, 20);
        addressArea.setFont(new Font("Arial", Font.PLAIN, 14));
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);
        addressScroll.setPreferredSize(new Dimension(300, 80));
        formPanel.add(addressScroll, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(createButtonsPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField field) {
        JLabel label = createStyledLabel(labelText);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(label, gbc);

        gbc.gridx = 1;
        field.setPreferredSize(new Dimension(300, 30));
        panel.add(field, gbc);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        return field;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        if ("ADMIN".equals(userRole)) {
            // Admin sees all buttons
            registerButton = createStyledButton("Register");
            updateButton = createStyledButton("Update");
            deregisterButton = createStyledButton("Deregister");
            clearButton = createStyledButton("Clear");

            // Add action listeners
            registerButton.addActionListener(e -> registerCustomer());
            updateButton.addActionListener(e -> updateCustomer());
            deregisterButton.addActionListener(e -> deregisterCustomer());
            clearButton.addActionListener(e -> clearForm());

            updateButton.setEnabled(false);
            deregisterButton.setEnabled(false);

            buttonPanel.add(registerButton);
            buttonPanel.add(updateButton);
            buttonPanel.add(deregisterButton);
            buttonPanel.add(clearButton);
        } else {
            // Regular user only sees update button
            updateButton = createStyledButton("Update");
            updateButton.addActionListener(e -> updateCustomer());
            updateButton.setEnabled(false);

            buttonPanel.add(updateButton);
        }

        return buttonPanel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 35));
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        return button;
    }

    private JPanel createCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Registered Customers"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Updated column names to include Address
        String[] columnNames = {"ID", "Name", "Email", "Phone", "License", "Address", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customersTable = new JTable(tableModel);
        customersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.setFont(new Font("Arial", Font.PLAIN, 14));
        customersTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        customersTable.setRowHeight(30);

        // Configure column widths
        TableColumnModel columnModel = customersTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);  // ID
        columnModel.getColumn(1).setPreferredWidth(150); // Name
        columnModel.getColumn(2).setPreferredWidth(180); // Email
        columnModel.getColumn(3).setPreferredWidth(120); // Phone
        columnModel.getColumn(4).setPreferredWidth(120); // License
        columnModel.getColumn(5).setPreferredWidth(200); // Address
        columnModel.getColumn(6).setPreferredWidth(100); // Status

        // Add word wrap for the address column
        columnModel.getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setToolTipText(value.toString()); // Show full address on hover
                }
                return c;
            }
        });

        customersTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());

        JScrollPane scrollPane = new JScrollPane(customersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> refreshCustomerTable());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(refreshButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Status"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        statusArea = new JTextArea(4, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(statusArea);

        JButton clearLogButton = createStyledButton("Clear Log");
        clearLogButton.addActionListener(e -> statusArea.setText(""));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(clearLogButton, BorderLayout.EAST);

        return panel;
    }

    private void goToHome() {
        this.setVisible(false);
        clearForm();
        myAgent.redirectToHome();
    }

    private void registerCustomer() {

        if ("USER".equals(userRole)) {
            updateStatus("Operation not permitted for regular users");
            return;
        }

        if (!validateForm()) return;

        Map<String, String> customerData = getFormData();
        if (myAgent.registerCustomer(customerData)) {
            updateStatus("Customer registered successfully: " + customerData.get("email"));
            clearForm();
            refreshCustomerTable();
        } else {
            updateStatus("Failed to register customer");
        }
    }

    private void updateCustomer() {
        if (!validateForm()) return;

        Map<String, String> customerData = getFormData();
        if ("USER".equals(userRole)) {
            // For users, ensure they can only update their own profile
            customerData.put("customerId", customerId.toString());
        }

        if (myAgent.updateCustomer(customerData)) {
            updateStatus("Customer updated successfully: " + customerData.get("email"));
            if ("ADMIN".equals(userRole)) {
                clearForm();
                refreshCustomerTable();
            }
        } else {
            updateStatus("Failed to update customer");
        }
    }

    private void deregisterCustomer() {
        if ("USER".equals(userRole)) {
            updateStatus("Operation not permitted for regular users");
            return;
        }
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a customer to deregister",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to deregister this customer?",
                "Confirm Deregistration",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (myAgent.deregisterCustomer(email)) {
                updateStatus("Customer deregistered successfully: " + email);
                clearForm();
                refreshCustomerTable();
            } else {
                updateStatus("Failed to deregister customer");
            }
        }
    }

    private void handleTableSelection() {
        int selectedRow = customersTable.getSelectedRow();
        if (selectedRow >= 0) {
            String fullName = (String) tableModel.getValueAt(selectedRow, 1);
            String[] names = fullName.split(" ", 2);

            firstNameField.setText(names[0]);
            lastNameField.setText(names.length > 1 ? names[1] : "");
            emailField.setText((String) tableModel.getValueAt(selectedRow, 2));
            phoneField.setText((String) tableModel.getValueAt(selectedRow, 3));
            licenseField.setText((String) tableModel.getValueAt(selectedRow, 4));
            addressArea.setText((String) tableModel.getValueAt(selectedRow, 5)); // Added address

            updateButton.setEnabled(true);
            deregisterButton.setEnabled(true);
        }
    }

    private void clearForm() {
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        licenseField.setText("");
        addressArea.setText("");

        // Only clear table selection if it's an admin view
        if ("ADMIN".equals(userRole) && customersTable != null) {
            customersTable.clearSelection();
        }

        // Only handle these buttons for admin view
        if ("ADMIN".equals(userRole)) {
            updateButton.setEnabled(false);
            deregisterButton.setEnabled(false);
        }
    }

    private void refreshCustomerTable() {
        tableModel.setRowCount(0);
        List<Map<String, Object>> customers = myAgent.getAllCustomers();

        for (Map<String, Object> customer : customers) {
            Object[] row = {
                    customer.get("id"),
                    customer.get("firstName") + " " + customer.get("lastName"),
                    customer.get("email"),
                    customer.get("phone"),
                    customer.get("driversLicense"),
                    customer.get("address"),     // Added address
                    customer.get("status")
            };
            tableModel.addRow(row);
        }
    }

    private boolean validateForm() {
        if (firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                licenseField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private Map<String, String> getFormData() {
        Map<String, String> data = new HashMap<>();
        data.put("firstName", firstNameField.getText().trim());
        data.put("lastName", lastNameField.getText().trim());
        data.put("email", emailField.getText().trim());
        data.put("phone", phoneField.getText().trim());
        data.put("driversLicense", licenseField.getText().trim());
        data.put("address", addressArea.getText().trim());
        return data;
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", new java.util.Date());
            statusArea.append(timestamp + message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
}