package gui;

import agents.ReservationAgent;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.awt.event.*;
import java.util.List;

public class ReservationGUI extends JFrame {
    private ReservationAgent myAgent;
    private JTextField customerIdField;
    private JComboBox<VehicleItem> vehicleComboBox;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField totalCostField;
    private JTable reservationsTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;
    private JButton createButton;
    private JButton cancelButton;
    private JButton calculateButton;
    private JButton clearButton;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReservationGUI(ReservationAgent agent) {
        super("Car Rental System - Reservations");
        this.myAgent = agent;
        setupGUI();
        refreshData();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // Left panel - Reservation Form
        splitPane.setLeftComponent(createReservationPanel());

        // Right panel - Reservations Table
        splitPane.setRightComponent(createReservationsPanel());

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

        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createReservationPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("New Reservation"));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Customer ID
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Customer ID:"), gbc);
        gbc.gridx = 1;
        customerIdField = new JTextField(15);
        formPanel.add(customerIdField, gbc);

        // Vehicle Selection
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Vehicle:"), gbc);
        gbc.gridx = 1;
        vehicleComboBox = new JComboBox<>();
        vehicleComboBox.addActionListener(e -> updateTotalCost());
        formPanel.add(vehicleComboBox, gbc);

        // Start Date
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Start Date (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1;
        startDateField = new JTextField(15);
        startDateField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateTotalCost();
            }
        });
        formPanel.add(startDateField, gbc);

        // End Date
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("End Date (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1;
        endDateField = new JTextField(15);
        endDateField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateTotalCost();
            }
        });
        formPanel.add(endDateField, gbc);

        // Total Cost
        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(new JLabel("Total Cost ($):"), gbc);
        gbc.gridx = 1;
        totalCostField = new JTextField(15);
        totalCostField.setEditable(false);
        formPanel.add(totalCostField, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        createButton = new JButton("Create Reservation");
        createButton.addActionListener(e -> createReservation());

        calculateButton = new JButton("Calculate Cost");
        calculateButton.addActionListener(e -> updateTotalCost());

        clearButton = new JButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());

        buttonPanel.add(createButton);
        buttonPanel.add(calculateButton);
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Current Reservations"));

        // Create table model
        String[] columnNames = {
                "ID", "Vehicle", "Start Date", "End Date", "Status", "Total Cost"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table
        reservationsTable = new JTable(tableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add cancel button panel
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel Selected");
        cancelButton.addActionListener(e -> cancelSelectedReservation());
        tableButtonPanel.add(cancelButton);

        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(tableButtonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusArea = new JTextArea(4, 50);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);

        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> statusArea.setText(""));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(clearLogButton, BorderLayout.EAST);

        return panel;
    }

    private void createReservation() {
        if (!validateForm()) return;

        Map<String, String> reservationData = new HashMap<>();
        VehicleItem selectedVehicle = (VehicleItem)vehicleComboBox.getSelectedItem();

        reservationData.put("customerId", customerIdField.getText().trim());
        reservationData.put("vehicleId", String.valueOf(selectedVehicle.getId()));
        reservationData.put("startDate", startDateField.getText().trim());
        reservationData.put("endDate", endDateField.getText().trim());
        reservationData.put("totalCost", totalCostField.getText().trim().replace("$", ""));

        if (myAgent.createReservation(reservationData)) {
            updateStatus("Reservation created successfully");
            clearForm();
            refreshData();
        } else {
            updateStatus("Failed to create reservation");
        }
    }

    private void cancelSelectedReservation() {
        int selectedRow = reservationsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a reservation to cancel",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int reservationId = (Integer)tableModel.getValueAt(selectedRow, 0);
        String status = (String)tableModel.getValueAt(selectedRow, 4);

        if ("CANCELLED".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This reservation is already cancelled",
                    "Cannot Cancel",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this reservation?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (myAgent.cancelReservation(reservationId)) {
                updateStatus("Reservation cancelled successfully");
                refreshData();
            } else {
                updateStatus("Failed to cancel reservation");
            }
        }
    }

    private void updateTotalCost() {
        try {
            VehicleItem selectedVehicle = (VehicleItem)vehicleComboBox.getSelectedItem();
            if (selectedVehicle == null ||
                    startDateField.getText().trim().isEmpty() ||
                    endDateField.getText().trim().isEmpty()) {
                return;
            }

            LocalDateTime startDate = LocalDateTime.parse(
                    startDateField.getText().trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );
            LocalDateTime endDate = LocalDateTime.parse(
                    endDateField.getText().trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            long days = java.time.Duration.between(startDate, endDate).toDays() + 1;
            double totalCost = days * selectedVehicle.getDailyRate();
            totalCostField.setText(String.format("%.2f", totalCost));
        } catch (Exception e) {
            totalCostField.setText("");
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
        reservationsTable.clearSelection();
    }

    private boolean validateForm() {
        if (customerIdField.getText().trim().isEmpty() ||
                startDateField.getText().trim().isEmpty() ||
                endDateField.getText().trim().isEmpty() ||
                totalCostField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Integer.parseInt(customerIdField.getText().trim());
            LocalDateTime.parse(startDateField.getText().trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime.parse(endDateField.getText().trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid date/time format (yyyy-MM-dd HH:mm)",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void refreshData() {
        // Refresh vehicle list
        vehicleComboBox.removeAllItems();
        List<Map<String, Object>> vehicles = myAgent.getAvailableVehicles();
        for (Map<String, Object> vehicle : vehicles) {
            vehicleComboBox.addItem(new VehicleItem(
                    (Integer)vehicle.get("vehicleId"),
                    String.format("%s %s %d ($%.2f/day)",
                            vehicle.get("make"),
                            vehicle.get("model"),
                            vehicle.get("year"),
                            vehicle.get("dailyRate")),
                    (Double)vehicle.get("dailyRate")
            ));
        }

        // Refresh reservations table
        tableModel.setRowCount(0);
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

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", new java.util.Date());
            statusArea.append(timestamp + message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    // Inner class to hold vehicle information
    private static class VehicleItem {
        private final int id;
        private final String description;
        private final double dailyRate;

        public VehicleItem(int id, String description, double dailyRate) {
            this.id = id;
            this.description = description;
            this.dailyRate = dailyRate;
        }

        public int getId() { return id; }
        public double getDailyRate() { return dailyRate; }

        @Override
        public String toString() {
            return description;
        }
    }
}
