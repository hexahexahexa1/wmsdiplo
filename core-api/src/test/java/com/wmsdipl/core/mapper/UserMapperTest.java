package com.wmsdipl.core.mapper;

import com.wmsdipl.contracts.dto.UserDto;
import com.wmsdipl.contracts.dto.UserRequest;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserMapper - DTO/Entity mapping.
 */
class UserMapperTest {

    private UserMapper userMapper;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        userMapper = new UserMapper();
        
        testUser = new User();
        // Set id via reflection
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);
        
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.OPERATOR);
        testUser.setActive(true);
    }

    @Test
    void shouldMapUserToDto_WhenValidUser() {
        // When
        UserDto dto = userMapper.toDto(testUser);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("testuser", dto.username());
        assertEquals("Test User", dto.fullName());
        assertEquals("test@example.com", dto.email());
        assertEquals("OPERATOR", dto.role());
        assertTrue(dto.active());
    }

    @Test
    void shouldMapUserRequestToEntity_WhenValidRequest() {
        // Given
        UserRequest request = new UserRequest(
            "newuser",
            "password123",
            "New User",
            "new@example.com",
            "SUPERVISOR",
            true
        );

        // When
        User user = userMapper.toEntity(request);

        // Then
        assertNotNull(user);
        assertEquals("newuser", user.getUsername());
        assertEquals("password123", user.getPasswordHash());
        assertEquals("New User", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals(UserRole.SUPERVISOR, user.getRole());
        assertTrue(user.getActive());
    }

    @Test
    void shouldMapUserRequestToEntity_WhenPasswordIsNull() {
        // Given
        UserRequest request = new UserRequest(
            "newuser",
            null, // no password
            "New User",
            "new@example.com",
            "OPERATOR",
            true
        );

        // When
        User user = userMapper.toEntity(request);

        // Then
        assertNotNull(user);
        assertNull(user.getPasswordHash());
    }

    @Test
    void shouldUpdateEntity_WhenAllFieldsProvided() {
        // Given
        UserRequest request = new UserRequest(
            "updateduser",
            "newpassword",
            "Updated User",
            "updated@example.com",
            "ADMIN",
            false
        );

        // When
        userMapper.updateEntity(testUser, request);

        // Then
        assertEquals("updateduser", testUser.getUsername());
        assertEquals("newpassword", testUser.getPasswordHash());
        assertEquals("Updated User", testUser.getFullName());
        assertEquals("updated@example.com", testUser.getEmail());
        assertEquals(UserRole.ADMIN, testUser.getRole());
        assertFalse(testUser.getActive());
    }

    @Test
    void shouldUpdateOnlyProvidedFields_WhenPartialRequest() {
        // Given
        String originalUsername = testUser.getUsername();
        String originalPassword = testUser.getPasswordHash();
        String originalEmail = testUser.getEmail();
        UserRole originalRole = testUser.getRole();
        Boolean originalActive = testUser.getActive();

        UserRequest partialRequest = new UserRequest(
            null, // username not updated
            null, // password not updated
            "Updated Name Only",
            null, // email not updated
            null, // role not updated
            null  // active not updated
        );

        // When
        userMapper.updateEntity(testUser, partialRequest);

        // Then
        assertEquals(originalUsername, testUser.getUsername());
        assertEquals(originalPassword, testUser.getPasswordHash());
        assertEquals("Updated Name Only", testUser.getFullName());
        assertEquals(originalEmail, testUser.getEmail());
        assertEquals(originalRole, testUser.getRole());
        assertEquals(originalActive, testUser.getActive());
    }

    @Test
    void shouldMapAdminRole_WhenAdminRoleProvided() {
        // Given
        UserRequest request = new UserRequest(
            "admin",
            "adminpass",
            "Admin User",
            "admin@example.com",
            "ADMIN",
            true
        );

        // When
        User user = userMapper.toEntity(request);

        // Then
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void shouldMapDtoCorrectly_WhenUserHasDifferentRoles() {
        // Given - ADMIN
        testUser.setRole(UserRole.ADMIN);

        // When
        UserDto adminDto = userMapper.toDto(testUser);

        // Then
        assertEquals("ADMIN", adminDto.role());

        // Given - SUPERVISOR
        testUser.setRole(UserRole.SUPERVISOR);

        // When
        UserDto supervisorDto = userMapper.toDto(testUser);

        // Then
        assertEquals("SUPERVISOR", supervisorDto.role());
    }

    @Test
    void shouldHandleNullFullName_WhenMapping() {
        // Given
        testUser.setFullName(null);

        // When
        UserDto dto = userMapper.toDto(testUser);

        // Then
        assertNotNull(dto);
        assertNull(dto.fullName());
    }

    @Test
    void shouldHandleNullEmail_WhenMapping() {
        // Given
        testUser.setEmail(null);

        // When
        UserDto dto = userMapper.toDto(testUser);

        // Then
        assertNotNull(dto);
        assertNull(dto.email());
    }

    @Test
    void shouldHandleInactiveUser_WhenMapping() {
        // Given
        testUser.setActive(false);

        // When
        UserDto dto = userMapper.toDto(testUser);

        // Then
        assertNotNull(dto);
        assertFalse(dto.active());
    }
}
