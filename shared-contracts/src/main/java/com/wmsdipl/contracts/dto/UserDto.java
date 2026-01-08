package com.wmsdipl.contracts.dto;

public record UserDto(Long id,
                      String username,
                      String fullName,
                      String email,
                      String role,
                      Boolean active) {
}
