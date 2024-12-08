package agents;

import database.DatabaseConnection;
import gui.RegistrationGUI;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.*;
import java.sql.*;
import java.util.*;

public class RegistrationAgent extends Agent {
    private Connection dbConnection;
    private RegistrationGUI gui;
    private String userRole;
    private Integer customerId;

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

        // Register agent services
        registerService();

        // Add behavior to handle registration requests
        addBehaviour(new SimpleBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    // Parse user info from message
                    String[] parts = content.split(";");
                    String userInfo = parts[0];

                    // Extract role and customer ID
                    String[] userParts = userInfo.split(",");
                    String role = userParts[0].split(":")[1];
                    Integer custId = null;
                    if (userParts.length > 1) {
                        custId = Integer.parseInt(userParts[1].split(":")[1]);
                    }

                    showGUI(role, custId);
                    handleMessage(msg);
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

    private void showGUI(String role, Integer custId) {
        if (gui == null) {
            SwingUtilities.invokeLater(() -> {
                gui = new RegistrationGUI(this, role, custId);
                gui.setVisible(true);
            });
        }
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

    public Map<String, Object> getCustomerDetails(int customerId) {
        Map<String, Object> customerData = new HashMap<>();
        String sql = "SELECT * FROM customers WHERE customer_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                customerData.put("id", rs.getInt("customer_id"));
                customerData.put("firstName", rs.getString("first_name"));
                customerData.put("lastName", rs.getString("last_name"));
                customerData.put("email", rs.getString("email"));
                customerData.put("phone", rs.getString("phone"));
                customerData.put("driversLicense", rs.getString("drivers_license"));
                customerData.put("address", rs.getString("address"));
                customerData.put("status", rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customerData;
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

    public boolean updateCustomer(Map<String, String> customerData) {
        String sql;
        if (customerData.containsKey("customerId")) {
            // Update by customer ID for user's own profile
            sql = "UPDATE customers SET first_name = ?, last_name = ?, phone = ?, address = ? " +
                    "WHERE customer_id = ?";
        } else {
            // Update by email for admin updates
            sql = "UPDATE customers SET first_name = ?, last_name = ?, phone = ?, address = ? " +
                    "WHERE email = ?";
        }

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, customerData.get("firstName"));
            pstmt.setString(2, customerData.get("lastName"));
            pstmt.setString(3, customerData.get("phone"));
            pstmt.setString(4, customerData.get("address"));

            if (customerData.containsKey("customerId")) {
                pstmt.setInt(5, Integer.parseInt(customerData.get("customerId")));
            } else {
                pstmt.setString(5, customerData.get("email"));
            }

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

    public void redirectToHome() {
        // Create and send a message to the master agent
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
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

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        ACLMessage reply = msg.createReply();

        try {
            // Parse the complete message content
            String[] parts = content.split(";");
            String userInfo = parts[0];
            String command = parts.length > 1 ? parts[1] : "";

            // Handle different commands
            if (command.startsWith("REGISTER:")) {
                Map<String, String> customerData = parseCustomerData(command);
                boolean success = registerCustomer(customerData);
                handleOperationResult(reply, success, "Customer registration");
            }
            else if (command.startsWith("UPDATE:")) {
                Map<String, String> customerData = parseCustomerData(command);
                boolean success = updateCustomer(customerData);
                handleOperationResult(reply, success, "Customer update");
            }
            else if (command.startsWith("DEREGISTER:")) {
                String email = command.substring(11);
                boolean success = deregisterCustomer(email);
                handleOperationResult(reply, success, "Customer deregistration");
            }
            else if (command.equals("GET_ALL_CUSTOMERS")) {
                List<Map<String, Object>> customers = getAllCustomers();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Customers retrieved: " + customers.size());
            }
            else if (command.startsWith("GET_CUSTOMER:")) {
                int custId = Integer.parseInt(command.substring(13));
                Map<String, Object> customerData = getCustomerDetails(custId);
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Customer details retrieved");
            }
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("Error: " + e.getMessage());
        }

        send(reply);
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
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (gui != null) {
            gui.dispose();
        }

        System.out.println("Registration Agent " + getAID().getName() + " terminating.");
    }
}