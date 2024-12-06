package agents;

// VehicleManagementAgent.java
import database.DatabaseConnection;
import gui.VehicleManagementGUI;
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
import java.time.LocalDate;

public class VehicleManagementAgent extends Agent {
    private Connection dbConnection;
    private VehicleManagementGUI gui;


    protected void setup() {
        // Initialize database connection
        setupDatabase();

        // Create and show GUI
//        gui = new VehicleManagementGUI(this);
//        gui.display();

        // Register the agent services
        registerService();

        // Add behavior to handle vehicle management requests
        addBehaviour(new SimpleBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    try {
                        if (content.startsWith("ADD:")) {
                            Map<String, String> vehicleData = parseVehicleData(content);
                            boolean success = addVehicle(vehicleData);
                            handleOperationResult(reply, success, "Vehicle addition");
                        } else if (content.startsWith("UPDATE_STATUS:")) {
                            String[] parts = content.substring(14).split(",");
                            boolean success = updateVehicleStatus(Integer.parseInt(parts[0]), parts[1]);
                            handleOperationResult(reply, success, "Status update");
                        } else if (content.startsWith("MAINTENANCE:")) {
                            int vehicleId = Integer.parseInt(content.substring(12));
                            boolean success = setVehicleMaintenanceStatus(vehicleId);
                            handleOperationResult(reply, success, "Maintenance status update");
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

            @Override
            public boolean done() {
                return false;
            }
        });
    }

    private void setupDatabase() {
        try {
//            Class.forName("com.mysql.jdbc.Driver");
            dbConnection = DatabaseConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            doDelete();
        }
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("vehicle-management");
        sd.setName("car-rental-vehicle-management");
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

        // Close GUI with null check
        if (gui != null) {
            gui.dispose();
        }

        System.out.println("Vehicle Management Agent " + getAID().getName() + " terminating.");
    }

    // Database operations
    public boolean addVehicle(Map<String, String> vehicleData) {
        String sql = "INSERT INTO vehicles (make, model, year, color, license_plate, daily_rate, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, vehicleData.get("make"));
            pstmt.setString(2, vehicleData.get("model"));
            pstmt.setInt(3, Integer.parseInt(vehicleData.get("year")));
            pstmt.setString(4, vehicleData.get("color"));
            pstmt.setString(5, vehicleData.get("licensePlate"));
            pstmt.setDouble(6, Double.parseDouble(vehicleData.get("dailyRate")));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateVehicleStatus(int vehicleId, String status) {
        String sql = "UPDATE vehicles SET status = ? WHERE vehicle_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, vehicleId);

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setVehicleMaintenanceStatus(int vehicleId) {
        String sql = "UPDATE vehicles SET status = 'MAINTENANCE', last_maintenance_date = CURDATE(), " +
                "next_maintenance_date = DATE_ADD(CURDATE(), INTERVAL 3 MONTH) WHERE vehicle_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, vehicleId);

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateVehicle(Map<String, String> vehicleData) {
        String sql = "UPDATE vehicles SET make = ?, model = ?, year = ?, color = ?, " +
                "daily_rate = ? WHERE vehicle_id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, vehicleData.get("make"));
            pstmt.setString(2, vehicleData.get("model"));
            pstmt.setInt(3, Integer.parseInt(vehicleData.get("year")));
            pstmt.setString(4, vehicleData.get("color"));
            pstmt.setDouble(5, Double.parseDouble(vehicleData.get("dailyRate")));
            pstmt.setInt(6, Integer.parseInt(vehicleData.get("vehicleId")));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getAllVehicles() {
        List<Map<String, Object>> vehicles = new ArrayList<>();
        String sql = "SELECT * FROM vehicles";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> vehicle = new HashMap<>();
                vehicle.put("vehicleId", rs.getInt("vehicle_id"));
                vehicle.put("make", rs.getString("make"));
                vehicle.put("model", rs.getString("model"));
                vehicle.put("year", rs.getInt("year"));
                vehicle.put("color", rs.getString("color"));
                vehicle.put("licensePlate", rs.getString("license_plate"));
                vehicle.put("status", rs.getString("status"));
                vehicle.put("dailyRate", rs.getDouble("daily_rate"));
                vehicle.put("lastMaintenance", rs.getDate("last_maintenance_date"));
                vehicle.put("nextMaintenance", rs.getDate("next_maintenance_date"));
                vehicles.add(vehicle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return vehicles;
    }

    private Map<String, String> parseVehicleData(String content) {
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
}