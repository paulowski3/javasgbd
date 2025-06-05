package org.example.gui;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OrderService {
    private final HttpClient httpClient;
    private static final String EXTERNAL_SERVER_URL = "http://localhost:8081/api/orders"; // Folosește portul corect

    public OrderService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean sendOrderToExternalServer(Order order) {
        try {
            // Construim obiectul JSON pentru request
            JSONObject orderJson = new JSONObject();
            orderJson.put("userId", order.getUserId());
            orderJson.put("totalPrice", order.getTotalPrice());

            // Construim detaliile de livrare
            JSONObject deliveryDetails = new JSONObject();
            deliveryDetails.put("name", order.getDeliveryName());
            deliveryDetails.put("phone", order.getDeliveryPhone());
            deliveryDetails.put("address", order.getDeliveryAddress());
            deliveryDetails.put("notes", order.getDeliveryNotes());
            orderJson.put("deliveryDetails", deliveryDetails);

            // Adăugăm florile selectate
            // Adăugăm data curentă
            orderJson.put("orderDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXTERNAL_SERVER_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(orderJson.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}