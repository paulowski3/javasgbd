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
        setLayout(new BorderLayout());

        // Panel pentru header cu butonul de logout
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Label pentru titlu Admin
        JLabel adminLabel = new JLabel("Panel Administrator");
        adminLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(adminLabel, BorderLayout.WEST);

        // Buton de logout
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> handleLogout());
        headerPanel.add(logoutButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Panel principal pentru stocuri
        JPanel mainPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadFlowerStock(mainPanel);
    }

    private void loadFlowerStock(JPanel panel) {
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
                        if (newStock < 0) {
                            JOptionPane.showMessageDialog(this,
                                    "Stocul nu poate fi negativ!",
                                    "Eroare",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        ps.setInt(1, newStock);
                        ps.setInt(2, flowerId);
                        ps.executeUpdate();

                        JOptionPane.showMessageDialog(this,
                                "Stoc actualizat pentru " + name,
                                "Succes",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Vă rugăm introduceți un număr valid!",
                                "Eroare",
                                JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                                "Eroare la actualizarea stocului: " + ex.getMessage(),
                                "Eroare",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                panel.add(updateButton);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la încărcarea stocului: " + e.getMessage(),
                    "Eroare",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout() {
        int option = JOptionPane.showConfirmDialog(this,
                "Sunteți sigur că doriți să vă deconectați?",
                "Confirmare Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            this.dispose();
            SwingUtilities.invokeLater(() -> {
                LoginView loginView = new LoginView();
                loginView.setVisible(true);
            });
        }
    }
}