package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.UserDto;
import com.wmsdipl.contracts.dto.UserRequest;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.mapper.UserMapper;
import com.wmsdipl.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management operations for warehouse workers and administrators")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Retrieves all registered users in the warehouse management system")
    public List<UserDto> list() {
        return userService.findAll().stream().map(userMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a single user by their unique identifier")
    public UserDto get(@PathVariable Long id) {
        return userMapper.toDto(userService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user account with the provided details")
    public UserDto create(@RequestBody UserRequest request) {
        User created = userService.create(userMapper.toEntity(request));
        return userMapper.toDto(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user's details (excluding password)")
    public UserDto update(@PathVariable Long id, @RequestBody UserRequest request) {
        User user = userService.findById(id);
        userMapper.updateEntity(user, request);
        User updated = userService.update(id, user);
        return userMapper.toDto(updated);
    }

    @PostMapping("/{id}/password")
    @Operation(summary = "Change user password", description = "Updates a user's password securely")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @RequestBody UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        userService.updatePassword(id, request.password());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Removes a user from the system")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

