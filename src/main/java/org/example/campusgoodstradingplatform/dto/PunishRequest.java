package org.example.campusgoodstradingplatform.dto;

import org.example.campusgoodstradingplatform.entity.UserStatus;

public record PunishRequest(long userId, UserStatus status) {
}
