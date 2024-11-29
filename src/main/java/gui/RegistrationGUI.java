package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import agents.RegistrationAgent;
import models.Customer;

public class RegistrationGUI extends JFrame {
    private RegistrationAgent agent;
    private JPanel mainPanel;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;

    public RegistrationGUI(RegistrationAgent agent) {
        this.agent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Customer Registration Management");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Create components
        setupMenuBar();
        setupRegistrationForm();
        setupCustomerTable();
        setupStatusBar();

        // Refresh customer list
        refreshCustomerList();

        add(mainPanel);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshCustomerList());
        fileMenu.add(refreshItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void setupRegistrationForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Customer Registration"));
        GridBagConstraints gbc = new GridBagConstraints();

        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        emailField = new JTextField(20);
        formPanel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Phone:"), gbc);

        gbc.gridx = 1;
        phoneField = new JTextField(20);
        formPanel.add(phoneField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> registerCustomer());
        buttonPanel.add(registerButton);

        JButton deregisterButton = new JButton("Deregister Selected");
        deregisterButton.addActionListener(e -> deregisterSelectedCustomer());
        buttonPanel.add(deregisterButton);

        JButton updateButton = new JButton("Update Selected");
        updateButton.addActionListener(e -> updateSelectedCustomer());
        buttonPanel.add(updateButton);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);
    }

    private void setupCustomerTable() {
        String[] columns = {"ID", "Name", "Email", "Phone", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(customerTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.add(new JLabel("Ready"));
        mainPanel.add(statusBar, BorderLayout.SOUTH);
    }

    private void registerCustomer() {
        try {
            Customer customer = new Customer();
            customer.setName(nameField.getText());
            customer.setEmail(emailField.getText());
            customer.setPhone(phoneField.getText());
            customer.setPassword(new String(passwordField.getPassword()));

            if (validateCustomerInput(customer)) {
                boolean success = agent.registerCustomer(customer);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Customer registered successfully");
                    clearForm();
                    refreshCustomerList();
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deregisterSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow >= 0) {
            String customerId = customerTable.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to deregister this customer?",
                    "Confirm Deregistration",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = agent.deregisterCustomer(customerId);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Customer deregistered successfully");
                    refreshCustomerList();
                } else {
                    JOptionPane.showMessageDialog(this, "Deregistration failed",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a customer to deregister");
        }
    }

    private void updateSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                Customer customer = new Customer();
                customer.setId((Integer) customerTable.getValueAt(selectedRow, 0));
                customer.setName(nameField.getText());
                customer.setPhone(phoneField.getText());

                if (validateCustomerInput(customer)) {
                    boolean success = agent.updateCustomer(customer);
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Customer updated successfully");
                        clearForm();
                        refreshCustomerList();
                    } else {
                        JOptionPane.showMessageDialog(this, "Update failed",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a customer to update");
        }
    }

    private boolean validateCustomerInput(Customer customer) {
        if (customer.getName().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required");
            return false;
        }
        if (customer.getEmail() != null && !customer.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Invalid email format");
            return false;
        }

        if (customer.getPhone() != null && !customer.getPhone().matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Phone number must be 10 digits");
            return false;
        }

        if (customer.getPassword() != null && customer.getPassword().length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        passwordField.setText("");
    }

    public void refreshCustomerList() {
        tableModel.setRowCount(0);
        List<Customer> customers = agent.getAllCustomers();

        for (Customer customer : customers) {
            Object[] row = {
                    customer.getId(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getPhone(),
                    customer.getStatus()
            };
            tableModel.addRow(row);
        }
    }
}