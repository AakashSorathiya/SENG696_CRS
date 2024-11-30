package agents;

// ReservationAgent.java
import gui.ReservationGUI;
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
import java.time.format.DateTimeFormatter;

public class ReservationAgent extends Agent {
    private Connection dbConnection;
    private ReservationGUI gui;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/car_rental";
    private static final String USER = "root";
    private static final String PASS = "password";

    protected void setup() {
        // Initialize database connection
        setupDatabase();

        // Create and show GUI
        gui = new ReservationGUI(this);
        gui.display();

        // Register the agent services
        registerService();

        // Add behavior to handle reservation requests
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    try {
                        if (content.startsWith("CREATE:")) {
                            Map<String, String> reservationData = parseReservationData(content);
                            boolean success = createReservation(reservationData);
                            handleOperationResult(reply, success, "Reservation creation");
                        } else if (content.startsWith("CANCEL:")) {
                            int reservationId = Integer.parseInt(content.substring(7));
                            boolean success = cancelReservation(reservationId);
                            handleOperationResult(reply, success, "Reservation cancellation");
                        } else if (content.startsWith("MODIFY:")) {
                            Map<String, String> modificationData = parseReservationData(content);
                            boolean success = modifyReservation(modificationData);
                            handleOperationResult(reply, success, "Reservation modification");
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
        sd.setType("reservation");
        sd.setName("car-rental-reservation");
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

        System.out.println("Reservation Agent " + getAID().getName() + " terminating.");
    }

    // Database operations
    public boolean createReservation(Map<String, String> reservationData) {
        String sql = "INSERT INTO reservations (customer_id, vehicle_id, start_date, end_date, total_cost, status) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(reservationData.get("customerId")));
            pstmt.setInt(2, Integer.parseInt(reservationData.get("vehicleId")));
            pstmt.setString(3, reservationData.get("startDate"));
            pstmt.setString(4, reservationData.get("endDate"));
            pstmt.setDouble(5, Double.parseDouble(reservationData.get("totalCost")));

            int result = pstmt.executeUpdate();
            updateVehicleStatus(Integer.parseInt(reservationData.get("vehicleId")), "RESERVED");
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancelReservation(int reservationId) {
        String sql = "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);

            // First get the vehicle ID
            int vehicleId = getVehicleIdFromReservation(reservationId);
            int result = pstmt.executeUpdate();

            if (result > 0) {
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
        String sql = "UPDATE reservations SET start_date = ?, end_date = ?, total_cost = ? WHERE reservation_id = ?";

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

    // Helper methods
    private Map<String, String> parseReservationData(String content) {
        Map<String, String> data = new HashMap<>();
        String[] pairs = content.substring(content.indexOf(":") + 1).split(",");
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

    // Methods that can be called from GUI
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
                "WHERE r.customer_id = ?";

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
}
