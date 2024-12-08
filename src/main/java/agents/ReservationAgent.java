package agents;

import database.DatabaseConnection;
import gui.RegistrationGUI;
import gui.ReservationGUI;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.SwingUtilities;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReservationAgent extends Agent {
    private Connection dbConnection;
    private ReservationGUI gui;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String currentRole;
    private Integer currentCustomerId;

    protected void setup() {
        System.out.println("Reservation Agent " + getLocalName() + " starting...");

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

        // Add behavior to handle messages
        addBehaviour(new SimpleBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    if (content != null && !content.isEmpty()) {
                        try {
                            // Extract role and customer ID from message
                            String[] parts = content.split(";");
                            String userInfo = parts[0];
                            String[] userParts = userInfo.split(",");
                            currentRole = userParts[0].split(":")[1];
                            if (userParts.length > 1) {
                                currentCustomerId = Integer.parseInt(userParts[1].split(":")[1]);
                            } else {
                                currentCustomerId = null;
                            }

                            showGUI();
                            handleMessage(msg);
                        } catch (Exception e) {
                            System.out.println("Error processing message: " + e.getMessage());
                        }
                    }
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
            SwingUtilities.invokeLater(() -> {
                gui = new ReservationGUI(this, currentRole, currentCustomerId);
                gui.setVisible(true);
            });
        }
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("reservation");
        sd.setName("car-rental-reservation");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
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

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        ACLMessage reply = msg.createReply();

        try {
            // Parse the complete message content
            String[] parts = content.split(";");
            String userInfo = parts[0];
            String[] userParts = userInfo.split(",");
            String role = userParts[0].split(":")[1];
            Integer customerId = null;
            if (userParts.length > 1) {
                customerId = Integer.parseInt(userParts[1].split(":")[1]);
            }

            String command = parts.length > 1 ? parts[1] : "";

            // Handle different commands
            if (command.startsWith("CREATE:")) {
                Map<String, String> reservationData = parseReservationData(command);
                boolean success = createReservation(reservationData, role, customerId);
                handleOperationResult(reply, success, "Reservation creation");
            }
            // ... (other command handling)
        } catch (Exception e) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("Error: " + e.getMessage());
        }

        send(reply);
    }

    public boolean createReservation(Map<String, String> reservationData, String role, Integer customerId) {
        String sql = "INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, total_cost, status) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, customerId != null ? customerId : Integer.parseInt(reservationData.get("customerId")));
            pstmt.setInt(2, Integer.parseInt(reservationData.get("vehicleId")));
            pstmt.setString(3, reservationData.get("startDate"));
            pstmt.setString(4, reservationData.get("endDate"));
            pstmt.setDouble(5, Double.parseDouble(reservationData.get("totalCost")));

            int result = pstmt.executeUpdate();
            if (result > 0) {
                updateVehicleStatus(Integer.parseInt(reservationData.get("vehicleId")), "RESERVED");
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancelReservation(int reservationId) {
        String sql = "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);

            // Get vehicle ID before updating reservation
            int vehicleId = getVehicleIdFromReservation(reservationId);
            int result = pstmt.executeUpdate();

            if (result > 0 && vehicleId != -1) {
                updateVehicleStatus(vehicleId, "AVAILABLE");
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifyReservation(Map<String, String> reservationData) {
        String sql = "UPDATE reservations SET start_date = ?, end_date = ?, total_cost = ? " +
                "WHERE reservation_id = ? AND status = 'PENDING'";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, reservationData.get("startDate"));
            pstmt.setString(2, reservationData.get("endDate"));
            pstmt.setDouble(3, Double.parseDouble(reservationData.get("totalCost")));
            pstmt.setInt(4, Integer.parseInt(reservationData.get("reservationId")));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getAllReservations() {
        List<Map<String, Object>> reservations = new ArrayList<>();
        String sql = "SELECT r.*, v.make, v.model, r.customer_id FROM reservations r " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                "ORDER BY r.start_date DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> reservation = new HashMap<>();
                reservation.put("reservationId", rs.getInt("reservation_id"));
                reservation.put("customerId", rs.getInt("customer_id"));
                reservation.put("startDate", rs.getString("start_date"));
                reservation.put("endDate", rs.getString("end_date"));
                reservation.put("status", rs.getString("status"));
                reservation.put("totalCost", rs.getDouble("total_cost"));
                reservation.put("vehicle", rs.getString("make") + " " + rs.getString("model"));
                reservations.add(reservation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    public List<Map<String, Object>> getAvailableVehicles() {
        List<Map<String, Object>> vehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles WHERE status = 'AVAILABLE'";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> vehicle = new HashMap<>();
                vehicle.put("vehicleId", rs.getInt("vehicle_id"));
                vehicle.put("make", rs.getString("make"));
                vehicle.put("model", rs.getString("model"));
                vehicle.put("year", rs.getInt("year"));
                vehicle.put("dailyRate", rs.getDouble("daily_rate"));
                vehicles.add(vehicle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return vehicles;
    }

    public List<Map<String, Object>> getCustomerReservations(int customerId) {
        List<Map<String, Object>> reservations = new ArrayList<>();
        String sql = "SELECT r.*, v.make, v.model FROM reservations r " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                "WHERE r.customer_id = ? ORDER BY r.start_date DESC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> reservation = new HashMap<>();
                reservation.put("reservationId", rs.getInt("reservation_id"));
                reservation.put("startDate", rs.getString("start_date"));
                reservation.put("endDate", rs.getString("end_date"));
                reservation.put("status", rs.getString("status"));
                reservation.put("totalCost", rs.getDouble("total_cost"));
                reservation.put("vehicle", rs.getString("make") + " " + rs.getString("model"));
                reservations.add(reservation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    private int getVehicleIdFromReservation(int reservationId) throws SQLException {
        String sql = "SELECT vehicle_id FROM reservations WHERE reservation_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("vehicle_id");
            }
        }
        return -1;
    }

    private void updateVehicleStatus(int vehicleId, String status) {
        String sql = "UPDATE vehicles SET status = ? WHERE vehicle_id = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, vehicleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> parseReservationData(String content) {
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

    public boolean createPendingPayment(Map<String, String> paymentData) {
        String sql = "INSERT INTO payments (reservation_id, amount, payment_status) VALUES (?, ?, 'PENDING')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(paymentData.get("reservationId")));
            pstmt.setDouble(2, Double.parseDouble(paymentData.get("amount")));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Add this method to show payment GUI
    public void showPaymentGUI() {
        // Create and send a message to the payment agent
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("SHOW_PAYMENT_GUI");

        // Find the payment agent
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("payment");
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

        System.out.println("Reservation Agent " + getAID().getName() + " terminating.");
    }
}