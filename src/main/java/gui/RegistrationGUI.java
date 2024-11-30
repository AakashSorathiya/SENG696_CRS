package gui;

// RegistrationGUI.java
import agents.RegistrationAgent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class RegistrationGUI extends JFrame {
    private RegistrationAgent myAgent;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField licenseField;
    private JTextArea addressArea;
    private JTextArea statusArea;

    public RegistrationGUI(RegistrationAgent agent) {
        super("Car Rental System - Registration Agent");
        this.myAgent = agent;

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Registration form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Add form fields
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        firstNameField = new JTextField(20);
        formPanel.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lastNameField = new JTextField(20);
        formPanel.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        formPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(20);
        formPanel.add(phoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Driver's License:"), gbc);
        gbc.gridx = 1;
        licenseField = new JTextField(20);
        formPanel.add(licenseField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        addressArea = new JTextArea(3, 20);
        addressArea.setLineWrap(true);
        JScrollPane addressScroll = new JScrollPane(addressArea);
        formPanel.add(addressScroll, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                registerCustomer();
            }
        });

        JButton deregisterButton = new JButton("Deregister");
        deregisterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                String email = emailField.getText().trim();
                if (!email.isEmpty()) {
                    myAgent.deregisterCustomerFromGUI(email);
                }
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                clearFields();
            }
        });

        buttonPanel.add(registerButton);
        buttonPanel.add(deregisterButton);
        buttonPanel.add(clearButton);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusArea = new JTextArea(10, 40);
        statusArea.setEditable(false);
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusPanel.add(new JLabel("Status:"), BorderLayout.NORTH);
        statusPanel.add(statusScroll, BorderLayout.CENTER);

        // Add all panels to main panel
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        getContentPane().add(mainPanel);

        // Window closing behavior
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

    private void registerCustomer() {
        Map<String, String> customerData = new HashMap<>();
        customerData.put("firstName", firstNameField.getText().trim());
        customerData.put("lastName", lastNameField.getText().trim());
        customerData.put("email", emailField.getText().trim());
        customerData.put("phone", phoneField.getText().trim());
        customerData.put("driversLicense", licenseField.getText().trim());
        customerData.put("address", addressArea.getText().trim());

        // Validate required fields
        if (customerData.get("firstName").isEmpty() ||
                customerData.get("lastName").isEmpty() ||
                customerData.get("email").isEmpty() ||
                customerData.get("driversLicense").isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields (First Name, Last Name, Email, Driver's License)",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        myAgent.registerCustomerFromGUI(customerData);
    }

    private void clearFields() {
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        licenseField.setText("");
        addressArea.setText("");
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusArea.append(status + "\n");
                // Auto-scroll to bottom
                statusArea.setCaretPosition(statusArea.getDocument().getLength());
            }
        });
    }
}