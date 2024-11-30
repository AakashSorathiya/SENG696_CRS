package agents;

// PaymentAgent.java
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
import java.time.LocalDateTime;

public class PaymentAgent extends Agent {
    private Connection dbConnection;
    private PaymentGUI gui;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/car_rental";
    private static final String USER = "root";
    private static final String PASS = "password";

    protected void setup() {
        // Initialize database connection
        setupDatabase();

        // Create and show GUI
        gui = new PaymentGUI(this);
        gui.display();

        // Register the agent services
        registerService();

        // Add behavior to handle payment requests
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    try {
                        if (content.startsWith("PROCESS:")) {
                            Map<String, String> paymentData = parsePaymentData(content);
                            boolean success = processPayment(paymentData);
                            handleOperationResult(reply, success, "Payment processing");
                        } else if (content.startsWith("REFUND:")) {
                            int paymentId = Integer.parseInt(content.substring(7));
                            boolean success = processRefund(paymentId);
                            handleOperationResult(reply, success, "Refund processing");
                        }
                    } catch (Exception e) {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Error: " + e.getMessage());
                    }

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private void setupDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace();
            doDelete();
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

    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Close database connection
        try {
            if (dbConnection != null) {
                dbConnection.close();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        // Close GUI
        gui.dispose();

        System.out.println("Payment Agent " + getAID().getName() + " terminating.");
    }

    // Database operations
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
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean processRefund(int paymentId) {
        String sql = "UPDATE payments SET payment_status = 'REFUNDED' WHERE payment_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, paymentId);

            // Get reservation ID before updating payment
            int reservationId = getReservationIdFromPayment(paymentId);
            int result = pstmt.executeUpdate();

            if (result > 0 && reservationId != -1) {
                updateReservationStatus(reservationId, "CANCELLED");
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateReservationStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, reservationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        }
        return -1;
    }

    public List<Map<String, Object>> getPaymentHistory() {
        List<Map<String, Object>> payments = new ArrayList<>();
        String sql = "SELECT p.*, r.customer_id, r.status as reservation_status " +
                "FROM payments p " +
                "JOIN reservations r ON p.reservation_id = r.reservation_id " +
                "ORDER BY p.payment_date DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> payment = new HashMap<>();
                payment.put("paymentId", rs.getInt("payment_id"));
                payment.put("reservationId", rs.getInt("reservation_id"));
                payment.put("customerId", rs.getInt("customer_id"));
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
        }

        return payments;
    }

    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis();
    }

    private Map<String, String> parsePaymentData(String content) {
        Map<String, String> data = new HashMap<>();
        String[] pairs = content.substring(8).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            data.put(keyValue[0], keyValue[1]);
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
}
