package agents;

import database.DatabaseConnection;
import gui.PaymentGUI;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.sql.*;
import java.util.*;

public class PaymentAgent extends Agent {
    private Connection dbConnection;
    private PaymentGUI gui;

    protected void setup() {
        setupDatabase();
        registerService();

        addBehaviour(new SimpleBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    showGUI();
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    try {
                        if (content.startsWith("PROCESS:")) {
                            Map<String, String> paymentData = parsePaymentData(content);
                            boolean success = processPayment(paymentData);
                            if (success) {
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("Payment processing successful:" + paymentData.get("reservationId"));
                            } else {
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("Payment processing failed");
                            }
                        } else if (content.startsWith("REFUND:")) {
                            int paymentId = Integer.parseInt(content.substring(7));
                            boolean success = processRefund(paymentId);
                            handleOperationResult(reply, success, "Refund processing");
                        }
                    } catch (Exception e) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Error: " + e.getMessage());
                        if (gui != null) {
                            gui.updateStatus("Error processing request: " + e.getMessage());
                        }
                    }

                    send(reply);
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        });
    }

    private void showGUI() {
        if (gui == null) {
            gui = new PaymentGUI(this);
        }
        gui.setVisible(true);
    }

    private void setupDatabase() {
        try {
            dbConnection = DatabaseConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            doDelete();
        }
    }

    public void redirectToHome() {
        // Create and send a message to the master agent
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("SHOW_HOME_GUI");

        // Find the master agent
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("master");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                msg.addReceiver(result[0].getName());
                send(msg);
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Dispose of the current GUI
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("payment");
        sd.setName("car-rental-payment");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public boolean processPayment(Map<String, String> paymentData) {
        String sql = "INSERT INTO payments (reservation_id, amount, payment_method, payment_status, transaction_reference) " +
                "VALUES (?, ?, ?, 'COMPLETED', ?)";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(paymentData.get("reservationId")));
            pstmt.setDouble(2, Double.parseDouble(paymentData.get("amount")));
            pstmt.setString(3, paymentData.get("paymentMethod"));
            pstmt.setString(4, generateTransactionReference());

            int result = pstmt.executeUpdate();
            if (result > 0) {
                updateReservationStatus(Integer.parseInt(paymentData.get("reservationId")), "CONFIRMED");
                if (gui != null) {
                    gui.updateStatus("Payment processed successfully for reservation " + paymentData.get("reservationId"));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Payment processing failed: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean processRefund(int paymentId) {
        // First check if the payment is eligible for refund
        String checkSql = "SELECT payment_status, reservation_id FROM payments WHERE payment_id = ?";

        try (PreparedStatement checkStmt = dbConnection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, paymentId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("payment_status");
                if (!"COMPLETED".equals(status)) {
                    if (gui != null) {
                        gui.updateStatus("Cannot refund payment: payment must be in COMPLETED status");
                    }
                    return false;
                }

                int reservationId = rs.getInt("reservation_id");

                // If payment is eligible, proceed with refund
                String updateSql = "UPDATE payments SET payment_status = 'REFUNDED' WHERE payment_id = ?";
                try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, paymentId);

                    int result = updateStmt.executeUpdate();
                    if (result > 0) {
                        // Update reservation status to CANCELLED
                        updateReservationStatus(reservationId, "CANCELLED");
                        if (gui != null) {
                            gui.updateStatus("Refund processed successfully for payment ID " + paymentId);
                        }
                        return true;
                    }
                }
            }

            if (gui != null) {
                gui.updateStatus("Failed to process refund for payment ID " + paymentId);
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Error processing refund: " + e.getMessage());
            }
            return false;
        }
    }

    public List<Map<String, Object>> getPaymentHistory() {
        List<Map<String, Object>> payments = new ArrayList<>();
        String sql = "SELECT p.*, r.customer_id, r.status as reservation_status, " +
                "c.first_name, c.last_name, " +
                "v.make, v.model, v.year " +
                "FROM payments p " +
                "JOIN reservations r ON p.reservation_id = r.reservation_id " +
                "JOIN customers c ON r.customer_id = c.customer_id " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                "ORDER BY p.payment_date DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> payment = new HashMap<>();
                payment.put("paymentId", rs.getInt("payment_id"));
                payment.put("reservationId", rs.getInt("reservation_id"));
                payment.put("customerName", rs.getString("first_name") + " " + rs.getString("last_name"));
                payment.put("vehicleInfo", String.format("%d %s %s",
                        rs.getInt("year"), rs.getString("make"), rs.getString("model")));
                payment.put("amount", rs.getDouble("amount"));
                payment.put("paymentDate", rs.getTimestamp("payment_date"));
                payment.put("paymentMethod", rs.getString("payment_method"));
                payment.put("paymentStatus", rs.getString("payment_status"));
                payment.put("transactionReference", rs.getString("transaction_reference"));
                payment.put("reservationStatus", rs.getString("reservation_status"));
                payments.add(payment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Error fetching payment history: " + e.getMessage());
            }
        }

        return payments;
    }

    private void updateReservationStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Error updating reservation status: " + e.getMessage());
            }
        }
    }

    // Modify the handleMessage method to handle SHOW_PAYMENT_GUI
    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        ACLMessage reply = msg.createReply();

        try {
            if ("SHOW_PAYMENT_GUI".equals(content)) {
                showGUI();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Payment GUI displayed");
            } else if (content.startsWith("PROCESS:")) {
                // ... existing process payment handling
            } else if (content.startsWith("REFUND:")) {
                // ... existing refund handling
            }
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("Error: " + e.getMessage());
        }

        send(reply);
    }

    private int getReservationIdFromPayment(int paymentId) {
        String sql = "SELECT reservation_id FROM payments WHERE payment_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, paymentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("reservation_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Error retrieving reservation ID: " + e.getMessage());
            }
        }
        return -1;
    }

    public boolean updatePayment(Map<String, String> paymentData) {
        String sql = "UPDATE payments SET payment_method = ?, payment_status = 'COMPLETED', " +
                "transaction_reference = ? WHERE payment_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, paymentData.get("paymentMethod"));
            pstmt.setString(2, generateTransactionReference());
            pstmt.setInt(3, Integer.parseInt(paymentData.get("paymentId")));

            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Update reservation status to CONFIRMED
                updateReservationStatus(Integer.parseInt(paymentData.get("reservationId")), "CONFIRMED");
                if (gui != null) {
                    gui.updateStatus("Payment " + paymentData.get("paymentId") + " processed successfully");
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            if (gui != null) {
                gui.updateStatus("Payment processing failed: " + e.getMessage());
            }
            return false;
        }
    }

    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis();
    }

    private Map<String, String> parsePaymentData(String content) {
        Map<String, String> data = new HashMap<>();
        String[] pairs = content.substring(8).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                data.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return data;
    }

    private void handleOperationResult(ACLMessage reply, boolean success, String operation) {
        if (success) {
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(operation + " successful");
        } else {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent(operation + " failed");
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        if (gui != null) {
            gui.dispose();
        }

        System.out.println("Payment Agent " + getAID().getName() + " terminating.");
    }
}