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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


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
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private JButton payButton;

    public ReservationGUI(ReservationAgent agent) {
        super("Car Rental System - Reservations");
        this.myAgent = agent;
        setupGUI();
        refreshData();
    }

    private void setupGUI() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton homeButton = new JButton("Home");
        homeButton.setPreferredSize(new Dimension(100, 30));
        homeButton.setFont(new Font("Arial", Font.PLAIN, 14));
        homeButton.addActionListener(e -> goToHome());
        topPanel.add(homeButton);
        add(topPanel, BorderLayout.NORTH);

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

        reservationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleReservationSelection();
            }
        });

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


    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Current Reservations"));

        // Create table model
        String[] columnNames = {
                "ID", "Customer ID", "Vehicle", "Start Date", "End Date", "Status", "Total Cost"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create and style table
        reservationsTable = new JTable(tableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservationsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        reservationsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        reservationsTable.setRowHeight(30);

        // Configure column widths
        TableColumnModel columnModel = reservationsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);  // ID
        columnModel.getColumn(1).setPreferredWidth(80);  // Customer ID
        columnModel.getColumn(2).setPreferredWidth(200); // Vehicle
        columnModel.getColumn(3).setPreferredWidth(100); // Start Date
        columnModel.getColumn(4).setPreferredWidth(100); // End Date
        columnModel.getColumn(5).setPreferredWidth(80);  // Status
        columnModel.getColumn(6).setPreferredWidth(100); // Total Cost

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // Add refresh button
        JButton refreshAllButton = createStyledButton("Show All Reservations");
        refreshAllButton.addActionListener(e -> refreshAllReservations());

        // Add customer search button
        JButton searchButton = createStyledButton("Search by Customer");
        searchButton.addActionListener(e -> refreshCustomerReservations());

        payButton = createStyledButton("Pay");
        payButton.addActionListener(e -> initiatePayment());
        payButton.setEnabled(false);  // Initially disabled until a reservation is selected
        buttonPanel.add(payButton);

        // Add cancel button
        cancelButton = createStyledButton("Cancel Selected");
        cancelButton.addActionListener(e -> cancelSelectedReservation());

        buttonPanel.add(refreshAllButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(payButton);

        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshAllReservations() {
        tableModel.setRowCount(0);
        List<Map<String, Object>> reservations = myAgent.getAllReservations();
        populateReservationsTable(reservations);
        updateStatus("All reservations loaded");
    }

    private void refreshCustomerReservations() {
        if (customerIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a customer ID to search",
                    "Input Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int customerId = Integer.parseInt(customerIdField.getText().trim());
            tableModel.setRowCount(0);
            List<Map<String, Object>> reservations = myAgent.getCustomerReservations(customerId);
            populateReservationsTable(reservations);
            updateStatus("Loaded reservations for customer ID: " + customerId);
        } catch (NumberFormatException e) {
            updateStatus("Please enter a valid customer ID");
        }
    }

    private void populateReservationsTable(List<Map<String, Object>> reservations) {
        for (Map<String, Object> reservation : reservations) {
            Object[] row = {
                    reservation.get("reservationId"),
                    reservation.get("customerId"),    // Added Customer ID column
                    reservation.get("vehicle"),
                    reservation.get("startDate"),
                    reservation.get("endDate"),
                    reservation.get("status"),
                    String.format("$%.2f", reservation.get("totalCost"))
            };
            tableModel.addRow(row);
        }
    }

    private JPanel createReservationPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("New Reservation"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Customer ID
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel customerLabel = createStyledLabel("Customer ID:");
        formPanel.add(customerLabel, gbc);
        gbc.gridx = 1;
        customerIdField = createStyledTextField();
        formPanel.add(customerIdField, gbc);

        // Vehicle Selection
        gbc.gridx = 0; gbc.gridy++;
        JLabel vehicleLabel = createStyledLabel("Vehicle:");
        formPanel.add(vehicleLabel, gbc);
        gbc.gridx = 1;
        vehicleComboBox = new JComboBox<>();
        vehicleComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        vehicleComboBox.setPreferredSize(new Dimension(300, 30));
        vehicleComboBox.addActionListener(e -> updateTotalCost());
        formPanel.add(vehicleComboBox, gbc);

        // Start Date - Changed label to show date-only format
        gbc.gridx = 0; gbc.gridy++;
        JLabel startLabel = createStyledLabel("Start Date (yyyy-MM-dd):");
        formPanel.add(startLabel, gbc);
        gbc.gridx = 1;
        startDateField = createStyledTextField();
        startDateField.setToolTipText("Enter date in format: yyyy-MM-dd");
        startDateField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateTotalCost();
            }
        });
        formPanel.add(startDateField, gbc);

        // End Date - Changed label to show date-only format
        gbc.gridx = 0; gbc.gridy++;
        JLabel endLabel = createStyledLabel("End Date (yyyy-MM-dd):");
        formPanel.add(endLabel, gbc);
        gbc.gridx = 1;
        endDateField = createStyledTextField();
        endDateField.setToolTipText("Enter date in format: yyyy-MM-dd");
        endDateField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateTotalCost();
            }
        });
        formPanel.add(endDateField, gbc);

        // Total Cost
        gbc.gridx = 0; gbc.gridy++;
        JLabel costLabel = createStyledLabel("Total Cost ($):");
        formPanel.add(costLabel, gbc);
        gbc.gridx = 1;
        totalCostField = createStyledTextField();
        totalCostField.setEditable(false);
        formPanel.add(totalCostField, gbc);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        createButton = createStyledButton("Create Reservation");
        createButton.addActionListener(e -> createReservation());

        calculateButton = createStyledButton("Calculate Cost");
        calculateButton.addActionListener(e -> updateTotalCost());

        clearButton = createStyledButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());

