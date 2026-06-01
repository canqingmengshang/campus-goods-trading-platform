package org.example.campusgoodstradingplatform.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class User {
    public long id;
    public String username;
    public String password;
    public String realName;
    public String phone;
    public String email;
    public String city;
    public String gender;
    public String bankAccount;
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
