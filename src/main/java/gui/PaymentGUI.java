package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.text.DecimalFormat;
import agents.PaymentAgent;
import models.Payment;

public class PaymentGUI extends JFrame {
    private PaymentAgent agent;
    private JPanel mainPanel;
    private JTable paymentTable;
    private DefaultTableModel tableModel;
    private DecimalFormat currencyFormat;

    public PaymentGUI(PaymentAgent agent) {
        this.agent = agent;
        this.currencyFormat = new DecimalFormat("#,##0.00");
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Payment Management");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Create components
        setupMenuBar();
        setupPaymentForm();
        setupPaymentTable();
        setupStatusBar();

        // Refresh payment list
        refreshPaymentList();

        add(mainPanel);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshPaymentList());
        fileMenu.add(refreshItem);

        JMenuItem exportItem = new JMenuItem("Export Report");
        exportItem.addActionListener(e -> exportPaymentReport());
        fileMenu.add(exportItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void setupPaymentForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Process Payment"));
        GridBagConstraints gbc = new GridBagConstraints();

        // Booking ID
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Booking ID:"), gbc);

        gbc.gridx = 1;
        JTextField bookingIdField = new JTextField(10);
        formPanel.add(bookingIdField, gbc);

        // Amount
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Amount:"), gbc);

        gbc.gridx = 1;
        JTextField amountField = new JTextField(10);
        formPanel.add(amountField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton processButton = new JButton("Process Payment");
        processButton.addActionListener(e -> {
            try {
                int bookingId = Integer.parseInt(bookingIdField.getText());
                double amount = Double.parseDouble(amountField.getText());

                Payment payment = new Payment();
                payment.setBookingId(bookingId);
                payment.setAmount(amount);

                boolean success = agent.processPayment(payment);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Payment processed successfully");
                    refreshPaymentList();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Payment processing failed",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input format",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(processButton);

        JButton refundButton = new JButton("Process Refund");
        refundButton.addActionListener(e -> processSelectedRefund());
        buttonPanel.add(refundButton);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);
    }

    private void setupPaymentTable() {
        String[] columns = {"ID", "Booking ID", "Amount", "Date", "Status", "Refund Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(paymentTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.add(new JLabel("Ready"));
        mainPanel.add(statusBar, BorderLayout.SOUTH);
    }

    private void processSelectedRefund() {
        int selectedRow = paymentTable.getSelectedRow();
        if (selectedRow >= 0) {
            int paymentId = (int) paymentTable.getValueAt(selectedRow, 0);
            String status = (String) paymentTable.getValueAt(selectedRow, 4);

            if (!"COMPLETED".equals(status)) {
                JOptionPane.showMessageDialog(this,
                        "Only completed payments can be refunded",
                        "Invalid Operation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to process this refund?",
                    "Confirm Refund",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = agent.processRefund(paymentId);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Refund processed successfully");
                    refreshPaymentList();
                } else {
                    JOptionPane.showMessageDialog(this, "Refund processing failed",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a payment to refund");
        }
    }

    private void clearForm() {
        // Clear all form fields
    }

    private void exportPaymentReport() {
        // Implementation for exporting payment report
    }

    public void refreshPaymentList() {
        tableModel.setRowCount(0);
        List<Payment> payments = agent.getAllPayments();

        for (Payment payment : payments) {
            Object[] row = {
                    payment.getId(),
                    payment.getBookingId(),
                    currencyFormat.format(payment.getAmount()),
                    payment.getPaymentDate(),
                    payment.getStatus(),
                    payment.getRefundDate()
            };
            tableModel.addRow(row);
        }
    }
}