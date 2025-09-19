package com.example.bankcards.enums;

public enum Role {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_%s".formatted(this.name());
    }
}
