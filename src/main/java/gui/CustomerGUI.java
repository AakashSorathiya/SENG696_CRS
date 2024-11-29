package gui;

import javax.swing.*;
import java.awt.*;
import agents.CustomerAgent;

public class CustomerGUI extends JFrame {
    private CustomerAgent agent;
    private JPanel mainPanel;

    public CustomerGUI(CustomerAgent agent) {
        this.agent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Car Rental - Customer Interface");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Add components
        setupLoginPanel();
        setupReservationPanel();

        add(mainPanel);
    }

    private void setupLoginPanel() {
        JPanel loginPanel = new JPanel(new GridLayout(3, 2));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Login"));

        loginPanel.add(new JLabel("Username:"));
        JTextField usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        JPasswordField passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginPanel.add(loginButton);

        mainPanel.add(loginPanel, BorderLayout.NORTH);
    }

    private void setupReservationPanel() {
        JPanel reservationPanel = new JPanel();
        reservationPanel.setBorder(BorderFactory.createTitledBorder("Make Reservation"));
        // Add reservation components

        mainPanel.add(reservationPanel, BorderLayout.CENTER);
    }
}