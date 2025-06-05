package org.example.gui;

import java.util.Map;

public class Order {
    private final int orderId;
    private final int userId;
    private final double totalPrice;
    private final String deliveryName;
    private final String deliveryPhone;
    private final String deliveryAddress;
    private final String deliveryNotes;
    private final Map<Integer, Integer> flowers;
    private final String orderDate;

    public Order(int orderId, int userId, double totalPrice,
                     String deliveryName, String deliveryPhone,
                     String deliveryAddress, String deliveryNotes,
                     Map<Integer, Integer> flowers,
                     String orderDate) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.deliveryName = deliveryName;
        this.deliveryPhone = deliveryPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryNotes = deliveryNotes;
        this.flowers = flowers;
        this.orderDate = orderDate;
    }

    // Getteri
    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public double getTotalPrice() { return totalPrice; }
    public String getDeliveryName() { return deliveryName; }
    public String getDeliveryPhone() { return deliveryPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryNotes() { return deliveryNotes; }
    public Map<Integer, Integer> getFlowers() { return flowers; }
    public String getOrderDate() { return orderDate; }
}