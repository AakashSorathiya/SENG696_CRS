package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import agents.GatewayAgent;
import models.PaymentGateway;
import models.Transaction;

public class GatewayGUI extends JFrame {
    private GatewayAgent agent;
    private JPanel mainPanel;
    private JTable gatewayTable;
    private JTable transactionTable;
    private DefaultTableModel gatewayTableModel;
    private DefaultTableModel transactionTableModel;

    public GatewayGUI(GatewayAgent agent) {
        this.agent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Payment Gateway Management");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Create components
        setupMenuBar();
        setupGatewayPanel();
        setupTransactionPanel();
        setupStatusBar();

        add(mainPanel);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshData());
        fileMenu.add(refreshItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void setupGatewayPanel() {
        JPanel gatewayPanel = new JPanel(new BorderLayout());
        gatewayPanel.setBorder(BorderFactory.createTitledBorder("Payment Gateways"));

        String[] columns = {"Gateway ID", "Name", "Status", "Last Check"};
        gatewayTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        gatewayTable = new JTable(gatewayTableModel);
        JScrollPane scrollPane = new JScrollPane(gatewayTable);
        gatewayPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(gatewayPanel, BorderLayout.NORTH);
    }

    private void setupTransactionPanel() {
        JPanel transactionPanel = new JPanel(new BorderLayout());
        transactionPanel.setBorder(BorderFactory.createTitledBorder("Recent Transactions"));

        String[] columns = {"Transaction ID", "Amount", "Gateway", "Status", "Timestamp"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(transactionTableModel);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        transactionPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(transactionPanel, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.add(new JLabel("System Status: Active"));
        mainPanel.add(statusBar, BorderLayout.SOUTH);
    }

    public void updateGatewayStatus() {
        // Update gateway status in the table
        refreshData();
    }

    private void refreshData() {
        // Refresh both tables
        updateGatewayTable();
        updateTransactionTable();
    }

    // Enhanced Gateway GUI methods
    public void updateGatewayTable() {
        gatewayTableModel.setRowCount(0);
        List<PaymentGateway> gateways = getAllGateways();

        for (PaymentGateway gateway : gateways) {
            Object[] row = {
                    gateway.getId(),
                    gateway.getName(),
                    gateway.getStatus(),
                    gateway.getLastCheck()
            };
            gatewayTableModel.addRow(row);
        }
    }

    public void updateTransactionTable() {
        transactionTableModel.setRowCount(0);
        List<Transaction> transactions = getRecentTransactions(50);  // Show last 50 transactions

        for (Transaction transaction : transactions) {
            Object[] row = {
                    transaction.getTransactionId(),
                    String.format("%.2f", transaction.getAmount()),
                    transaction.getGatewayId(),
                    transaction.getStatus(),
                    transaction.getTimestamp()
            };
            transactionTableModel.addRow(row);
        }
    }

    // Add monitoring panel to GatewayGUI
    private void setupMonitoringPanel() {
        JPanel monitoringPanel = new JPanel(new BorderLayout());
        monitoringPanel.setBorder(BorderFactory.createTitledBorder("Gateway Monitoring"));

        // Create log area
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // Create refresh button
        JButton refreshButton = new JButton("Refresh Logs");
        refreshButton.addActionListener(e -> {
            String selectedGatewayId = getSelectedGatewayId();
            if (selectedGatewayId != null) {
                List<String> logs = agent.getGatewayLogs(selectedGatewayId, 100);
                logArea.setText("");
                for (String log : logs) {
                    logArea.append(log + "\n");
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);

        monitoringPanel.add(logScrollPane, BorderLayout.CENTER);
        monitoringPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(monitoringPanel, BorderLayout.EAST);
    }

    private String getSelectedGatewayId() {
        int selectedRow = gatewayTable.getSelectedRow();
        if (selectedRow >= 0) {
            return (String) gatewayTable.getValueAt(selectedRow, 0);
        }
        return null;
    }
}