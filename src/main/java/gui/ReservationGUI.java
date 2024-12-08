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
    private String currentRole;
    private Integer currentCustomerId;

    public ReservationGUI(ReservationAgent agent, String role, Integer customerId) {
        super("Car Rental System - Reservations");
        this.myAgent = agent;
        this.currentRole = role;
        this.currentCustomerId = customerId;  // Make sure this is being set
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

        String[] columnNames = {
                "ID", "Vehicle", "Start Date", "End Date", "Status", "Total Cost"
        };

        // If admin, add Customer ID column
        if ("ADMIN".equals(currentRole)) {
            columnNames = new String[]{"ID", "Customer ID", "Vehicle", "Start Date", "End Date", "Status", "Total Cost"};
        }

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reservationsTable = new JTable(tableModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservationsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        reservationsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        reservationsTable.setRowHeight(30);

        // Configure column widths
        TableColumnModel columnModel = reservationsTable.getColumnModel();

        if ("ADMIN".equals(currentRole)) {
            columnModel.getColumn(0).setPreferredWidth(50);  // ID
            columnModel.getColumn(1).setPreferredWidth(80);  // Customer ID
            columnModel.getColumn(2).setPreferredWidth(200); // Vehicle
            columnModel.getColumn(3).setPreferredWidth(100); // Start Date
            columnModel.getColumn(4).setPreferredWidth(100); // End Date
            columnModel.getColumn(5).setPreferredWidth(80);  // Status
            columnModel.getColumn(6).setPreferredWidth(100); // Total Cost
        } else {
            columnModel.getColumn(0).setPreferredWidth(50);  // ID
            columnModel.getColumn(1).setPreferredWidth(200); // Vehicle
            columnModel.getColumn(2).setPreferredWidth(100); // Start Date
            columnModel.getColumn(3).setPreferredWidth(100); // End Date
            columnModel.getColumn(4).setPreferredWidth(80);  // Status
            columnModel.getColumn(5).setPreferredWidth(100); // Total Cost
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // Only show admin-specific buttons for admin role
        if ("ADMIN".equals(currentRole)) {
            JButton refreshAllButton = createStyledButton("Show All Reservations");
            refreshAllButton.addActionListener(e -> refreshAllReservations());

            JButton searchButton = createStyledButton("Search by Customer");
            searchButton.addActionListener(e -> refreshCustomerReservations());

            buttonPanel.add(refreshAllButton);
            buttonPanel.add(searchButton);
        } else {
            // Add refresh button for normal users
            JButton refreshButton = createStyledButton("Refresh");
            refreshButton.addActionListener(e -> {
                tableModel.setRowCount(0);
                List<Map<String, Object>> reservations = myAgent.getCustomerReservations(currentCustomerId);
                populateReservationsTable(reservations);
                updateStatus("Reservations refreshed");
            });
            buttonPanel.add(refreshButton);
        }

        payButton = createStyledButton("Pay");
        payButton.addActionListener(e -> initiatePayment());
        payButton.setEnabled(false);

        cancelButton = createStyledButton("Cancel Selected");
        cancelButton.addActionListener(e -> cancelSelectedReservation());

        buttonPanel.add(payButton);
        buttonPanel.add(cancelButton);

        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data based on role
        if ("ADMIN".equals(currentRole)) {
            refreshAllReservations();
        } else {
            // For normal users, only show their reservations
            tableModel.setRowCount(0);
            List<Map<String, Object>> reservations = myAgent.getCustomerReservations(currentCustomerId);
            populateReservationsTable(reservations);
        }

        // Add selection listener for managing pay button
        reservationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = reservationsTable.getSelectedRow();
                if (selectedRow != -1) {
                    // Get status column index based on role
                    int statusColumnIndex = "ADMIN".equals(currentRole) ? 5 : 4;
                    String status = (String) tableModel.getValueAt(selectedRow, statusColumnIndex);
                    // Enable pay button only for PENDING reservations
                    payButton.setEnabled("PENDING".equals(status));
                } else {
                    payButton.setEnabled(false);
                }
            }
        });

        return panel;
    }

    private void refreshAllReservations() {
        tableModel.setRowCount(0);
        List<Map<String, Object>> reservations = myAgent.getAllReservations();
        populateReservationsTable(reservations);
        updateStatus("All reservations loaded");
    }

    private void refreshCustomerReservations() {
        // Ensure we have a valid customer ID
        if (!"ADMIN".equals(currentRole) && currentCustomerId == null) {
            updateStatus("Error: No customer ID available");
            return;
        }

        int customerId;
        if ("ADMIN".equals(currentRole)) {
            // For admin, get customer ID from the text field
            if (customerIdField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a customer ID to search",
                        "Input Required",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                customerId = Integer.parseInt(customerIdField.getText().trim());
            } catch (NumberFormatException e) {
                updateStatus("Please enter a valid customer ID");
                return;
            }
        } else {
            // For normal users, use their own customer ID
            customerId = currentCustomerId;
        }

        tableModel.setRowCount(0);
        List<Map<String, Object>> reservations = myAgent.getCustomerReservations(customerId);
        populateReservationsTable(reservations);
        updateStatus("Loaded reservations for customer ID: " + customerId);
    }

    private void populateReservationsTable(List<Map<String, Object>> reservations) {
        for (Map<String, Object> reservation : reservations) {
            Object[] row;
            if ("ADMIN".equals(currentRole)) {
                row = new Object[]{
                        reservation.get("reservationId"),
                        reservation.get("customerId"),
                        reservation.get("vehicle"),
                        reservation.get("startDate"),
                        reservation.get("endDate"),
                        reservation.get("status"),
                        String.format("$%.2f", reservation.get("totalCost"))
                };
            } else {
                row = new Object[]{
                        reservation.get("reservationId"),
                        reservation.get("vehicle"),
                        reservation.get("startDate"),
                        reservation.get("endDate"),
                        reservation.get("status"),
                        String.format("$%.2f", reservation.get("totalCost"))
                };
            }
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

        // Only show Customer ID field for admin role
        if ("ADMIN".equals(currentRole)) {
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel customerLabel = createStyledLabel("Customer ID:");
            formPanel.add(customerLabel, gbc);
            gbc.gridx = 1;
            customerIdField = createStyledTextField();
            formPanel.add(customerIdField, gbc);
        }

        // Vehicle Selection
        gbc.gridx = 0;
        gbc.gridy = "ADMIN".equals(currentRole) ? 1 : 0;  // Adjust gridy based on role
        JLabel vehicleLabel = createStyledLabel("Vehicle:");
        formPanel.add(vehicleLabel, gbc);
        gbc.gridx = 1;
        vehicleComboBox = new JComboBox<>();
        vehicleComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        vehicleComboBox.setPreferredSize(new Dimension(300, 30));
        vehicleComboBox.addActionListener(e -> updateTotalCost());
        formPanel.add(vehicleComboBox, gbc);

        // Rest of the form components with adjusted gridy values
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

        // Use current customer ID for normal users, or text field for admin
        String customerId = "ADMIN".equals(currentRole) ?
                customerIdField.getText().trim() :
                currentCustomerId.toString();

        reservationData.put("customerId", customerId);
        reservationData.put("vehicleId", String.valueOf(selectedVehicle.getId()));
        reservationData.put("startDate", startDateField.getText().trim());
        reservationData.put("endDate", endDateField.getText().trim());
        reservationData.put("totalCost", totalCostField.getText().trim().replace("$", ""));

        if (myAgent.createReservation(reservationData, currentRole, currentCustomerId)) {
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
        // Only clear customerIdField if admin role
        if ("ADMIN".equals(currentRole) && customerIdField != null) {
            customerIdField.setText("");
        }
        startDateField.setText("");
        endDateField.setText("");
        totalCostField.setText("");
        if (vehicleComboBox.getItemCount() > 0) {
            vehicleComboBox.setSelectedIndex(0);
        }
        reservationsTable.clearSelection();
    }

    private boolean validateForm() {
        // Validate required fields based on role
        if (("ADMIN".equals(currentRole) && customerIdField.getText().trim().isEmpty()) ||
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
            // Validate customer ID only for admin
            if ("ADMIN".equals(currentRole)) {
                Integer.parseInt(customerIdField.getText().trim());
            }

            // Validate dates for all users
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

            // Refresh reservations based on user role
            if ("ADMIN".equals(currentRole)) {
                refreshAllReservations();
            } else if (currentCustomerId != null) {
                // For normal users, only show their reservations
                tableModel.setRowCount(0);
                List<Map<String, Object>> reservations = myAgent.getCustomerReservations(currentCustomerId);
                populateReservationsTable(reservations);
                updateStatus("Loaded reservations for user ID: " + currentCustomerId);
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
