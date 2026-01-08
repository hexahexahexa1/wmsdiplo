package com.wmsdipl.contracts.dto;

public record UserRequest(String username,
                          String password,
                          String fullName,
                          String email,
                          String role,
                          Boolean active) {
}
