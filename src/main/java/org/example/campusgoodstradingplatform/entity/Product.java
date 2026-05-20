package org.example.campusgoodstradingplatform.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Product {
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
