package gui;

import agents.VehicleManagementAgent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class VehicleManagementGUI extends JFrame {
    private VehicleManagementAgent myAgent;

    private JTextField makeField;
    private JTextField modelField;
    private JTextField yearField;
    private JTextField colorField;
    private JTextField licensePlateField;
    private JTextField dailyRateField;
    private JComboBox<String> statusComboBox;
    private JTable vehiclesTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JSplitPane splitPane;

    public VehicleManagementGUI(VehicleManagementAgent agent) {
        super("Car Rental System - Vehicle Management");
        this.myAgent = agent;
        setupGUI();
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

        // Left panel - Vehicle Form
        JPanel leftPanel = createFormPanel();
        leftPanel.setPreferredSize(new Dimension(500, 600));
        splitPane.setLeftComponent(leftPanel);

        // Right panel - Vehicles Table
        JPanel rightPanel = createTablePanel();
        rightPanel.setPreferredSize(new Dimension(600, 600));
        splitPane.setRightComponent(rightPanel);

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

        setMinimumSize(new Dimension(1400, 800));

        // Pack and set divider
        pack();
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.4);
        });

        setLocationRelativeTo(null);
        refreshVehicleTable();
        setVisible(true);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Vehicle Details"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Form fields panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create and add form fields
        addLabelAndField(formPanel, gbc, 0, "Make:", makeField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 1, "Model:", modelField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 2, "Year:", yearField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 3, "Color:", colorField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 4, "License Plate:", licensePlateField = createStyledTextField());
        addLabelAndField(formPanel, gbc, 5, "Daily Rate:", dailyRateField = createStyledTextField());

        // Status ComboBox
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(createStyledLabel("Status:"), gbc);

        gbc.gridx = 1;
        String[] statuses = {"AVAILABLE", "RESERVED", "RENTED", "MAINTENANCE"};
        statusComboBox = new JComboBox<>(statuses);
        statusComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(statusComboBox, gbc);

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

        addButton = createStyledButton("Add Vehicle");
        updateButton = createStyledButton("Update Vehicle");
        deleteButton = createStyledButton("Delete Vehicle");
        clearButton = createStyledButton("Clear");

        // Add action listeners
        addButton.addActionListener(e -> addVehicle());
        updateButton.addActionListener(e -> updateVehicle());
        deleteButton.addActionListener(e -> deleteVehicle());
        clearButton.addActionListener(e -> clearForm());

        // Initially disable update and delete buttons
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 35));
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        return button;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Vehicle Inventory"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        String[] columnNames = {
                "ID", "Make", "Model", "Year", "Color", "License Plate",
                "Status", "Daily Rate", "Last Maintenance", "Next Maintenance"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        vehiclesTable = new JTable(tableModel);
        vehiclesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehiclesTable.setFont(new Font("Arial", Font.PLAIN, 14));
        vehiclesTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        vehiclesTable.setRowHeight(30);
        vehiclesTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());

        // Set column widths
        vehiclesTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        vehiclesTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Make
        vehiclesTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Model
        vehiclesTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // Year
        vehiclesTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Color
        vehiclesTable.getColumnModel().getColumn(5).setPreferredWidth(100); // License Plate
        vehiclesTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Status
        vehiclesTable.getColumnModel().getColumn(7).setPreferredWidth(80);  // Daily Rate
        vehiclesTable.getColumnModel().getColumn(8).setPreferredWidth(120); // Last Maintenance
        vehiclesTable.getColumnModel().getColumn(9).setPreferredWidth(120); // Next Maintenance

        JScrollPane scrollPane = new JScrollPane(vehiclesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> refreshVehicleTable());

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

    private void addVehicle() {
        if (!validateForm()) return;

        Map<String, String> vehicleData = getFormData();
        if (myAgent.addVehicle(vehicleData)) {
            updateStatus("Vehicle added successfully");
            clearForm();
            refreshVehicleTable();
        } else {
            updateStatus("Failed to add vehicle");
        }
    }

    private void updateVehicle() {
        if (!validateForm()) return;

        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow == -1) {
            updateStatus("Please select a vehicle to update");
            return;
        }

        // Get current vehicle status
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 6);
        String newStatus = statusComboBox.getSelectedItem().toString();

        // First, try to update the status if it has changed
        if (!currentStatus.equals(newStatus)) {
            int vehicleId = (Integer) tableModel.getValueAt(selectedRow, 0);
            if (!myAgent.updateVehicleStatus(vehicleId, newStatus)) {
                updateStatus("Failed to update vehicle status");
                return;
            }
        }

        // Then update other vehicle details
        Map<String, String> vehicleData = getFormData();
        vehicleData.put("vehicleId", tableModel.getValueAt(selectedRow, 0).toString());

        if (myAgent.updateVehicle(vehicleData)) {
            updateStatus("Vehicle updated successfully");
            clearForm();
            refreshVehicleTable();
        } else {
            updateStatus("Failed to update vehicle");
        }
    }

    private void deleteVehicle() {
        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a vehicle to delete",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int vehicleId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 6);

        if ("RENTED".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete a rented vehicle",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this vehicle?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (myAgent.deleteVehicle(vehicleId)) {
                updateStatus("Vehicle deleted successfully");
                clearForm();
                refreshVehicleTable();
            } else {
                updateStatus("Failed to delete vehicle");
            }
        }
    }

    private void handleTableSelection() {
        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow >= 0) {
            makeField.setText((String) tableModel.getValueAt(selectedRow, 1));
            modelField.setText((String) tableModel.getValueAt(selectedRow, 2));
            yearField.setText(tableModel.getValueAt(selectedRow, 3).toString());
            colorField.setText((String) tableModel.getValueAt(selectedRow, 4));
            licensePlateField.setText((String) tableModel.getValueAt(selectedRow, 5));
            statusComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 6));
            dailyRateField.setText(tableModel.getValueAt(selectedRow, 7).toString());

            updateButton.setEnabled(true);
            deleteButton.setEnabled(true);

            String status = (String) tableModel.getValueAt(selectedRow, 6);
            deleteButton.setEnabled(!"RENTED".equals(status));
        }
    }

    private void clearForm() {
        makeField.setText("");
        modelField.setText("");
        yearField.setText("");
        colorField.setText("");
        licensePlateField.setText("");
        dailyRateField.setText("");
        statusComboBox.setSelectedIndex(0);
        vehiclesTable.clearSelection();

        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private boolean validateForm() {
        if (makeField.getText().trim().isEmpty() ||
                modelField.getText().trim().isEmpty() ||
                yearField.getText().trim().isEmpty() ||
                colorField.getText().trim().isEmpty() ||
                licensePlateField.getText().trim().isEmpty() ||
                dailyRateField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Integer.parseInt(yearField.getText().trim());
            Double.parseDouble(dailyRateField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for Year and Daily Rate",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private Map<String, String> getFormData() {
        Map<String, String> data = new HashMap<>();
        data.put("make", makeField.getText().trim());
        data.put("model", modelField.getText().trim());
        data.put("year", yearField.getText().trim());
        data.put("color", colorField.getText().trim());
        data.put("licensePlate", licensePlateField.getText().trim());
        data.put("dailyRate", dailyRateField.getText().trim());
        data.put("status", statusComboBox.getSelectedItem().toString());
        return data;
    }

    public void refreshVehicleTable() {
        tableModel.setRowCount(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        List<Map<String, Object>> vehicles = myAgent.getAllVehicles();
        for (Map<String, Object> vehicle : vehicles) {
            Object[] row = {
                    vehicle.get("vehicleId"),
                    vehicle.get("make"),
                    vehicle.get("model"),
                    vehicle.get("year"),
                    vehicle.get("color"),
                    vehicle.get("licensePlate"),
                    vehicle.get("status"),
                    vehicle.get("dailyRate"),
                    vehicle.get("lastMaintenance") != null ?
                            dateFormat.format(vehicle.get("lastMaintenance")) : "",
                    vehicle.get("nextMaintenance") != null ?
                            dateFormat.format(vehicle.get("nextMaintenance")) : ""
            };
            tableModel.addRow(row);
        }

        // Adjust column widths after data is loaded
        if (vehicles.size() > 0) {
            adjustColumnWidths();
        }
    }

    private void adjustColumnWidths() {
        for (int i = 0; i < vehiclesTable.getColumnCount(); i++) {
            int maxWidth = 0;
            for (int j = 0; j < vehiclesTable.getRowCount(); j++) {
                Object value = vehiclesTable.getValueAt(j, i);
                String stringValue = value != null ? value.toString() : "";
                int cellWidth = getFontMetrics(vehiclesTable.getFont())
                        .stringWidth(stringValue) + 20; // Add padding
                maxWidth = Math.max(maxWidth, cellWidth);
            }
            // Set a maximum width to prevent extremely wide columns
            maxWidth = Math.min(maxWidth, 200);
            vehiclesTable.getColumnModel().getColumn(i).setPreferredWidth(maxWidth);
        }
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = String.format("[%tT] ", new java.util.Date());
            statusArea.append(timestamp + message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    public void display() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}