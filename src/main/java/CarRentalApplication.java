public class CarRentalApplication {
    public static void main(String[] args) {
        // Initialize JADE runtime
        jade.core.Runtime rt = jade.core.Runtime.instance();

        // Create main container
        jade.core.Profile p = new jade.core.ProfileImpl();
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            // Start master agent
            mainContainer.createNewAgent("MasterAgent", "agents.MasterAgent", null).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}