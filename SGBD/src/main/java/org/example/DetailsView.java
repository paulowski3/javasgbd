package org.example.gui;

import org.example.db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Map;

public class DetailsView extends JFrame {
    private final int userId;
    private final Map<Integer, Integer> selectedFlowers;
    private double totalPrice;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextArea notesArea;
    private OrderCompletedListener orderCompletedListener;

    public DetailsView(int userId, Map<Integer, Integer> selectedFlowers, OrderCompletedListener listener) {
        this.userId = userId;
        this.selectedFlowers = selectedFlowers;
        this.orderCompletedListener = listener;
        calculateTotalPrice();

        setTitle("Detalii Livrare");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void calculateTotalPrice() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT price FROM flowers WHERE id = ?")) {

            totalPrice = 0.0;
            for (Map.Entry<Integer, Integer> entry : selectedFlowers.entrySet()) {
                int flowerId = entry.getKey();
                int quantity = entry.getValue();

                stmt.setInt(1, flowerId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    double price = rs.getDouble("price");
                    totalPrice += price * quantity;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la calcularea prețului total: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Panou principal cu GridBagLayout pentru un aspect mai flexibil
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Preț total
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel priceLabel = new JLabel(String.format("Preț total: %.2f RON", totalPrice));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mainPanel.add(priceLabel, gbc);

        // Nume
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Nume:"), gbc);

        gbc.gridx = 1;
        nameField = new JTextField(20);
        mainPanel.add(nameField, gbc);

        // Telefon
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Telefon:"), gbc);

        gbc.gridx = 1;
        phoneField = new JTextField(20);
        mainPanel.add(phoneField, gbc);

        // Adresă
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Adresă:"), gbc);

        gbc.gridx = 1;
        addressField = new JTextField(20);
        mainPanel.add(addressField, gbc);

        // Observații
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Observații:"), gbc);

        gbc.gridx = 1;
        notesArea = new JTextArea(4, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(notesArea);
        mainPanel.add(scrollPane, gbc);

        // Butoane
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton confirmButton = new JButton("Confirmă Comanda");
        JButton cancelButton = new JButton("Anulează");

        confirmButton.addActionListener(e -> placeOrder());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void placeOrder() {
        if (!validateInputs()) {
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Inserăm detaliile de livrare
                String insertDeliverySQL = """
                    INSERT INTO delivery_details (user_id, name, phone, address, notes)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                """;

                PreparedStatement deliveryStmt = conn.prepareStatement(insertDeliverySQL);
                deliveryStmt.setInt(1, userId);
                deliveryStmt.setString(2, nameField.getText().trim());
                deliveryStmt.setString(3, phoneField.getText().trim());
                deliveryStmt.setString(4, addressField.getText().trim());
                deliveryStmt.setString(5, notesArea.getText().trim());

                ResultSet rs = deliveryStmt.executeQuery();
                int deliveryId = -1;
                if (rs.next()) {
                    deliveryId = rs.getInt(1);
                }

                // Creăm buchetul
                String insertBouquetSQL = """
                    INSERT INTO bouquets (user_id, delivery_id, created_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    RETURNING id
                """;

                PreparedStatement bouquetStmt = conn.prepareStatement(insertBouquetSQL);
                bouquetStmt.setInt(1, userId);
                bouquetStmt.setInt(2, deliveryId);

                rs = bouquetStmt.executeQuery();
                int bouquetId = -1;
                if (rs.next()) {
                    bouquetId = rs.getInt(1);
                }

                // Adăugăm florile în buchet
                String insertFlowerSQL = "INSERT INTO bouquet_flowers (bouquet_id, flower_id, quantity) VALUES (?, ?, ?)";
                String updateStockSQL = "UPDATE flowers SET stock = stock - ? WHERE id = ?";

                for (Map.Entry<Integer, Integer> entry : selectedFlowers.entrySet()) {
                    // Adăugăm floarea în buchet
                    PreparedStatement flowerStmt = conn.prepareStatement(insertFlowerSQL);
                    flowerStmt.setInt(1, bouquetId);
                    flowerStmt.setInt(2, entry.getKey());
                    flowerStmt.setInt(3, entry.getValue());
                    flowerStmt.executeUpdate();

                    // Actualizăm stocul
                    PreparedStatement stockStmt = conn.prepareStatement(updateStockSQL);
                    stockStmt.setInt(1, entry.getValue());
                    stockStmt.setInt(2, entry.getKey());
                    stockStmt.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this,
                        "Comanda a fost plasată cu succes!",
                        "Succes",
                        JOptionPane.INFORMATION_MESSAGE);

                if (orderCompletedListener != null) {
                    orderCompletedListener.onOrderCompleted();
                }

                dispose();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la plasarea comenzii: " + ex.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vă rugăm să introduceți numele!");
            return false;
        }
        if (phoneField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vă rugăm să introduceți numărul de telefon!");
            return false;
        }
        if (addressField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vă rugăm să introduceți adresa de livrare!");
            return false;
        }
        return true;
    }
}