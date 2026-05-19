package org.example.campusgoodstradingplatform;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CampusStoreData {
    private CampusStoreData() {
    }

    public enum Role {
        BUYER, MERCHANT, ADMIN
    }

    public enum UserStatus {
        PENDING, ACTIVE, BLACKLISTED, LIMITED
    }

    public enum ProductStatus {
        AUDITING, PUBLISHED, LOCKED, OFF_SHELF
    }

    public enum OrderStatus {
        PAID, RECEIVED, RETURN_REQUESTED
    }

    public static class User {
        public long id;
        public String username;
        public String password;
        public String phone;
        public Role role;
        public UserStatus status;
        public BigDecimal wallet;
        public int points;
        public String shopName;
        public String licenseImage;
        public String idCardImage;
        public int merchantLevel;
        public double feeRate;
        public double favorableRate;
        public List<Review> reviews = new ArrayList<>();
    }

    public static class Product {
        public long id;
        public long merchantId;
        public String merchantName;
        public String name;
        public String category;
        public BigDecimal originalPrice;
        public BigDecimal salePrice;
        public String size;
        public List<String> photos = new ArrayList<>();
        public String usageGuide;
        public boolean negotiable;
        public int stock;
        public int sales;
        public String condition;
        public ProductStatus status;
        public double favorableRate;
        public List<Review> reviews = new ArrayList<>();
    }

    public static class CartItem {
        public long productId;
        public int quantity;
        public boolean selected;
    }

    public static class Order {
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

    public static class Review {
        public long id;
        public long userId;
        public long merchantId;
        public Long productId;
        public int stars;
        public String content;
        public LocalDateTime createdAt;
    }

    public record LoginRequest(String username, String password, String captcha) {
    }

    public record RegisterRequest(
            String username,
            String password,
            String phone,
            Role role,
            String shopName
    ) {
    }

    public record ProductRequest(
            Long id,
            String name,
            String category,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            String size,
            List<String> photos,
            String usageGuide,
            boolean negotiable,
            int stock,
            String condition
    ) {
    }

    public record ReviewRequest(long orderId, long merchantId, Long productId, int stars, String content) {
    }

    public record RechargeRequest(long userId, BigDecimal amount) {
    }

    public record PunishRequest(long userId, UserStatus status) {
    }

    public record FeeRequest(long merchantId, int level) {
    }
}
