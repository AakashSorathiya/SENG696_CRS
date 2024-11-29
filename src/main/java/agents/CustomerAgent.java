package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import gui.CustomerGUI;

public class CustomerAgent extends Agent {
    private CustomerGUI gui;

    @Override
    protected void setup() {
        gui = new CustomerGUI(this);
        gui.setVisible(true);

        addBehaviour(new CustomerBehaviour());
    }

    private class CustomerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                // Process message
                processMessage(msg);
            } else {
                block();
            }
        }

        private void processMessage(ACLMessage msg) {
            // Handle different message types
            switch (msg.getPerformative()) {
                case ACLMessage.REQUEST:
                    // Handle request
                    break;
                case ACLMessage.INFORM:
                    // Handle information
                    break;
                // Add other cases as needed
            }
        }
    }
}