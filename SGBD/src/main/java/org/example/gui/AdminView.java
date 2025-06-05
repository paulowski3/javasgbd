package org.example.gui;

import org.example.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.*;

public class AdminView extends JFrame {

    public AdminView() {
        setTitle("Admin - Gestionează Stoc");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, stock FROM flowers")) {

            while (rs.next()) {
                int flowerId = rs.getInt("id");
                String name = rs.getString("name");
                int stock = rs.getInt("stock");

                panel.add(new JLabel(name));
                JTextField stockField = new JTextField(String.valueOf(stock));
                panel.add(stockField);

                JButton updateButton = new JButton("Update");
                updateButton.addActionListener(e -> {
                    try (Connection innerConn = DBConnection.getConnection();
                         PreparedStatement ps = innerConn.prepareStatement("UPDATE flowers SET stock = ? WHERE id = ?")) {

                        int newStock = Integer.parseInt(stockField.getText());
                        ps.setInt(1, newStock);
                        ps.setInt(2, flowerId);
                        ps.executeUpdate();

                        JOptionPane.showMessageDialog(this, "Stoc actualizat pentru " + name);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Eroare la actualizarea stocului.");
                    }
                });
                panel.add(updateButton);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

