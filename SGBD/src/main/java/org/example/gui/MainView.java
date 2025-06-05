package org.example.gui;

import org.example.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    private void placeOrder() {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Creează buchet nou
            String insertBouquet = "INSERT INTO bouquets (user_id) VALUES (?)";
            PreparedStatement bouquetStmt = conn.prepareStatement(insertBouquet, Statement.RETURN_GENERATED_KEYS);
            bouquetStmt.setInt(1, userId);
            bouquetStmt.executeUpdate();

            ResultSet generatedKeys = bouquetStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int bouquetId = generatedKeys.getInt(1);

                String insertFlower = "INSERT INTO bouquet_flowers (bouquet_id, flower_id, quantity) VALUES (?, ?, ?)";
                PreparedStatement flowerStmt = conn.prepareStatement(insertFlower);

                boolean hasSelection = false;

                for (Map.Entry<Integer, JTextField> entry : quantityFields.entrySet()) {
                    int flowerId = entry.getKey();
                    int qty = Integer.parseInt(entry.getValue().getText());

                    if (qty > 0) {
                        flowerStmt.setInt(1, bouquetId);
                        flowerStmt.setInt(2, flowerId);
                        flowerStmt.setInt(3, qty);
                        flowerStmt.addBatch();
                        hasSelection = true;
                    }
                }

                if (!hasSelection) {
                    JOptionPane.showMessageDialog(this, "Selectează cel puțin o floare.");
                    conn.rollback();
                    return;
                }

                flowerStmt.executeBatch();
                conn.commit();
                JOptionPane.showMessageDialog(this, "Buchetul a fost comandat cu succes!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Eroare la trimiterea comenzii.");
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
