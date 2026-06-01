package org.example.campusgoodstradingplatform.mapper;

import java.math.BigDecimal;

public class ProductUpdateCommand {
    public Long id;
    public long merchantId;
    public String name;
    public String category;
    public BigDecimal originalPrice;
    public BigDecimal salePrice;
    public String size;
    public String photos;
    public String usageGuide;
    public boolean negotiable;
    public int stock;
    public String condition;
}
