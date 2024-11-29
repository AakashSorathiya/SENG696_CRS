package models;

public class PaymentGateway {
    private String id;
    private String name;
    private String status;
    private String lastCheck;

    // Constructors
    public PaymentGateway() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLastCheck() { return lastCheck; }
    public void setLastCheck(String lastCheck) { this.lastCheck = lastCheck; }
}