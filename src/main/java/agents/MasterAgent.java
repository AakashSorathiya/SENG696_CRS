package agents;

import database.DatabaseConnection;
import gui.LoginGUI;
import gui.MasterGUI;
import gui.RegistrationGUI;
import gui.ReservationGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MasterAgent extends Agent {
    private MasterGUI gui;
    private static int currentState = 0;
    private Map<String, Object> currentUser; // Store logged in user info

    protected void setup() {
        System.out.println("Master Agent " + getLocalName() + " starting...");

        // Show login GUI first instead of main GUI
        SwingUtilities.invokeLater(() -> {
            showLoginGUI();
        });

        // Register master agent services
        registerService();

        // Add behavior to handle messages
        addBehaviour(new SimpleBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
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

        System.out.println("Master Agent setup completed");
    }

    private void showLoginGUI() {
        new LoginGUI(this);
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("master");
        sd.setName("car-rental-master");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void handleLogout() {
        // Clear the current user
        currentUser = null;

        // Close the main GUI if it exists
        if (gui != null) {
            gui.dispose();
            gui = null;
        }

        // Show login GUI again
        SwingUtilities.invokeLater(() -> {
            showLoginGUI();
        });
    }

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        switch (msg.getPerformative()) {
            case ACLMessage.INFORM:
                if (content.equals("SHOW_HOME_GUI")) {
                    SwingUtilities.invokeLater(() -> {
                        gui = new MasterGUI(this, currentUser);
                        gui.setVisible(true);
                    });
                } else {
                    gui.updateLog("Success: " + content);
                }
                break;
            case ACLMessage.FAILURE:
                gui.updateLog("Error: " + content);
                break;
            default:
                gui.updateLog("Received message: " + content);
        }
    }

    public boolean handleLogin(String username, String password) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT u.*, c.customer_id FROM users u " +
                    "LEFT JOIN customers c ON u.customer_id = c.customer_id " +
                    "WHERE u.username = ? AND u.password = ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In production, use proper password hashing

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUser = new HashMap<>();
                currentUser.put("userId", rs.getInt("user_id"));
                currentUser.put("username", rs.getString("username"));
                currentUser.put("role", rs.getString("role"));
                currentUser.put("customerId", rs.getInt("customer_id"));

                // Show main GUI
                SwingUtilities.invokeLater(() -> {
                    gui = new MasterGUI(this, currentUser);
                    gui.setVisible(true);
                });
                return true;  // Login successful
            } else {
                JOptionPane.showMessageDialog(null,
                        "Invalid username or password",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                return false;  // Login failed
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database error occurred",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;  // Login failed due to error
        }
    }

    public boolean registerNewUser(Map<String, String> userData) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);  // Start transaction

            // First check if username or email already exists
            String checkSql = "SELECT username, email FROM users u " +
                    "LEFT JOIN customers c ON u.customer_id = c.customer_id " +
                    "WHERE username = ? OR email = ?";
            pstmt = conn.prepareStatement(checkSql);
            pstmt.setString(1, userData.get("username"));
            pstmt.setString(2, userData.get("email"));
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String existingUsername = rs.getString("username");
                String existingEmail = rs.getString("email");

                if (existingUsername != null && existingUsername.equals(userData.get("username"))) {
                    JOptionPane.showMessageDialog(null,
                            "Username already exists",
                            "Registration Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (existingEmail != null && existingEmail.equals(userData.get("email"))) {
                    JOptionPane.showMessageDialog(null,
                            "Email already registered",
                            "Registration Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Insert customer first
            String customerSql = "INSERT INTO customers (first_name, last_name, email, phone, " +
                    "drivers_license, address, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
            pstmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, userData.get("firstName"));
            pstmt.setString(2, userData.get("lastName"));
            pstmt.setString(3, userData.get("email"));
            pstmt.setString(4, userData.get("phone"));
            pstmt.setString(5, userData.get("driversLicense"));
            pstmt.setString(6, userData.get("address"));
            pstmt.executeUpdate();

            // Get the generated customer ID
            rs = pstmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Failed to get customer ID");
            }
            int customerId = rs.getInt(1);

            // Insert user
            String userSql = "INSERT INTO users (username, password, customer_id, role) " +
                    "VALUES (?, ?, ?, 'USER')";
            pstmt = conn.prepareStatement(userSql);
            pstmt.setString(1, userData.get("username"));
            pstmt.setString(2, userData.get("password")); // In production, use password hashing
            pstmt.setInt(3, customerId);
            pstmt.executeUpdate();

            conn.commit();  // Commit transaction
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error during registration: " + e.getMessage(),
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;

        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Modified to include user info in request
    public void processRequest(int choice) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        String content = "";
        AID receiver = null;

        // Always include user info in content
        String userInfo = "USER:" + currentUser.get("role") +
                ",ID:" + currentUser.get("customerId");

        switch (choice) {
            case 1:
                // For registration/profile, send the user info with REGISTRATION_REQUEST
                content = userInfo + ";REGISTRATION_REQUEST";  // Add the command part
                receiver = new AID("reg", AID.ISLOCALNAME);
                break;
            case 2:
                content = userInfo + ";RESERVATION_REQUEST";
                receiver = new AID("res", AID.ISLOCALNAME);
                break;
            case 3:
                content = userInfo + ";VEHICLE_REQUEST";
                receiver = new AID("veh", AID.ISLOCALNAME);
                break;
            case 4:
                content = userInfo + ";PAYMENT_REQUEST";
                receiver = new AID("pay", AID.ISLOCALNAME);
                break;
        }

        if (receiver != null && !content.isEmpty()) {
            msg.setContent(content);
            msg.addReceiver(receiver);
            send(msg);
            gui.updateLog("Sent request to " + receiver.getLocalName());
        }
    }

    public Map<String, Object> getCurrentUser() {
        return currentUser;
    }
}