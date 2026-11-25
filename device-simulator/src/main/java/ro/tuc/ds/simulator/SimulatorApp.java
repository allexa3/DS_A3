package ro.tuc.ds.simulator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Device Data Simulator (message producer) - standalone desktop application.
 * <pre>
 *   java -jar device-simulator.jar                      graphical interface
 *   java -jar device-simulator.jar --headless --devices id1,id2 [--host h] [--port p] [--user u] [--password pw]
 *                                  [--queue q] [--interval seconds] [--backfill]
 * </pre>
 */
public final class SimulatorApp {

    private SimulatorApp() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--headless")) {
            runHeadless(args);
        } else {
            SwingUtilities.invokeLater(SimulatorApp::showWindow);
        }
    }

    // ------------------------------------------------------------------ headless
    private static void runHeadless(String[] args) throws Exception {
        String host = "localhost";
        int port = 5672;
        String user = "simulator";
        String password = "change-me-data";
        String queue = "device-measurements";
        String devices = "";
        int interval = 600;
        boolean backfill = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--user" -> user = args[++i];
                case "--password" -> password = args[++i];
                case "--queue" -> queue = args[++i];
                case "--devices" -> devices = args[++i];
                case "--interval" -> interval = Integer.parseInt(args[++i]);
                case "--backfill" -> backfill = true;
                default -> throw new IllegalArgumentException("Unknown option " + args[i]);
            }
        }
        Simulator simulator = new Simulator(msg -> System.out.println(LocalTime.now().withNano(0) + "  " + msg));
        simulator.start(new Simulator.Config(host, port, user, password, queue, parseDevices(devices), interval, backfill));
        Runtime.getRuntime().addShutdownHook(new Thread(simulator::stop));
        Thread.currentThread().join();
    }

    static List<UUID> parseDevices(String text) {
        List<UUID> ids = new ArrayList<>();
        for (String part : text.split("[,;\\s]+")) {
            if (!part.isBlank()) {
                ids.add(UUID.fromString(part.trim()));
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one device id (UUID)");
        }
        return ids;
    }

    // ------------------------------------------------------------------ desktop window
    private static void showWindow() {
        JFrame frame = new JFrame("Device Data Simulator");
        JTextField host = new JTextField("localhost", 14);
        JSpinner port = new JSpinner(new SpinnerNumberModel(5672, 1, 65535, 1));
        JTextField user = new JTextField("simulator", 14);
        JPasswordField password = new JPasswordField("change-me-data", 14);
        JTextField queue = new JTextField("device-measurements", 14);
        JTextField devices = new JTextField(40);
        JSpinner interval = new JSpinner(new SpinnerNumberModel(600, 1, 86400, 1));
        JCheckBox backfill = new JCheckBox("Send today's readings so far (from 00:00) when starting");
        backfill.setSelected(true);
        JButton start = new JButton("Start");
        JButton stop = new JButton("Stop");
        stop.setEnabled(false);
        JTextArea logArea = new JTextArea(14, 70);
        logArea.setEditable(false);

        Simulator simulator = new Simulator(msg -> SwingUtilities.invokeLater(() -> {
            logArea.append(LocalTime.now().withNano(0) + "  " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        int row = 0;
        row = addRow(form, row, "Broker host", host);
        row = addRow(form, row, "AMQP port", port);
        row = addRow(form, row, "User", user);
        row = addRow(form, row, "Password", password);
        row = addRow(form, row, "Queue", queue);
        row = addRow(form, row, "Device id(s)", devices);
        row = addRow(form, row, "Seconds between readings", interval);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        form.add(backfill, c);
        form.add(new JLabel(" "), c);
        JPanel buttons = new JPanel();
        buttons.add(start);
        buttons.add(stop);
        c.gridy = row;
        form.add(buttons, c);

        start.addActionListener(e -> {
            try {
                Simulator.Config cfg = new Simulator.Config(host.getText().trim(), (Integer) port.getValue(),
                        user.getText().trim(), new String(password.getPassword()), queue.getText().trim(),
                        parseDevices(devices.getText()), (Integer) interval.getValue(), backfill.isSelected());
                start.setEnabled(false);
                new Thread(() -> {
                    try {
                        simulator.start(cfg);
                        SwingUtilities.invokeLater(() -> stop.setEnabled(true));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            start.setEnabled(true);
                            JOptionPane.showMessageDialog(frame, "Could not start: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }, "simulator-start").start();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
            }
        });
        stop.addActionListener(e -> {
            simulator.stop();
            stop.setEnabled(false);
            start.setEnabled(true);
        });

        frame.setLayout(new BorderLayout());
        frame.add(form, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static int addRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(2, 2, 2, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(field, c);
        return row + 1;
    }
}
