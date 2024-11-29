package agents;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import gui.MasterGUI;

public class MasterAgent extends Agent {
    private MasterGUI gui;

    @Override
    protected void setup() {
        gui = new MasterGUI(this);
        gui.setVisible(true);

        // Start other agents
        startAgents();
    }

    private void startAgents() {
        ContainerController cc = getContainerController();
        try {
            // Create agent instances
            AgentController customerAgent = cc.createNewAgent("CustomerAgent",
                    "agents.CustomerAgent", null);
            AgentController vehicleAgent = cc.createNewAgent("VehicleAgent",
                    "agents.VehicleManagementAgent", null);
            AgentController reservationAgent = cc.createNewAgent("ReservationAgent",
                    "agents.ReservationAgent", null);
            AgentController paymentAgent = cc.createNewAgent("PaymentAgent",
                    "agents.PaymentAgent", null);

            // Start agents
            customerAgent.start();
            vehicleAgent.start();
            reservationAgent.start();
            paymentAgent.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}