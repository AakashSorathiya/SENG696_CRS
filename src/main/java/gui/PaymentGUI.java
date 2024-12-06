package gui;

import agents.PaymentAgent;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class PaymentGUI extends JFrame {
    private PaymentAgent myAgent;
    private JSplitPane splitPane;
    private JTextField reservationIdField;
    private JTextField amountField;
    private JComboBox<String> paymentMethodComboBox;
    private JTable paymentsTable;
    private DefaultTableModel tableModel;
    private JTextArea statusArea;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PaymentGUI(PaymentAgent agent) {
        super("Car Rental System - Payment Management");
        this.myAgent = agent;

        // Main panel with spacing
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for the "Home" button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton homeButton = new JButton("Home");
        homeButton.addActionListener(e -> goToHome());
        topPanel.add(homeButton);

        // Create left and right components
        JPanel leftComponent = createTablePanel();
        JPanel rightComponent = new JPanel(new BorderLayout(10, 10));
        rightComponent.add(createPaymentPanel(), BorderLayout.CENTER);
        rightComponent.add(createButtonPanel(), BorderLayout.SOUTH);

        // Set preferred sizes for both components
        leftComponent.setPreferredSize(new Dimension(400, 500));
        rightComponent.setPreferredSize(new Dimension(400, 500));

        // Create split pane with both components
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftComponent, rightComponent);
        splitPane.setResizeWeight(0.5); // Set initial division to 50-50
        splitPane.setContinuousLayout(true); // Enable continuous layout while dragging
        splitPane.setOneTouchExpandable(true); // Add one-touch expand/collapse buttons

        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        // Set up frame
        setContentPane(mainPanel);
        setupWindowBehavior();
        refreshPaymentTable();

        // Set minimum sizes for split pane components
        leftComponent.setMinimumSize(new Dimension(200, 200));
        rightComponent.setMinimumSize(new Dimension(200, 200));

        // Pack and display
        pack();
        centerOnScreen();

        // Set the divider location after packing
        SwingUtilities.invokeLater(() -> {
            int width = getWidth();
            splitPane.setDividerLocation(width / 2);
        });

        setVisible(true);
    }

    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Payment Processing"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Reservation ID
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Reservation ID:"), gbc);
        gbc.gridx = 1;
        reservationIdField = new JTextField(15);
        panel.add(reservationIdField, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Amount ($):"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField(15);
        panel.add(amountField, gbc);

        // Payment Method
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Payment Method:"), gbc);
        gbc.gridx = 1;
        String[] methods = {"CREDIT_CARD", "DEBIT_CARD", "CASH", "BANK_TRANSFER"};
        paymentMethodComboBox = new JComboBox<>(methods);
        panel.add(paymentMethodComboBox, gbc);

        // Add some padding
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        addButton(panel, "Process Payment", e -> processPayment());
        addButton(panel, "Process Refund", e -> processRefund());
        addButton(panel, "Refresh", e -> refreshPaymentTable());
        addButton(panel, "Clear Form", e -> clearForm());

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Payment History"));

        String[] columnNames = {
                "Payment ID", "Reservation ID", "Customer ID", "Amount",
                "Payment Date", "Method", "Status", "Transaction Ref"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentsTable = new JTable(tableModel);
        paymentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentsTable.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        // Configure the amount column to right-align and format as currency
        paymentsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            @Override
            public void setValue(Object value) {
                setText(value instanceof Double ? String.format("$%.2f", value) : "");
            }
        });

        JScrollPane scrollPane = new JScrollPane(paymentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add some padding
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status Log"));

        statusArea = new JTextArea(5, 40);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void goToHome() {
        this.setVisible(false);
        clearForm();
        myAgent.redirectToHome();
    }

    private void addButton(JPanel panel, String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    private void setupWindowBehavior() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });
        setResizable(true);
        setMinimumSize(new Dimension(900, 600));
    }

    private void processPayment() {
        try {
            Map<String, String> paymentData = new HashMap<>();
            paymentData.put("reservationId", reservationIdField.getText().trim());
            paymentData.put("amount", amountField.getText().trim());
            paymentData.put("paymentMethod", paymentMethodComboBox.getSelectedItem().toString());

            if (validatePaymentData(paymentData)) {
                if (myAgent.processPayment(paymentData)) {
                    updateStatus("Payment processed successfully");
                    clearForm();
                    refreshPaymentTable();
                } else {
                    updateStatus("Failed to process payment");
                }
            }
        } catch (Exception e) {
            updateStatus("Error processing payment: " + e.getMessage());
        }
    }

    private void processRefund() {
        int selectedRow = paymentsTable.getSelectedRow();
        if (selectedRow == -1) {
            updateStatus("Please select a payment to refund");
            return;
        }

        int paymentId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) tableModel.getValueAt(selectedRow, 6);

        if ("REFUNDED".equals(currentStatus)) {
            updateStatus("This payment has already been refunded");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to process this refund?",
                "Confirm Refund",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (myAgent.processRefund(paymentId)) {
                updateStatus("Refund processed successfully");
                refreshPaymentTable();
            } else {
                updateStatus("Failed to process refund");
            }
        }
    }

    private boolean validatePaymentData(Map<String, String> data) {
        if (data.get("reservationId").isEmpty() || data.get("amount").isEmpty()) {
            updateStatus("Please fill in all required fields");
            return false;
        }

        try {
            int reservationId = Integer.parseInt(data.get("reservationId"));
            double amount = Double.parseDouble(data.get("amount"));

            if (amount <= 0) {
                updateStatus("Amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            updateStatus("Please enter valid numbers for Reservation ID and Amount");
            return false;
        }

        return true;
    }

    private void fillFormFromSelection() {
        int selectedRow = paymentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            reservationIdField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            amountField.setText(tableModel.getValueAt(selectedRow, 3).toString().replace("$", ""));
            String method = (String) tableModel.getValueAt(selectedRow, 5);
            paymentMethodComboBox.setSelectedItem(method);
        }
    }

    private void clearForm() {
        reservationIdField.setText("");
        amountField.setText("");
        paymentMethodComboBox.setSelectedIndex(0);
        paymentsTable.clearSelection();
    }

    public void refreshPaymentTable() {
        tableModel.setRowCount(0);
        List<Map<String, Object>> payments = myAgent.getPaymentHistory();

        for (Map<String, Object> payment : payments) {
            Object[] row = {
                    payment.get("paymentId"),
                    payment.get("reservationId"),
                    payment.get("customerId"),
                    payment.get("amount"),
                    dateFormat.format(payment.get("paymentDate")),
                    payment.get("paymentMethod"),
                    payment.get("paymentStatus"),
                    payment.get("transactionReference")
            };
            tableModel.addRow(row);
        }
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = dateFormat.format(new Date());
            statusArea.append(timestamp + ": " + message + "\n");
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
