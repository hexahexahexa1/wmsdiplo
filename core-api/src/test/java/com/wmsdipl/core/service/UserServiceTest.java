package com.wmsdipl.core.service;

import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import com.wmsdipl.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("rawPassword");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.OPERATOR);
        testUser.setActive(true);
    }

    @Test
    void shouldFindAllUsers_WhenCalled() {
        // Given
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void shouldFindUserById_WhenValidId() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.findById(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void shouldCreateUser_WhenValidUser() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowException_WhenUsernameIsBlank() {
        // Given
        testUser.setUsername("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.create(testUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowException_WhenPasswordIsBlank() {
        // Given
        testUser.setPasswordHash("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.create(testUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldSetDefaultRole_WhenRoleIsNull() {
        // Given
        testUser.setRole(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertEquals(UserRole.OPERATOR, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldSetActiveTrue_WhenActiveIsNull() {
        // Given
        testUser.setActive(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertTrue(result.getActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldNotEncodePassword_WhenAlreadyEncoded() {
        // Given
        testUser.setPasswordHash("{bcrypt}alreadyEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldUpdateUser_WhenValidData() {
        // Given
        User updatePayload = new User();
        updatePayload.setFullName("Updated Name");
        updatePayload.setEmail("updated@example.com");
        updatePayload.setRole(UserRole.ADMIN);
        updatePayload.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.update(1L, updatePayload);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", testUser.getFullName());
        assertEquals("updated@example.com", testUser.getEmail());
        assertEquals(UserRole.ADMIN, testUser.getRole());
        assertFalse(testUser.getActive());
        assertNotNull(testUser.getUpdatedAt());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldUpdatePassword_WhenValidId() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updatePassword(1L, "newPassword");

        // Then
        assertNotNull(testUser.getUpdatedAt());
        verify(passwordEncoder, times(1)).encode("newPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldDeleteUser_WhenValidId() {
        // Given
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.delete(1L);

        // Then
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldUpdateOnlyProvidedFields_WhenPartialUpdate() {
        // Given
        User updatePayload = new User();
        updatePayload.setFullName("Updated Name");
        // Other fields are null

        String originalEmail = testUser.getEmail();
        UserRole originalRole = testUser.getRole();
        Boolean originalActive = testUser.getActive();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.update(1L, updatePayload);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", testUser.getFullName());
        assertEquals(originalEmail, testUser.getEmail());
        assertEquals(originalRole, testUser.getRole());
        assertEquals(originalActive, testUser.getActive());
        verify(userRepository, times(1)).save(any(User.class));
    }
}
