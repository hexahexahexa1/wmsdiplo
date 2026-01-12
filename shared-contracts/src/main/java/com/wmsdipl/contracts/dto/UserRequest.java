package com.wmsdipl.contracts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
        String username,
        
        String password,  // Optional for updates, validated in service
        
        String fullName,
        
        String email,
        
        String role,
        
        Boolean active) {
}
