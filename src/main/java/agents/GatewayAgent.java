package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import gui.GatewayGUI;
import database.DatabaseConnection;
import models.Transaction;
import models.PaymentGateway;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class GatewayAgent extends Agent {
    private GatewayGUI gui;
    private Connection dbConnection;
    private static final String ENCRYPTION_KEY = "YourEncryptionKey"; // In production, use secure key management

    @Override
    protected void setup() {
        try {
            dbConnection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        gui = new GatewayGUI(this);
        gui.setVisible(true);

        // Add behaviors
        addBehaviour(new PaymentRequestHandler());
        addBehaviour(new GatewayStatusMonitor());
    }

    // Behavior to handle payment requests
    private class PaymentRequestHandler extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (content.startsWith("PROCESS_PAYMENT")) {
                    handlePaymentProcess(content, reply);
                } else if (content.startsWith("REFUND_PAYMENT")) {
                    handleRefundProcess(content, reply);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }

        private void handlePaymentProcess(String content, ACLMessage reply) {
            try {
                String[] parts = content.split(":");
                Transaction transaction = new Transaction();
                transaction.setAmount(Double.parseDouble(parts[1]));
                transaction.setGatewayId(parts[2]);

                boolean success = processPaymentThroughGateway(transaction);
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "PAYMENT_SUCCESS:" + transaction.getTransactionId()
                        : "PAYMENT_FAILED");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ERROR: " + e.getMessage());
            }
        }

        private void handleRefundProcess(String content, ACLMessage reply) {
            try {
                String[] parts = content.split(":");
                String transactionId = parts[1];
                boolean success = processRefundThroughGateway(transactionId);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "REFUND_SUCCESS" : "REFUND_FAILED");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ERROR: " + e.getMessage());
            }
        }
    }

    // Behavior to monitor gateway status
    private class GatewayStatusMonitor extends CyclicBehaviour {
        @Override
        public void action() {
            try {
                List<PaymentGateway> gateways = getAllGateways();
                for (PaymentGateway gateway : gateways) {
                    boolean isActive = checkGatewayStatus(gateway);
                    updateGatewayStatus(gateway.getId(), isActive);
                }
                gui.updateGatewayStatus();
                Thread.sleep(30000); // Check every 30 seconds
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Payment Gateway Operations
    private boolean processPaymentThroughGateway(Transaction transaction) {
        try {
            // In a real implementation, this would communicate with the actual payment gateway
            String encryptedData = encryptPaymentData(transaction);
            // Simulate gateway communication
            boolean gatewayResponse = simulateGatewayResponse();

            if (gatewayResponse) {
                transaction.setTransactionId(generateTransactionId());
                saveTransaction(transaction);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean processRefundThroughGateway(String transactionId) {
        try {
            // Retrieve original transaction
            Transaction originalTransaction = getTransaction(transactionId);
            if (originalTransaction == null) return false;

            // In a real implementation, this would communicate with the actual payment gateway
            boolean gatewayResponse = simulateGatewayResponse();

            if (gatewayResponse) {
                updateTransactionStatus(transactionId, "REFUNDED");
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper Methods
    private String encryptPaymentData(Transaction transaction) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            String data = transaction.toString();
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean simulateGatewayResponse() {
        // Simulate gateway response with 95% success rate
        return Math.random() > 0.05;
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    private boolean checkGatewayStatus(PaymentGateway gateway) {
        // In real implementation, would check actual gateway status
        return simulateGatewayResponse();
    }

    // Database Operations
    private void saveTransaction(Transaction transaction) {
        try {
            String query = "INSERT INTO transactions (transaction_id, amount, gateway_id, status) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, transaction.getTransactionId());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getGatewayId());
            stmt.setString(4, "COMPLETED");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Transaction getTransaction(String transactionId) {
        try {
            String query = "SELECT * FROM transactions WHERE transaction_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, transactionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getString("transaction_id"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setGatewayId(rs.getString("gateway_id"));
                transaction.setStatus(rs.getString("status"));
                return transaction;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateTransactionStatus(String transactionId, String status) {
        try {
            String query = "UPDATE transactions SET status = ? WHERE transaction_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, status);
            stmt.setString(2, transactionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<PaymentGateway> getAllGateways() {
        List<PaymentGateway> gateways = new ArrayList<>();
        try {
            String query = "SELECT * FROM payment_gateways";
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                PaymentGateway gateway = new PaymentGateway();
                gateway.setId(rs.getString("gateway_id"));
                gateway.setName(rs.getString("name"));
                gateway.setStatus(rs.getString("status"));
                gateways.add(gateway);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gateways;
    }

    private void updateGatewayStatus(String gatewayId, boolean isActive) {
        try {
            String query = "UPDATE payment_gateways SET status = ? WHERE gateway_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, isActive ? "ACTIVE" : "INACTIVE");
            stmt.setString(2, gatewayId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logGatewayEvent(String gatewayId, String eventType, String eventData) {
        try {
            String query = "INSERT INTO gateway_logs (gateway_id, event_type, event_data) VALUES (?, ?, ?)";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, gatewayId);
            stmt.setString(2, eventType);
            stmt.setString(3, eventData);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            String query = "SELECT * FROM transactions ORDER BY created_at DESC LIMIT ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getString("transaction_id"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setGatewayId(rs.getString("gateway_id"));
                transaction.setStatus(rs.getString("status"));
                transaction.setTimestamp(rs.getTimestamp("created_at").toString());
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public List<String> getGatewayLogs(String gatewayId, int limit) {
        List<String> logs = new ArrayList<>();
        try {
            String query = "SELECT * FROM gateway_logs WHERE gateway_id = ? ORDER BY created_at DESC LIMIT ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, gatewayId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String logEntry = String.format("[%s] %s: %s",
                        rs.getTimestamp("created_at"),
                        rs.getString("event_type"),
                        rs.getString("event_data")
                );
                logs.add(logEntry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    protected void takeDown() {
        try {
            if (dbConnection != null) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        gui.dispose();
    }
}