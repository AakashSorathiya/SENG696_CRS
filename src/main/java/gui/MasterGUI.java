package gui;

import javax.swing.*;
import java.awt.*;
import agents.MasterAgent;

public class MasterGUI extends JFrame {
    private final MasterAgent agent;
    private JPanel mainPanel;

    public MasterGUI(MasterAgent agent) {
        this.agent = agent;
        setupGUI();
    }

    private void setupGUI() {
        setTitle("Car Rental System - Master Control");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Add components
        setupMenuBar();
        setupMainPanel();

        add(mainPanel);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Agents Menu
        JMenu agentsMenu = new JMenu("Agents");
        JMenuItem startAllItem = new JMenuItem("Start All Agents");
        JMenuItem stopAllItem = new JMenuItem("Stop All Agents");
        agentsMenu.add(startAllItem);
        agentsMenu.add(stopAllItem);

        menuBar.add(fileMenu);
        menuBar.add(agentsMenu);
        setJMenuBar(menuBar);
    }

    private void setupMainPanel() {
        // Add status panel
        JPanel statusPanel = new JPanel(new GridLayout(0, 2));
        statusPanel.setBorder(BorderFactory.createTitledBorder("System Status"));
        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Add monitoring panel
        JPanel monitorPanel = new JPanel();
        monitorPanel.setBorder(BorderFactory.createTitledBorder("Agent Monitoring"));
        mainPanel.add(monitorPanel, BorderLayout.CENTER);
    }
}