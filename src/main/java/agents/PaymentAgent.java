package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import gui.PaymentGUI;
import database.DatabaseConnection;
import models.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class PaymentAgent extends Agent {
    private PaymentGUI gui;
    private Connection dbConnection;

    @Override
    protected void setup() {
        try {
            dbConnection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        gui = new PaymentGUI(this);
        gui.setVisible(true);

        // Add behaviors
        addBehaviour(new PaymentRequestsServer());
        addBehaviour(new RefundRequestHandler());
    }

    // Behavior to handle payment requests
    private class PaymentRequestsServer extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (content.startsWith("PROCESS_PAYMENT")) {
                    handlePaymentProcess(content, reply);
                } else if (content.startsWith("VERIFY_PAYMENT")) {
                    handlePaymentVerification(content, reply);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }

        private void handlePaymentProcess(String content, ACLMessage reply) {
            try {
                String[] parts = content.split(":");
                Payment payment = new Payment();
                payment.setAmount(Double.parseDouble(parts[1]));
                payment.setBookingId(Integer.parseInt(parts[2]));

                boolean success = processPayment(payment);
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ERROR: " + e.getMessage());
            }
        }
    }

    // Behavior to handle refund requests
    private class RefundRequestHandler extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null && msg.getContent().startsWith("PROCESS_REFUND")) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();

                try {
                    String[] parts = content.split(":");
                    int paymentId = Integer.parseInt(parts[1]);
                    boolean success = processRefund(paymentId);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(success ? "REFUND_SUCCESS" : "REFUND_FAILED");
                } catch (Exception e) {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("ERROR: " + e.getMessage());
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    // Database operations
    public boolean processPayment(Payment payment) {
        try {
            String query = "INSERT INTO payments (booking_id, amount, payment_date, status) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setInt(1, payment.getBookingId());
            stmt.setDouble(2, payment.getAmount());
            stmt.setTimestamp(3, new Timestamp(new Date().getTime()));
            stmt.setString(4, "COMPLETED");

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean processRefund(int paymentId) {
        try {
            String query = "UPDATE payments SET status = 'REFUNDED', refund_date = ? WHERE payment_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setTimestamp(1, new Timestamp(new Date().getTime()));
            stmt.setInt(2, paymentId);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Payment> getAllPayments() {
        List<Payment> payments = new ArrayList<>();
        try {
            String query = "SELECT * FROM payments ORDER BY payment_date DESC";
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("payment_id"));
                payment.setBookingId(rs.getInt("booking_id"));
                payment.setAmount(rs.getDouble("amount"));
                payment.setPaymentDate(rs.getTimestamp("payment_date"));
                payment.setStatus(rs.getString("status"));
                payment.setRefundDate(rs.getTimestamp("refund_date"));
                payments.add(payment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
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