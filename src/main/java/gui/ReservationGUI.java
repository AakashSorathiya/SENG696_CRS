package gui;

// ReservationGUI.java
import agents.ReservationAgent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationGUI extends JFrame {
    private ReservationAgent myAgent;

    private JTextField customerIdField;
    private JComboBox<String> vehicleComboBox;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField totalCostField;
    private JTable reservationsTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;
    private Map<String, Integer> vehicleIdMap;

    public ReservationGUI(ReservationAgent agent) {
        super("Car Rental System - Reservation Agent");
        this.myAgent = agent;
        vehicleIdMap = new HashMap<>();

        // Main panel with spacing
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = createFormPanel();

        // Buttons panel
        JPanel buttonPanel = createButtonPanel();

        // Reservations table panel
        JPanel tablePanel = createTablePanel();

        // Status panel
        JPanel statusPanel = createStatusPanel();

        // Add all panels to main panel
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        setContentPane(mainPanel);

        // Window closing behavior
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(true);
        setMinimumSize(new Dimension(800, 600));
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Reservation Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Customer ID
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Customer ID:"), gbc);
        gbc.gridx = 1;
        customerIdField = new JTextField(10);
        formPanel.add(customerIdField, gbc);

        // Vehicle Selection
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Vehicle:"), gbc);
        gbc.gridx = 1;
        vehicleComboBox = new JComboBox<>();
        updateVehicleList();
        formPanel.add(vehicleComboBox, gbc);

        // Start Date
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Start Date (YYYY-MM-DD HH:mm):"), gbc);
        gbc.gridx = 1;
        startDateField = new JTextField(16);
        formPanel.add(startDateField, gbc);

        // End Date
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("End Date (YYYY-MM-DD HH:mm):"), gbc);
        gbc.gridx = 1;
        endDateField = new JTextField(16);
        formPanel.add(endDateField, gbc);

        // Total Cost
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Total Cost:"), gbc);
        gbc.gridx = 1;
        totalCostField = new JTextField(10);
        formPanel.add(totalCostField, gbc);

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Create Reservation Button
        JButton createButton = new JButton("Create Reservation");
        createButton.addActionListener(e -> createReservation());
        buttonPanel.add(createButton);

        // Cancel Reservation Button
        JButton cancelButton = new JButton("Cancel Reservation");
        cancelButton.addActionListener(e -> cancelSelectedReservation());
        buttonPanel.add(cancelButton);

        // Refresh Button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        buttonPanel.add(refreshButton);

        // Clear Button
        JButton clearButton = new JButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Current Reservations"));

        // Create table model with columns
        String[] columnNames = {"ID", "Vehicle", "Start Date", "End Date", "Status", "Cost"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table and add to scroll pane
        reservationsTable = new JTable(tableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(reservationsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);
        statusPanel.add(scrollPane, BorderLayout.CENTER);

        return statusPanel;
    }

    public void display() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        setVisible(true);
    }

    private void updateVehicleList() {
        vehicleComboBox.removeAllItems();
        vehicleIdMap.clear();

        List<Map<String, Object>> vehicles = myAgent.getAvailableVehicles();
        for (Map<String, Object> vehicle : vehicles) {
            String displayText = String.format("%s %s %d ($%.2f/day)",
                    vehicle.get("make"),
                    vehicle.get("model"),
                    vehicle.get("year"),
                    vehicle.get("dailyRate"));
            vehicleComboBox.addItem(displayText);
            vehicleIdMap.put(displayText, (Integer)vehicle.get("vehicleId"));
        }
    }

    private void createReservation() {
        try {
            // Validate inputs
            if (customerIdField.getText().trim().isEmpty() ||
                    startDateField.getText().trim().isEmpty() ||
                    endDateField.getText().trim().isEmpty() ||
                    totalCostField.getText().trim().isEmpty()) {
                updateStatus("Please fill in all fields");
                return;
            }

            Map<String, String> reservationData = new HashMap<>();
            reservationData.put("customerId", customerIdField.getText().trim());
            reservationData.put("vehicleId",
                    vehicleIdMap.get(vehicleComboBox.getSelectedItem().toString()).toString());
            reservationData.put("startDate", startDateField.getText().trim());
            reservationData.put("endDate", endDateField.getText().trim());
            reservationData.put("totalCost", totalCostField.getText().trim());

            boolean success = myAgent.createReservation(reservationData);
            if (success) {
                updateStatus("Reservation created successfully");
                clearForm();
                refreshData();
            } else {
                updateStatus("Failed to create reservation");
            }
        } catch (Exception e) {
            updateStatus("Error creating reservation: " + e.getMessage());
        }
    }

    private void cancelSelectedReservation() {
        int selectedRow = reservationsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int reservationId = (Integer)tableModel.getValueAt(selectedRow, 0);
            if (myAgent.cancelReservation(reservationId)) {
                updateStatus("Reservation cancelled successfully");
                refreshData();
            } else {
                updateStatus("Failed to cancel reservation");
            }
        } else {
            updateStatus("Please select a reservation to cancel");
        }
    }

    private void refreshData() {
        updateVehicleList();

        // Clear existing table data
        tableModel.setRowCount(0);

        // Only refresh if customer ID is provided
        if (!customerIdField.getText().trim().isEmpty()) {
            try {
                int customerId = Integer.parseInt(customerIdField.getText().trim());
                List<Map<String, Object>> reservations = myAgent.getCustomerReservations(customerId);

                for (Map<String, Object> reservation : reservations) {
                    Object[] row = {
                            reservation.get("reservationId"),
                            reservation.get("vehicle"),
                            reservation.get("startDate"),
                            reservation.get("endDate"),
                            reservation.get("status"),
                            String.format("$%.2f", reservation.get("totalCost"))
                    };
                    tableModel.addRow(row);
                }
            } catch (NumberFormatException e) {
                updateStatus("Please enter a valid customer ID");
            }
        }
    }

    private void clearForm() {
        customerIdField.setText("");
        startDateField.setText("");
        endDateField.setText("");
        totalCostField.setText("");
        if (vehicleComboBox.getItemCount() > 0) {
            vehicleComboBox.setSelectedIndex(0);
        }
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
}
