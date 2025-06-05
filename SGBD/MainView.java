package org.example.gui;

import org.example.db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class MainView extends JFrame implements OrderCompletedListener {
    private final int userId;
    private final String username;
    private Map<Integer, JSpinner> flowerSpinners = new HashMap<>();
    private Map<Integer, Double> flowerPrices = new HashMap<>();
    private JLabel totalPriceLabel;
    private double totalPrice = 0.0;
    private JPanel flowersPanel;

    public MainView(int userId, String username) {
        this.userId = userId;
        this.username = username;
        setTitle("Bun venit, " + username + "!");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel userLabel = new JLabel("Utilizator: " + username);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(userLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> handleLogout());
        headerPanel.add(logoutButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Flowers panel
        flowersPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        loadFlowers();

        // Controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        totalPriceLabel = new JLabel("Preț total: 0.00 RON");
        totalPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton randomButton = new JButton("Generare Aleatorie");
        randomButton.addActionListener(e -> generateRandomBouquet());

        JButton orderButton = new JButton("Plasează Comanda");
        orderButton.addActionListener(e -> placeOrder());

        controlsPanel.add(randomButton);
        controlsPanel.add(totalPriceLabel);
        controlsPanel.add(orderButton);

        mainPanel.add(new JScrollPane(flowersPanel), BorderLayout.CENTER);
        mainPanel.add(controlsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void handleLogout() {
        int option = JOptionPane.showConfirmDialog(this,
                "Sunteți sigur că doriți să vă deconectați?",
                "Confirmare Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginView loginView = new LoginView();
                loginView.setVisible(true);
            });
        }
    }

    private void loadFlowers() {
        flowersPanel.removeAll();
        flowerSpinners.clear();
        flowerPrices.clear();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price, stock FROM flowers ORDER BY name ASC")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                flowerPrices.put(id, price);

                JPanel flowerPanel = new JPanel(new GridLayout(4, 1, 5, 5));
                flowerPanel.setBorder(BorderFactory.createEtchedBorder());

                flowerPanel.add(new JLabel(name, SwingConstants.CENTER));
                flowerPanel.add(new JLabel(String.format("%.2f RON", price), SwingConstants.CENTER));

                JLabel stockLabel = new JLabel("Stoc: " + stock, SwingConstants.CENTER);
                if (stock < 5) {
                    stockLabel.setForeground(Color.RED);
                }
                flowerPanel.add(stockLabel);

                SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, stock, 1);
                JSpinner spinner = new JSpinner(spinnerModel);
                spinner.addChangeListener(e -> updateTotalPrice());
                flowerSpinners.put(id, spinner);

                flowerPanel.add(spinner);
                flowersPanel.add(flowerPanel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la încărcarea florilor: " + e.getMessage());
        }

        flowersPanel.revalidate();
        flowersPanel.repaint();
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (Map.Entry<Integer, JSpinner> entry : flowerSpinners.entrySet()) {
            int flowerId = entry.getKey();
            int quantity = (Integer) entry.getValue().getValue();
            double price = flowerPrices.get(flowerId);
            totalPrice += quantity * price;
        }
        totalPriceLabel.setText(String.format("Preț total: %.2f RON", totalPrice));
    }

    private void generateRandomBouquet() {
        try (Connection conn = DBConnection.getConnection()) {
            // Reset toate spinnerele la 0
            flowerSpinners.values().forEach(spinner -> spinner.setValue(0));

            String sql = "SELECT * FROM get_random_bouquet(?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, 1);             // min_flowers
                stmt.setInt(2, 5);             // max_flowers
                stmt.setBigDecimal(3, new BigDecimal("1000.00")); // max_price

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int flowerId = rs.getInt("flower_id");
                    int quantity = rs.getInt("quantity");

                    JSpinner spinner = flowerSpinners.get(flowerId);
                    if (spinner != null) {
                        spinner.setValue(quantity);
                    }
                }

                updateTotalPrice();

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Eroare la generarea buchetului: " + e.getMessage(),
                        "Eroare",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la conectarea la baza de date",
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void placeOrder() {
        Map<Integer, Integer> selectedFlowers = new HashMap<>();

        for (Map.Entry<Integer, JSpinner> entry : flowerSpinners.entrySet()) {
            int quantity = (Integer) entry.getValue().getValue();
            if (quantity > 0) {
                selectedFlowers.put(entry.getKey(), quantity);
            }
        }

        if (selectedFlowers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vă rugăm să selectați cel puțin o floare!",
                    "Atenție",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String checkStockSql = "SELECT id, stock FROM flowers WHERE id = ? AND stock >= ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkStockSql)) {
                for (Map.Entry<Integer, Integer> entry : selectedFlowers.entrySet()) {
                    stmt.setInt(1, entry.getKey());
                    stmt.setInt(2, entry.getValue());
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this,
                                "Stoc insuficient pentru una sau mai multe flori selectate!",
                                "Eroare",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            DetailsView detailsView = new DetailsView(userId, selectedFlowers, totalPrice, this);
            detailsView.setLocationRelativeTo(this);
            detailsView.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la verificarea stocului: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onOrderCompleted() {
        loadFlowers();
        updateTotalPrice();
    }
}