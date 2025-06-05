package org.example.gui;

import org.example.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginView extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    public LoginView() {
        setTitle("Login - FloriOnline");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setForeground(Color.RED);
        usernameField.setPreferredSize(new Dimension(200, 50));
        passwordField.setPreferredSize(new Dimension(200, 50));

        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Parolă:"));
        panel.add(passwordField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(messageLabel, BorderLayout.NORTH);

        loginButton.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Completează toate câmpurile.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT id, is_admin FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password); // ⚠️ În producție, folosește parole criptate

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean isAdmin = rs.getBoolean("is_admin");
                int userId = rs.getInt("id");

                messageLabel.setForeground(new Color(0, 128, 0));
                messageLabel.setText("Login reușit!");

                if (isAdmin) {
                    new AdminView().setVisible(true);
                } else {
                    new MainView(userId, username).setVisible(true);
                }

                dispose(); // Închide fereastra de login
            } else {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Date incorecte.");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            messageLabel.setText("Eroare la conectare.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }
}
