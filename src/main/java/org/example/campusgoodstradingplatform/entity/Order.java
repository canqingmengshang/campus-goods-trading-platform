package org.example.campusgoodstradingplatform.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public long id;
    public long buyerId;
    public List<CartItem> items = new ArrayList<>();
    public BigDecimal totalAmount;
    public int pointsUsed;
    public OrderStatus status;
    public LocalDateTime paidAt;
    public LocalDateTime receivedAt;
    public String returnReason;
    public List<Review> reviews = new ArrayList<>();
}
