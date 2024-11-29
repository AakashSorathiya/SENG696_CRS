import jade.core.Agent;

public class HelloWorld extends Agent {
    @Override
    protected void setup() {
        System.out.println("I am an agent");
        System.out.println("My name is: "+getAID().getName());
    }
}
