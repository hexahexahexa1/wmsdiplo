package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.UserDto;
import com.wmsdipl.contracts.dto.UserRequest;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.getActive()
        );
    }

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        if (request.password() != null) {
            user.setPasswordHash(request.password());
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(UserRole.valueOf(request.role()));
        user.setActive(request.active());
        return user;
    }

    public void updateEntity(User user, UserRequest request) {
        if (request.username() != null) {
            user.setUsername(request.username());
        }
        if (request.password() != null) {
            user.setPasswordHash(request.password());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.setRole(UserRole.valueOf(request.role()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
    }
}
