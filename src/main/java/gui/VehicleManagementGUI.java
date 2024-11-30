package gui;

// VehicleManagementGUI.java
import agents.VehicleManagementAgent;

import javax.swing.*;
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

    public VehicleManagementGUI(VehicleManagementAgent agent) {
        super("Car Rental System - Vehicle Management");
        this.myAgent = agent;

        // Main panel with spacing
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create and add all panels
        mainPanel.add(createFormPanel(), BorderLayout.NORTH);
        mainPanel.add(createButtonPanel(), BorderLayout.CENTER);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        // Set up frame
        setContentPane(mainPanel);
        setupWindowBehavior();
        refreshVehicleTable();
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Create form fields
        createFormFields(formPanel, gbc);

        return formPanel;
    }

    private void createFormFields(JPanel panel, GridBagConstraints gbc) {
        // Row 1: Make and Model
        addFormField(panel, gbc, 0, 0, "Make:", makeField = new JTextField(15));
        addFormField(panel, gbc, 2, 0, "Model:", modelField = new JTextField(15));

        // Row 2: Year and Color
        addFormField(panel, gbc, 0, 1, "Year:", yearField = new JTextField(15));
        addFormField(panel, gbc, 2, 1, "Color:", colorField = new JTextField(15));

        // Row 3: License Plate and Daily Rate
        addFormField(panel, gbc, 0, 2, "License Plate:", licensePlateField = new JTextField(15));
        addFormField(panel, gbc, 2, 2, "Daily Rate:", dailyRateField = new JTextField(15));

        // Row 4: Status
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        String[] statuses = {"AVAILABLE", "RESERVED", "RENTED", "MAINTENANCE"};
        statusComboBox = new JComboBox<>(statuses);
        panel.add(statusComboBox, gbc);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int x, int y, String label, JTextField field) {
        gbc.gridx = x; gbc.gridy = y;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = x + 1;
        panel.add(field, gbc);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Create and add buttons
        addButton(buttonPanel, "Add Vehicle", e -> addVehicle());
        addButton(buttonPanel, "Update Vehicle", e -> updateSelectedVehicle());
        addButton(buttonPanel, "Set Maintenance", e -> setMaintenanceStatus());
        addButton(buttonPanel, "Refresh", e -> refreshVehicleTable());
        addButton(buttonPanel, "Clear Form", e -> clearForm());

        return buttonPanel;
    }

    private void addButton(JPanel panel, String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Vehicle Inventory"));

        // Create table model
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

        // Create and configure table
        vehiclesTable = new JTable(tableModel);
        vehiclesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehiclesTable.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        // Add table to scrollpane
        JScrollPane scrollPane = new JScrollPane(vehiclesTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status Log"));

        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);
        statusPanel.add(scrollPane, BorderLayout.CENTER);

        return statusPanel;
    }

    private void setupWindowBehavior() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });
        setResizable(true);
        setMinimumSize(new Dimension(1000, 700));
    }

    // Action Methods
    private void addVehicle() {
        try {
            Map<String, String> vehicleData = getFormData();
            if (validateFormData(vehicleData)) {
                if (myAgent.addVehicle(vehicleData)) {
                    updateStatus("Vehicle added successfully");
                    clearForm();
                    refreshVehicleTable();
                } else {
                    updateStatus("Failed to add vehicle");
                }
            }
        } catch (Exception e) {
            updateStatus("Error adding vehicle: " + e.getMessage());
        }
    }

    private void updateSelectedVehicle() {
        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow == -1) {
            updateStatus("Please select a vehicle to update");
            return;
        }

        try {
            Map<String, String> vehicleData = getFormData();
            vehicleData.put("vehicleId", tableModel.getValueAt(selectedRow, 0).toString());

            if (validateFormData(vehicleData)) {
                if (myAgent.updateVehicle(vehicleData)) {
                    updateStatus("Vehicle updated successfully");
                    refreshVehicleTable();
                } else {
                    updateStatus("Failed to update vehicle");
                }
            }
        } catch (Exception e) {
            updateStatus("Error updating vehicle: " + e.getMessage());
        }
    }

    private void setMaintenanceStatus() {
        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow == -1) {
            updateStatus("Please select a vehicle to set maintenance status");
            return;
        }

        int vehicleId = (Integer)tableModel.getValueAt(selectedRow, 0);
        if (myAgent.setVehicleMaintenanceStatus(vehicleId)) {
            updateStatus("Maintenance status set successfully");
            refreshVehicleTable();
        } else {
            updateStatus("Failed to set maintenance status");
        }
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

    private boolean validateFormData(Map<String, String> data) {
        if (data.values().stream().anyMatch(String::isEmpty)) {
            updateStatus("Please fill in all fields");
            return false;
        }

        try {
            Integer.parseInt(data.get("year"));
            Double.parseDouble(data.get("dailyRate"));
        } catch (NumberFormatException e) {
            updateStatus("Please enter valid numbers for Year and Daily Rate");
            return false;
        }

        return true;
    }

    private void fillFormFromSelection() {
        int selectedRow = vehiclesTable.getSelectedRow();
        if (selectedRow >= 0) {
            makeField.setText((String)tableModel.getValueAt(selectedRow, 1));
            modelField.setText((String)tableModel.getValueAt(selectedRow, 2));
            yearField.setText(tableModel.getValueAt(selectedRow, 3).toString());
            colorField.setText((String)tableModel.getValueAt(selectedRow, 4));
            licensePlateField.setText((String)tableModel.getValueAt(selectedRow, 5));
            statusComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 6));
            dailyRateField.setText(tableModel.getValueAt(selectedRow, 7).toString());
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
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    public void display() {
        pack();
        centerOnScreen();
        setVisible(true);
    }

    private void centerOnScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
                (screenSize.width - getWidth()) / 2,
                (screenSize.height - getHeight()) / 2
        );
    }
}
