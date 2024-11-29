package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import gui.RegistrationGUI;
import database.DatabaseConnection;
import models.Customer;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegistrationAgent extends Agent {
    private RegistrationGUI gui;
    private Connection dbConnection;

    @Override
    protected void setup() {
        try {
            dbConnection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        gui = new RegistrationGUI(this);
        gui.setVisible(true);

        // Add behaviors
        addBehaviour(new RegistrationRequestHandler());
        addBehaviour(new DeregistrationHandler());
    }

    // Behavior to handle registration requests
    private class RegistrationRequestHandler extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (content.startsWith("REGISTER_CUSTOMER")) {
                    handleRegistration(content, reply);
                } else if (content.startsWith("UPDATE_CUSTOMER")) {
                    handleUpdateCustomer(content, reply);
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }

        private void handleRegistration(String content, ACLMessage reply) {
            try {
                // Parse customer details from content
                Customer customer = parseCustomerData(content);
                boolean success = registerCustomer(customer);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "REGISTRATION_SUCCESS" : "REGISTRATION_FAILED");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ERROR: " + e.getMessage());
            }
        }

        private void handleUpdateCustomer(String content, ACLMessage reply) {
            try {
                Customer customer = parseCustomerData(content);
                boolean success = updateCustomer(customer);

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "UPDATE_SUCCESS" : "UPDATE_FAILED");
            } catch (Exception e) {
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("ERROR: " + e.getMessage());
            }
        }
    }

    // Behavior to handle deregistration requests
    private class DeregistrationHandler extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null && msg.getContent().startsWith("DEREGISTER_CUSTOMER")) {
                String customerId = msg.getContent().split(":")[1];
                ACLMessage reply = msg.createReply();

                boolean success = deregisterCustomer(customerId);
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(success ? "DEREGISTRATION_SUCCESS" : "DEREGISTRATION_FAILED");

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    // Helper methods
    private Customer parseCustomerData(String content) {
        // Parse the content string and create a Customer object
        String[] parts = content.split(":");
        Customer customer = new Customer();
        // Set customer details from parts array
        return customer;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.US_ASCII));
            byte[] hexChars = new byte[hash.length * 2];
            for (int j = 0; j < hash.length; j++) {
                int v = hash[j] & 0xFF;
                hexChars[j * 2] = hash[v >>> 4];
                hexChars[j * 2 + 1] = hash[v & 0x0F];
            }
            return new String(hexChars, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Database operations
    public boolean registerCustomer(Customer customer) {
        try {
            // First check if customer already exists
            if (customerExists(customer.getEmail())) {
                return false;
            }

            String query = "INSERT INTO customers (name, email, phone, password_hash) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, hashPassword(customer.getPassword()));

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deregisterCustomer(String customerId) {
        try {
            String query = "UPDATE customers SET status = 'INACTIVE' WHERE customer_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, customerId);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCustomer(Customer customer) {
        try {
            String query = "UPDATE customers SET name = ?, phone = ? WHERE customer_id = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setInt(3, customer.getId());

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean customerExists(String email) {
        try {
            String query = "SELECT COUNT(*) FROM customers WHERE email = ?";
            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        try {
            String query = "SELECT * FROM customers ORDER BY name";
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Customer customer = new Customer();
                customer.setId(rs.getInt("customer_id"));
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setPhone(rs.getString("phone"));
                customer.setStatus(rs.getString("status"));
                customers.add(customer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
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