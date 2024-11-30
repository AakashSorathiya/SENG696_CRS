package agents;

// RegistrationAgent.java
import gui.RegistrationGUI;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class RegistrationAgent extends Agent {
    private Connection dbConnection;
    private RegistrationGUI gui;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/car_rental";
    private static final String USER = "root";
    private static final String PASS = "password";

    protected void setup() {
        // Initialize database connection
        setupDatabase();

        // Create and show GUI
        gui = new RegistrationGUI(this);
        gui.display();

        // Register the agent services
        registerService();

        // Add behavior to handle registration requests
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    if (content.startsWith("REGISTER:")) {
                        try {
                            Map<String, String> customerData = parseCustomerData(content);
                            boolean success = registerCustomer(customerData);

                            if (success) {
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("Registration successful");
                                gui.updateStatus("Registered new customer: " + customerData.get("email"));
                            } else {
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("Registration failed");
                            }
                        } catch (Exception e) {
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("Error: " + e.getMessage());
                        }
                    } else if (content.startsWith("DEREGISTER:")) {
                        String email = content.substring(11);
                        try {
                            boolean success = deregisterCustomer(email);
                            if (success) {
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent("Deregistration successful");
                                gui.updateStatus("Deregistered customer: " + email);
                            } else {
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("Deregistration failed");
                            }
                        } catch (Exception e) {
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("Error: " + e.getMessage());
                        }
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
        sd.setType("registration");
        sd.setName("car-rental-registration");
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

        System.out.println("Registration Agent " + getAID().getName() + " terminating.");
    }

    // Database operations
    public boolean registerCustomer(Map<String, String> customerData) {
        String sql = "INSERT INTO customers (first_name, last_name, email, phone, drivers_license, address) VALUES (?, ?, ?, ?, ?, ?)";

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

    private Map<String, String> parseCustomerData(String content) {
        // Format: "REGISTER:firstName=John,lastName=Doe,email=john@email.com,..."
        Map<String, String> data = new HashMap<>();
        String[] pairs = content.substring(9).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            data.put(keyValue[0], keyValue[1]);
        }
        return data;
    }

    // Methods that can be called from GUI
    public void registerCustomerFromGUI(Map<String, String> customerData) {
        if (registerCustomer(customerData)) {
            gui.updateStatus("Successfully registered: " + customerData.get("email"));
        } else {
            gui.updateStatus("Failed to register: " + customerData.get("email"));
        }
    }

    public void deregisterCustomerFromGUI(String email) {
        if (deregisterCustomer(email)) {
            gui.updateStatus("Successfully deregistered: " + email);
        } else {
            gui.updateStatus("Failed to deregister: " + email);
        }
    }
}