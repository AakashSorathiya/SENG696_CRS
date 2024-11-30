package agents;

// MasterAgent.java
import gui.MasterGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class MasterAgent extends Agent {
    private MasterGUI gui;
    private static int flag = -1;
    private static int currentState = 0;
    public static int masterChoice = 0;

    protected void setup() {
        addBehaviour(new SimpleBehaviour(this) {
            public void action() {
                switch (currentState) {
                    case 0:
                        // Wait if GUI is already showing
                        while(flag == 1) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // Create and show GUI
                        gui = new MasterGUI(MasterAgent.this);
                        gui.display();
                        flag = 1;
                        masterChoice = 0;

                        System.out.println("Master Agent: " + getLocalName());
                        System.out.println("1: Registration\n2: Reservations\n3: Vehicle Management\n4: Payments");

                        while(masterChoice == 0) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        currentState = masterChoice;
                        break;

                    case 1: // Registration
                        ACLMessage regMsg = new ACLMessage(ACLMessage.REQUEST);
                        regMsg.setContent("REGISTRATION_REQUEST");
                        regMsg.addReceiver(new AID("reg", AID.ISLOCALNAME));
                        send(regMsg);
                        currentState = 99;
                        break;

                    case 2: // Reservations
                        ACLMessage resMsg = new ACLMessage(ACLMessage.REQUEST);
                        resMsg.setContent("RESERVATION_REQUEST");
                        resMsg.addReceiver(new AID("res", AID.ISLOCALNAME));
                        send(resMsg);
                        currentState = 99;
                        break;

                    case 3: // Vehicle Management
                        ACLMessage vehMsg = new ACLMessage(ACLMessage.REQUEST);
                        vehMsg.setContent("VEHICLE_REQUEST");
                        vehMsg.addReceiver(new AID("veh", AID.ISLOCALNAME));
                        send(vehMsg);
                        currentState = 99;
                        break;

                    case 4: // Payments
                        ACLMessage payMsg = new ACLMessage(ACLMessage.REQUEST);
                        payMsg.setContent("PAYMENT_REQUEST");
                        payMsg.addReceiver(new AID("pay", AID.ISLOCALNAME));
                        send(payMsg);
                        currentState = 99;
                        break;

                    case 99:
                        // Reset state and flag for next operation
                        currentState = 0;
                        flag = -1;
                        break;
                }
            }

            public boolean done() {
                return false;
            }
        });
    }

    protected void takeDown() {
        if (gui != null) {
            gui.dispose();
        }
        System.out.println("Master Agent " + getAID().getName() + " terminating.");
    }

    // Method to be called from GUI
    public void processRequest(int choice) {
        masterChoice = choice;
    }
}