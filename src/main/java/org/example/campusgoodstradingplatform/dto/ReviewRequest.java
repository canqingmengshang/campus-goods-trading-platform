package org.example.campusgoodstradingplatform.dto;

public record ReviewRequest(long orderId, long merchantId, Long productId, int stars, String content) {
}
