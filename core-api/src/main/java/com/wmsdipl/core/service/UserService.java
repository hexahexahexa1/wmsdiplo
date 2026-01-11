package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import com.wmsdipl.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> 
            new ResponseStatusException(NOT_FOUND, "User not found: " + id));
    }

    public User create(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Username is required");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required");
        }
        user.setPasswordHash(encodeIfNeeded(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getRole() == null) {
            user.setRole(UserRole.OPERATOR);
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }
        return userRepository.save(user);
    }

    public User update(Long id, User payload) {
        User existing = findById(id);
        if (payload.getFullName() != null) existing.setFullName(payload.getFullName());
        if (payload.getEmail() != null) existing.setEmail(payload.getEmail());
        if (payload.getRole() != null) existing.setRole(payload.getRole());
        if (payload.getActive() != null) existing.setActive(payload.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existing);
    }

    public void updatePassword(Long id, String rawPassword) {
        User existing = findById(id);
        existing.setPasswordHash(encodeIfNeeded(rawPassword));
        existing.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    private String encodeIfNeeded(String value) {
        if (value.startsWith("{")) {
            return value;
        }
        return passwordEncoder.encode(value);
    }
}