//        payButton = createStyledButton("Pay");
//        payButton.addActionListener(e -> initiatePayment());
//        payButton.setEnabled(false);  // Initially disabled until a reservation is selected

        buttonPanel.add(createButton);
        buttonPanel.add(calculateButton);
        buttonPanel.add(clearButton);
//        buttonPanel.add(payButton);

        return buttonPanel;
    }

    private void handleReservationSelection() {
        int selectedRow = reservationsTable.getSelectedRow();
        if (selectedRow != -1) {
            String status = (String) tableModel.getValueAt(selectedRow, 5);
            // Enable pay button only for PENDING reservations
            payButton.setEnabled("PENDING".equals(status));
        } else {
            payButton.setEnabled(false);
        }
    }

    // Add this method to initiate payment
    private void initiatePayment() {
        int selectedRow = reservationsTable.getSelectedRow();
        if (selectedRow != -1) {
            int reservationId = (Integer) tableModel.getValueAt(selectedRow, 0);
            double totalCost = Double.parseDouble(((String) tableModel.getValueAt(selectedRow, 6))
                    .replace("$", "").trim());

            // Create pending payment record
            Map<String, String> paymentData = new HashMap<>();
            paymentData.put("reservationId", String.valueOf(reservationId));
            paymentData.put("amount", String.format("%.2f", totalCost));
            paymentData.put("status", "PENDING");

            if (myAgent.createPendingPayment(paymentData)) {
                updateStatus("Payment initiated for reservation " + reservationId);
                // Show payment GUI
                myAgent.showPaymentGUI();
                this.setVisible(false);
            } else {
                updateStatus("Failed to initiate payment");
            }
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(300, 30));
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        return field;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(150, 35));
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        return button;
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

    private void goToHome() {
        this.setVisible(false);
        clearForm();
        myAgent.redirectToHome();
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
            String startDateStr = startDateField.getText().trim();
            String endDateStr = endDateField.getText().trim();

            if (selectedVehicle == null || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                totalCostField.setText("");
                return;
            }

            LocalDate startDate = LocalDate.parse(startDateStr, dateFormatter);
            LocalDate endDate = LocalDate.parse(endDateStr, dateFormatter);

            // Calculate number of days between dates (inclusive)
            long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

            if (days < 1) {
                JOptionPane.showMessageDialog(this,
                        "End date must be after or equal to start date",
                        "Date Error",
                        JOptionPane.ERROR_MESSAGE);
                totalCostField.setText("");
                return;
            }

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
            LocalDate startDate = LocalDate.parse(startDateField.getText().trim(), dateFormatter);
            LocalDate endDate = LocalDate.parse(endDateField.getText().trim(), dateFormatter);

            if (endDate.isBefore(startDate)) {
                JOptionPane.showMessageDialog(this,
                        "End date must be after or equal to start date",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid date format (yyyy-MM-dd)",
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

        // Refresh all reservations
        refreshAllReservations();
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
