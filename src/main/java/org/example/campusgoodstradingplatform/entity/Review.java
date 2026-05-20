package org.example.campusgoodstradingplatform.entity;

import java.time.LocalDateTime;

public class Review {
    public long id;
    public long userId;
    public long merchantId;
    public Long productId;
    public int stars;
    public String content;
    public LocalDateTime createdAt;
}
