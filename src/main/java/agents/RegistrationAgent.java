package agents;

import database.DatabaseConnection;
import gui.RegistrationGUI;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.SwingUtilities;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class RegistrationAgent extends Agent {
    private Connection dbConnection;
    private RegistrationGUI gui;

    protected void setup() {
        System.out.println("Registration Agent " + getLocalName() + " starting...");

        // Initialize database connection
        try {
            dbConnection = DatabaseConnection.getConnection();
            System.out.println("Database connection established successfully");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            doDelete();
            return;
        }

        // Initialize GUI on EDT
        SwingUtilities.invokeLater(() -> {
            gui = new RegistrationGUI(this);
        });

        // Register agent services
        registerService();

        // Add behavior to handle registration requests
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("registration");
        sd.setName("car-rental-registration");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        ACLMessage reply = msg.createReply();

        try {
            if (content.startsWith("REGISTER:")) {
                Map<String, String> customerData = parseCustomerData(content);
                boolean success = registerCustomer(customerData);
                handleOperationResult(reply, success, "Customer registration");
            } else if (content.startsWith("DEREGISTER:")) {
                String email = content.substring(11);
                boolean success = deregisterCustomer(email);
                handleOperationResult(reply, success, "Customer deregistration");
            } else if (content.startsWith("UPDATE:")) {
                Map<String, String> customerData = parseCustomerData(content);
                boolean success = updateCustomer(customerData);
                handleOperationResult(reply, success, "Customer update");
            } else if (content.equals("GET_ALL_CUSTOMERS")) {
                List<Map<String, Object>> customers = getAllCustomers();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Customers retrieved: " + customers.size());
            }
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("Error: " + e.getMessage());
        }

        send(reply);
    }

    public boolean registerCustomer(Map<String, String> customerData) {
        String sql = "INSERT INTO customers (first_name, last_name, email, phone, drivers_license, address, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, customerData.get("firstName"));
            pstmt.setString(2, customerData.get("lastName"));
            pstmt.setString(3, customerData.get("email"));
            pstmt.setString(4, customerData.get("phone"));
            pstmt.setString(5, customerData.get("driversLicense"));
            pstmt.setString(6, customerData.get("address"));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deregisterCustomer(String email) {
        String sql = "UPDATE customers SET status = 'INACTIVE' WHERE email = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateCustomer(Map<String, String> customerData) {
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, phone = ?, address = ? WHERE email = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, customerData.get("firstName"));
            pstmt.setString(2, customerData.get("lastName"));
            pstmt.setString(3, customerData.get("phone"));
            pstmt.setString(4, customerData.get("address"));
            pstmt.setString(5, customerData.get("email"));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getAllCustomers() {
        List<Map<String, Object>> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE status = 'ACTIVE'";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("id", rs.getInt("customer_id"));
                customer.put("firstName", rs.getString("first_name"));
                customer.put("lastName", rs.getString("last_name"));
                customer.put("email", rs.getString("email"));
                customer.put("phone", rs.getString("phone"));
                customer.put("driversLicense", rs.getString("drivers_license"));
                customer.put("address", rs.getString("address"));
                customer.put("status", rs.getString("status"));
                customers.add(customer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    private Map<String, String> parseCustomerData(String content) {
        Map<String, String> data = new HashMap<>();
        String[] pairs = content.substring(content.indexOf(":") + 1).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                data.put(keyValue[0], keyValue[1]);
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
        // Deregister from DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Close database connection
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Close GUI
        if (gui != null) {
            gui.dispose();
        }

        System.out.println("Registration Agent " + getAID().getName() + " terminating.");
    }
}