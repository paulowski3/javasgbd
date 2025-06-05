package org.example.gui;

import org.example.db.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Map;

public class DetailsView extends JFrame {
    private final int userId;
    private final Map<Integer, Integer> selectedFlowers;
    private final double totalPrice;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextArea notesArea;
    private OrderCompletedListener orderCompletedListener;

    public DetailsView(int userId, Map<Integer, Integer> selectedFlowers, double totalPrice, OrderCompletedListener listener) {
        this.userId = userId;
        this.selectedFlowers = selectedFlowers;
        this.totalPrice = totalPrice;
        this.orderCompletedListener = listener;

        setTitle("Detalii Livrare");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

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
        mainPanel.add(new JScrollPane(notesArea), gbc);

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

                // Adăugăm florile în buchet și actualizăm stocul
                String insertFlowerSQL = "INSERT INTO bouquet_flowers (bouquet_id, flower_id, quantity) VALUES (?, ?, ?)";
                String updateStockSQL = "UPDATE flowers SET stock = stock - ? WHERE id = ? AND stock >= ?";

                PreparedStatement flowerStmt = conn.prepareStatement(insertFlowerSQL);
                PreparedStatement stockStmt = conn.prepareStatement(updateStockSQL);

                for (Map.Entry<Integer, Integer> entry : selectedFlowers.entrySet()) {
                    int flowerId = entry.getKey();
                    int quantity = entry.getValue();

                    // Actualizăm stocul
                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, flowerId);
                    stockStmt.setInt(3, quantity);
                    int updatedRows = stockStmt.executeUpdate();

                    if (updatedRows == 0) {
                        throw new SQLException("Stoc insuficient pentru una sau mai multe flori!");
                    }

                    // Adăugăm în buchet
                    flowerStmt.setInt(1, bouquetId);
                    flowerStmt.setInt(2, flowerId);
                    flowerStmt.setInt(3, quantity);
                    flowerStmt.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this,
                        "Comanda a fost plasată cu succes!",
                        "Succes",
                        JOptionPane.INFORMATION_MESSAGE);

                if (orderCompletedListener != null) {
                    orderCompletedListener.onOrderCompleted();
                }
                Order orderData = new Order(
                        bouquetId,
                        userId,
                        totalPrice,
                        nameField.getText().trim(),
                        phoneField.getText().trim(),
                        addressField.getText().trim(),
                        notesArea.getText().trim(),
                        selectedFlowers,
                        java.time.LocalDateTime.now().toString()
                );

                OrderService orderService = new OrderService();
                boolean sentToExternalServer = orderService.sendOrderToExternalServer(orderData);

                conn.commit();

                if (sentToExternalServer) {
                    JOptionPane.showMessageDialog(this,
                            "Comanda a fost plasată cu succes și trimisă către serverul extern!",
                            "Succes",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Comanda a fost plasată local cu succes, dar nu a putut fi trimisă către serverul extern.",
                            "Atenție",
                            JOptionPane.WARNING_MESSAGE);
                }

                if (orderCompletedListener != null) {
                    orderCompletedListener.onOrderCompleted();
                }

                dispose();

            } catch (SQLException e) {
                conn.rollback();

                String message = e.getMessage();
                String errorMessage;

                if (message.contains("PRICE_LIMIT_EXCEEDED")) {
                    try {
                        String[] parts = message.split(":");
                        if (parts.length > 1) {
                            double price = Double.parseDouble(parts[1].trim());
                            errorMessage = String.format("Prețul total al buchetului (%.2f RON) nu poate depăși 500 RON!",
                                    price);
                        } else {
                            errorMessage = "Prețul total al buchetului nu poate depăși 500 RON!";
                        }
                    } catch (NumberFormatException ex) {
                        errorMessage = "Prețul total al buchetului nu poate depăși 500 RON!";
                    }
                }
                else if (message.contains("FLOWER_LIMIT_EXCEEDED")) {
                    try {
                        String[] parts = message.split(":");
                        if (parts.length > 1) {
                            int count = Integer.parseInt(parts[1].trim());
                            errorMessage = String.format("Un buchet nu poate conține mai mult de 3 tipuri de flori diferite! " +
                                    "(Ați selectat %d tipuri)", count);
                        } else {
                            errorMessage = "Un buchet nu poate conține mai mult de 3 tipuri de flori diferite!";
                        }
                    } catch (NumberFormatException ex) {
                        errorMessage = "Un buchet nu poate conține mai mult de 3 tipuri de flori diferite!";
                    }
                }
                else {
                    errorMessage = "Eroare la plasarea comenzii: " + message;
                }

                JOptionPane.showMessageDialog(this,
                        errorMessage,
                        "Eroare",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Eroare la conectarea la baza de date: " + ex.getMessage(),
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