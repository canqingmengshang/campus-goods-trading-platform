package org.example.campusgoodstradingplatform.dto;

import org.example.campusgoodstradingplatform.entity.Role;

public record RegisterRequest(
        String username,
        String password,
        String realName,
        String phone,
        String email,
        String city,
        String gender,
        String bankAccount,
        Role role,
        String shopName
) {
}
