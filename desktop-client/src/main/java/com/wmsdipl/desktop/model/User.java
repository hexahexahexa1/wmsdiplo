package com.wmsdipl.desktop.model;

public record User(
    Long id,
    String username,
    String fullName,
    String email,
    String role,
    Boolean active
) {
}
