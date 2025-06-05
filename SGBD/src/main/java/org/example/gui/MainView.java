package org.example.gui;

import org.example.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class MainView extends JFrame {

    private int userId;
    private String username;
    private JPanel flowerPanel;
    private JButton orderButton;
    private JButton popularBouquetsButton;
    private boolean isAdmin;

    // Folosim o mapă ca să reținem floarea și câmpul cu cantitate
    private Map<Integer, JTextField> quantityFields = new HashMap<>();
    private JButton randomBouquetButton;

    public MainView(int userId, String username) {
        this.userId = userId;
        this.username = username;

        setTitle("FloriOnline - Bine ai venit, " + username);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initUI();
    }


    private void initUI() {
        setLayout(new BorderLayout());

        JLabel welcome = new JLabel("Creează-ți buchetul:", SwingConstants.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 16));
        add(welcome, BorderLayout.NORTH);

        flowerPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(flowerPanel);
        add(scrollPane, BorderLayout.CENTER);

        orderButton = new JButton("Comandă buchet");
        popularBouquetsButton = new JButton("Cele mai populare buchete");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(orderButton);
        bottomPanel.add(popularBouquetsButton);
        add(bottomPanel, BorderLayout.SOUTH);

        loadFlowers();

        orderButton.addActionListener(e -> placeOrder());
        popularBouquetsButton.addActionListener(e -> showPopularBouquets());
        randomBouquetButton = new JButton("Generează buchet random");
        bottomPanel.add(randomBouquetButton);

        randomBouquetButton.addActionListener(e -> generateRandomBouquet());
    }

    private void loadFlowers() {
        flowerPanel.removeAll();
        quantityFields.clear();

        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, price, stock FROM flowers");

            while (rs.next()) {
                int flowerId = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                JLabel label = new JLabel(name + " - " + price + " RON | Stoc: " + stock);
                flowerPanel.add(label);

                JTextField quantityField = new JTextField("0");
                quantityFields.put(flowerId, quantityField);
                flowerPanel.add(quantityField);

                flowerPanel.add(new JLabel("buc"));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        flowerPanel.revalidate();
        flowerPanel.repaint();
    }

    private void generateRandomBouquet() {
        try (Connection conn = DBConnection.getConnection()) {
            // Reset toate câmpurile la 0
            for (JTextField field : quantityFields.values()) {
                field.setText("0");
                field.setBackground(Color.WHITE);
            }

            // Apelăm procedura stocată
            String sql = "CALL generate_random_bouquet(?, ?, ?, ?, ?)";
            CallableStatement stmt = conn.prepareCall(sql);

            // Setăm toți parametrii
            stmt.setInt(1, userId);                    // p_user_id
            stmt.setInt(2, 3);                         // p_min_flowers
            stmt.setInt(3, 7);                         // p_max_flowers
            stmt.setBigDecimal(4, new BigDecimal("200.00")); // p_max_price
            stmt.registerOutParameter(5, Types.INTEGER);  // p_bouquet_id

            stmt.execute();

            // Obținem ID-ul buchetului generat
            int bouquetId = stmt.getInt(5);

            if (!stmt.wasNull()) {
                // Preluăm florile generate și completăm câmpurile
                try (PreparedStatement selectStmt = conn.prepareStatement(
                        "SELECT bf.flower_id, bf.quantity FROM bouquet_flowers bf WHERE bf.bouquet_id = ?")) {

                    selectStmt.setInt(1, bouquetId);
                    ResultSet rs = selectStmt.executeQuery();

                    while (rs.next()) {
                        int flowerId = rs.getInt("flower_id");
                        int quantity = rs.getInt("quantity");

                        // Completăm câmpul corespunzător
                        JTextField field = quantityFields.get(flowerId);
                        if (field != null) {
                            field.setText(String.valueOf(quantity));
                            field.setBackground(new Color(255, 255, 200));
                        }
                    }

                    // Afișăm preview-ul buchetului
                    showBouquetPreview(bouquetId);
                }

                // Ștergem buchetul temporar
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM bouquet_flowers WHERE bouquet_id = ?")) {
                    deleteStmt.setInt(1, bouquetId);
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM bouquets WHERE id = ?")) {
                    deleteStmt.setInt(1, bouquetId);
                    deleteStmt.executeUpdate();
                }

            } else {
                JOptionPane.showMessageDialog(this,
                        "Nu s-a putut genera buchetul. Verificați stocul disponibil.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la generarea buchetului random: " + ex.getMessage());
        }
    }

    private void showBouquetPreview(int bouquetId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
            SELECT f.name, bf.quantity, f.price
            FROM bouquet_flowers bf
            JOIN flowers f ON bf.flower_id = f.id
            WHERE bf.bouquet_id = ?
            """)) {

            stmt.setInt(1, bouquetId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder message = new StringBuilder();
            message.append("Buchet generat cu succes!\n\n");
            double totalPrice = 0;

            while (rs.next()) {
                String flowerName = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double subtotal = price * quantity;
                totalPrice += subtotal;

                message.append(String.format("%s x%d (%.2f RON)\n",
                        flowerName, quantity, subtotal));
            }

            message.append("\nPreț total: ").append(String.format("%.2f RON", totalPrice));
            message.append("\n\nCantitățile au fost completate automat.");
            message.append("\nPentru a finaliza comanda, apăsați butonul 'Comandă buchet'");

            JOptionPane.showMessageDialog(this, message.toString(),
                    "Buchet Random", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la afișarea detaliilor buchetului.");
        }
    }

    private void placeOrder() {
        Map<Integer, Integer> selectedFlowers = new HashMap<>();
        for (Map.Entry<Integer, JTextField> entry : quantityFields.entrySet()) {
            int flowerId = entry.getKey();
            try {
                int quantity = Integer.parseInt(entry.getValue().getText().trim());
                if (quantity > 0) {
                    selectedFlowers.put(flowerId, quantity);
                }
            } catch (NumberFormatException e) {
            }
        }

        if (selectedFlowers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vă rugăm să selectați cel puțin o floare!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String checkStock = "SELECT id, stock FROM flowers WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(checkStock);

            for (Map.Entry<Integer, Integer> entry : selectedFlowers.entrySet()) {
                stmt.setInt(1, entry.getKey());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int availableStock = rs.getInt("stock");
                    if (entry.getValue() > availableStock) {
                        JOptionPane.showMessageDialog(this,
                                "Stoc insuficient pentru una din florile selectate!\n" +
                                        "Stoc disponibil: " + availableStock);
                        return;
                    }
                }
            }
            DetailsView deliveryView = new DetailsView(userId, selectedFlowers);
            deliveryView.setVisible(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la verificarea stocului: " + ex.getMessage());
        }
    }

    private void showPopularBouquets() {
        String sql = """
            SELECT f.name, SUM(bf.quantity) AS total_vandut
            FROM bouquet_flowers bf
            JOIN flowers f ON bf.flower_id = f.id
            GROUP BY f.name
            ORDER BY total_vandut DESC
            LIMIT 5
        """;

        StringBuilder sb = new StringBuilder("Top flori din buchete:\n\n");

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sb.append(rs.getString("name"))
                        .append(" - ")
                        .append(rs.getInt("total_vandut"))
                        .append(" bucăți\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Top flori", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la încărcarea buchetelor populare.");
        }
    }
}
