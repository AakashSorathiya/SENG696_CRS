package agents;

import gui.MasterGUI;
import gui.RegistrationGUI;
import gui.ReservationGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import javax.swing.SwingUtilities;
import java.sql.*;

public class MasterAgent extends Agent {
    private MasterGUI gui;
    private static int currentState = 0;

    protected void setup() {
        System.out.println("Master Agent " + getLocalName() + " starting...");

        // Initialize GUI on EDT
        SwingUtilities.invokeLater(() -> {
            gui = new MasterGUI(this);
        });

        // Register master agent services
        registerService();

        // Add behavior to handle messages from other agents
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });

        System.out.println("Master Agent setup completed");
    }

    private void registerService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("master-agent");
        sd.setName("car-rental-master");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        switch (msg.getPerformative()) {
            case ACLMessage.INFORM:
                gui.updateLog("Success: " + content);
                break;
            case ACLMessage.FAILURE:
                gui.updateLog("Error: " + content);
                break;
            default:
                gui.updateLog("Received message: " + content);
        }
    }

    // Method called from GUI
    public void processRequest(int choice) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        String content = "";
        AID receiver = null;

        switch (choice) {
            case 1: // Registration
                content = "REGISTRATION_REQUEST";
                receiver = new AID("reg", AID.ISLOCALNAME);
                break;
            case 2: // Reservations
                content = "RESERVATION_REQUEST";
                receiver = new AID("res", AID.ISLOCALNAME);
                break;
            case 3: // Vehicle Management
                content = "VEHICLE_REQUEST";
                receiver = new AID("veh", AID.ISLOCALNAME);
                break;
            case 4: // Payments
                content = "PAYMENT_REQUEST";
                receiver = new AID("pay", AID.ISLOCALNAME);
                break;
        }

        if (receiver != null) {
            msg.setContent(content);
            msg.addReceiver(receiver);
            send(msg);
            gui.updateLog("Sent request to " + receiver.getLocalName());
        }
    }

    protected void takeDown() {
        // Deregister from the DF
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Close GUI
        if (gui != null) {
            gui.dispose();
        }

        System.out.println("Master Agent " + getAID().getName() + " terminating.");
    }
}