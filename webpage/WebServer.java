package org.example;

import spark.Spark;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class WebServer {
    private static List<Order> orders = Collections.synchronizedList(new ArrayList<>());
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        // Setăm portul
        Spark.port(8081);

        // Configurăm fișierele statice (CSS, JS, etc.)
        Spark.staticFileLocation("/public");

        // Ruta principală care servește pagina HTML
        Spark.get("/", (req, res) -> {
            res.type("text/html");
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>FloriOnline - Comenzi</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
                    <style>
                        .order-card { margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .order-header { background-color: #f8f9fa; padding: 10px; }
                        .order-body { padding: 15px; }
                    </style>
                </head>
                <body>
                    <div class="container mt-4">
                        <h1 class="mb-4">Comenzi FloriOnline</h1>
                        <div id="orders-container"></div>
                    </div>
                    
                    <script>
                        function loadOrders() {
                            fetch('/api/orders')
                                .then(response => response.json())
                                .then(orders => {
                                    const container = document.getElementById('orders-container');
                                    container.innerHTML = orders.reverse().map(order => `
                                        <div class="card order-card">
                                            <div class="order-header">
                                                <h5>Comandă #${order.orderId}</h5>
                                                <small>Data: ${order.orderDate}</small>
                                            </div>
                                            <div class="order-body">
                                                <p><strong>Client:</strong> ${order.deliveryDetails.name}</p>
                                                <p><strong>Telefon:</strong> ${order.deliveryDetails.phone}</p>
                                                <p><strong>Adresă:</strong> ${order.deliveryDetails.address}</p>
                                                <p><strong>Preț Total:</strong> ${order.totalPrice} RON</p>
                                                <hr>
                                                <h6>Produse:</h6>
                                                <ul>
                                                    ${Object.entries(order.flowers).map(([id, quantity]) => 
                                                        `<li>Floare ID ${id}: ${quantity} bucăți</li>`
                                                    ).join('')}
                                                </ul>
                                            </div>
                                        </div>
                                    `).join('');
                                });
                        }
                        
                        // Încărcăm comenzile la fiecare 30 secunde
                        loadOrders();
                        setInterval(loadOrders, 30000);
                    </script>
                </body>
                </html>
                """;
        });

        // API endpoint pentru primirea comenzilor
        Spark.post("/api/orders", (req, res) -> {
            res.type("application/json");
            Order order = gson.fromJson(req.body(), Order.class);
            orders.add(order);
            return gson.toJson(Collections.singletonMap("status", "success"));
        });

        // API endpoint pentru obținerea comenzilor
        Spark.get("/api/orders", (req, res) -> {
            res.type("application/json");
            return gson.toJson(orders);
        });
    }
}