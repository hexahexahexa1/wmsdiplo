package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.AuthRequest;
import com.wmsdipl.contracts.dto.UserDto;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.mapper.UserMapper;
import com.wmsdipl.core.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and session management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserRepository userRepository,
                         UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user with username and password, returning user details on success")
    public ResponseEntity<UserDto> login(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            if (auth.isAuthenticated()) {
                User user = userRepository.findByUsername(request.username()).orElseThrow();
                return ResponseEntity.ok(userMapper.toDto(user));
            }
            return ResponseEntity.status(401).build();
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }
}

