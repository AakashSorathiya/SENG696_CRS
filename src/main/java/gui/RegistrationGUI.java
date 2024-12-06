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

    public RegistrationGUI(RegistrationAgent agent) {
        super("Car Rental System - Customer Registration");
        this.myAgent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // Left panel - Registration Form
        splitPane.setLeftComponent(createRegistrationPanel());

        // Right panel - Customers Table
        splitPane.setRightComponent(createCustomersPanel());

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

        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Customer Registration"));

        // Form fields panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Add form fields
        gbc.gridx = 0; gbc.gridy = 0;
        addFormField(formPanel, "First Name:", firstNameField = new JTextField(20), gbc);

        gbc.gridy++;
        addFormField(formPanel, "Last Name:", lastNameField = new JTextField(20), gbc);

        gbc.gridy++;
        addFormField(formPanel, "Email:", emailField = new JTextField(20), gbc);

        gbc.gridy++;
        addFormField(formPanel, "Phone:", phoneField = new JTextField(20), gbc);

        gbc.gridy++;
        addFormField(formPanel, "Driver's License:", licenseField = new JTextField(20), gbc);

        gbc.gridy++;
        JLabel addressLabel = new JLabel("Address:");
        formPanel.add(addressLabel, gbc);

        gbc.gridy++;
        addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);
        formPanel.add(addressScroll, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        panel.add(createButtonsPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private void addFormField(JPanel panel, String label, JTextField field, GridBagConstraints gbc) {
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
        gbc.gridx = 0;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        registerButton = new JButton("Register");
        updateButton = new JButton("Update");
        deregisterButton = new JButton("Deregister");
        clearButton = new JButton("Clear");

        registerButton.addActionListener(e -> registerCustomer());
        updateButton.addActionListener(e -> updateCustomer());
        deregisterButton.addActionListener(e -> deregisterCustomer());
        clearButton.addActionListener(e -> clearForm());

        buttonPanel.add(registerButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deregisterButton);
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private JPanel createCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Registered Customers"));

        // Create table model
        String[] columnNames = {"ID", "Name", "Email", "Phone", "License", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table
        customersTable = new JTable(tableModel);
        customersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());

        JScrollPane scrollPane = new JScrollPane(customersTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCustomerTable());
        panel.add(refreshButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusArea = new JTextArea(4, 40);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);

        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> statusArea.setText(""));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(clearLogButton, BorderLayout.EAST);

        return panel;
    }

    private void registerCustomer() {
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
        if (myAgent.updateCustomer(customerData)) {
            updateStatus("Customer updated successfully: " + customerData.get("email"));
            clearForm();
            refreshCustomerTable();
        } else {
            updateStatus("Failed to update customer");
        }
    }

    private void deregisterCustomer() {
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
            // Fill form with selected customer data
            String fullName = (String) tableModel.getValueAt(selectedRow, 1);
            String[] names = fullName.split(" ", 2);

            firstNameField.setText(names[0]);
            lastNameField.setText(names.length > 1 ? names[1] : "");
            emailField.setText((String) tableModel.getValueAt(selectedRow, 2));
            phoneField.setText((String) tableModel.getValueAt(selectedRow, 3));
            licenseField.setText((String) tableModel.getValueAt(selectedRow, 4));

            // Enable/disable buttons appropriately
            updateButton.setEnabled(true);
            deregisterButton.setEnabled(true);
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
                    customer.get("status")
            };
            tableModel.addRow(row);
        }
    }

    private void clearForm() {
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        licenseField.setText("");
        addressArea.setText("");
        customersTable.clearSelection();

        updateButton.setEnabled(false);
        deregisterButton.setEnabled(false);
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