package org.example.gui;

import org.example.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class MainView extends JFrame implements OrderCompletedListener {

    private int userId;
    private String username;
    private JPanel flowerPanel;
    private JButton orderButton;
    private JButton popularBouquetsButton;
    private boolean isAdmin;
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
        setLayout(new BorderLayout(10, 10));

        // Panel-ul de sus cu titlu
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel welcome = new JLabel("Creează-ți buchetul:", SwingConstants.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(welcome, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Panel-ul principal pentru flori
        flowerPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane(flowerPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Panel-ul de jos cu butoane
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        orderButton = new JButton("Comandă buchet");
        popularBouquetsButton = new JButton("Cele mai populare buchete");
        randomBouquetButton = new JButton("Generează buchet random");

        // Stilizăm butoanele
        orderButton.setPreferredSize(new Dimension(150, 30));
        popularBouquetsButton.setPreferredSize(new Dimension(180, 30));
        randomBouquetButton.setPreferredSize(new Dimension(180, 30));

        bottomPanel.add(orderButton);
        bottomPanel.add(popularBouquetsButton);
        bottomPanel.add(randomBouquetButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Adăugăm listeners pentru butoane
        orderButton.addActionListener(e -> placeOrder());
        popularBouquetsButton.addActionListener(e -> showPopularBouquets());
        randomBouquetButton.addActionListener(e -> generateRandomBouquet());

        // Încărcăm florile
        loadFlowers();
    }

    @Override
    public void onOrderCompleted() {
        loadFlowers();
    }

    private void loadFlowers() {
        flowerPanel.removeAll();
        quantityFields.clear();

        // Schimbăm layout-ul la GridLayout cu o singură coloană
        flowerPanel.setLayout(new GridLayout(0, 1, 5, 5));

        // Adăugăm padding în jurul panel-ului
        flowerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price, stock FROM flowers ORDER BY name ASC")) {

            while (rs.next()) {
                int flowerId = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                // Creăm un panel pentru fiecare floare
                JPanel flowerItemPanel = new JPanel();
                flowerItemPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

                // Adăugăm un border pentru a separa vizual florile
                flowerItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));

                // Creăm label-ul cu informațiile despre floare
                JLabel nameLabel = new JLabel(name);
                nameLabel.setPreferredSize(new Dimension(150, 25));
                nameLabel.setFont(new Font("Arial", Font.BOLD, 12));

                JLabel priceLabel = new JLabel(String.format("%.2f RON", price));
                priceLabel.setPreferredSize(new Dimension(80, 25));

                JLabel stockLabel = new JLabel("Stoc: " + stock);
                stockLabel.setPreferredSize(new Dimension(80, 25));

                // Creăm field-ul pentru cantitate
                JTextField quantityField = new JTextField("0");
                quantityField.setPreferredSize(new Dimension(50, 25));
                quantityFields.put(flowerId, quantityField);

                // Adăugăm spinner pentru cantitate
                SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, stock, 1);
                JSpinner quantitySpinner = new JSpinner(spinnerModel);
                quantitySpinner.setPreferredSize(new Dimension(60, 25));

                // Sincronizăm spinner-ul cu text field-ul
                quantitySpinner.addChangeListener(e -> {
                    quantityField.setText(quantitySpinner.getValue().toString());
                });

                // Adăugăm componentele în panel-ul florii
                flowerItemPanel.add(nameLabel);
                flowerItemPanel.add(priceLabel);
                flowerItemPanel.add(stockLabel);
                flowerItemPanel.add(new JLabel("Cantitate:"));
                flowerItemPanel.add(quantitySpinner);

                // Adăugăm panel-ul florii în panel-ul principal
                flowerPanel.add(flowerItemPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la încărcarea florilor: " + e.getMessage());
        }

        // Actualizăm interfața
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

            String sql = "CALL generate_random_bouquet(?, ?, ?, ?, ?)";
            CallableStatement stmt = conn.prepareCall(sql);

            stmt.setInt(1, userId);
            stmt.setInt(2, 3);
            stmt.setInt(3, 7);
            stmt.setBigDecimal(4, new BigDecimal("200.00"));
            stmt.registerOutParameter(5, Types.INTEGER);

            stmt.execute();

            int bouquetId = stmt.getInt(5);

            if (!stmt.wasNull()) {
                String selectFlowers = """
                    SELECT bf.flower_id, bf.quantity 
                    FROM bouquet_flowers bf 
                    WHERE bf.bouquet_id = ?
                """;

                PreparedStatement selectStmt = conn.prepareStatement(selectFlowers);
                selectStmt.setInt(1, bouquetId);
                ResultSet rs = selectStmt.executeQuery();

                while (rs.next()) {
                    int flowerId = rs.getInt("flower_id");
                    int quantity = rs.getInt("quantity");

                    JTextField field = quantityFields.get(flowerId);
                    if (field != null) {
                        field.setText(String.valueOf(quantity));
                        field.setBackground(new Color(255, 255, 200));
                    }
                }

                showBouquetPreview(bouquetId);

                // Ștergem buchetul temporar
                PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM bouquet_flowers WHERE bouquet_id = ?; DELETE FROM bouquets WHERE id = ?;");
                deleteStmt.setInt(1, bouquetId);
                deleteStmt.setInt(2, bouquetId);
                deleteStmt.executeUpdate();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la generarea buchetului random: " + ex.getMessage());
        }
    }

    private void showBouquetPreview(int bouquetId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = """
                SELECT f.name, bf.quantity, f.price
                FROM bouquet_flowers bf
                JOIN flowers f ON bf.flower_id = f.id
                WHERE bf.bouquet_id = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(sql);
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
            try {
                int quantity = Integer.parseInt(entry.getValue().getText().trim());
                if (quantity > 0) {
                    selectedFlowers.put(entry.getKey(), quantity);
                }
            } catch (NumberFormatException e) {
                // Ignorăm câmpurile care nu conțin numere valide
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

            DetailsView detailsView = new DetailsView(userId, selectedFlowers, this);
            detailsView.setVisible(true);

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

            JOptionPane.showMessageDialog(this, sb.toString(),
                    "Top flori", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Eroare la încărcarea buchetelor populare.");
        }
    }
}