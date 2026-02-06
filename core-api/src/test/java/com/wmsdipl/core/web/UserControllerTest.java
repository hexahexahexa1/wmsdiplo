package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.UserDto;
import com.wmsdipl.contracts.dto.UserRequest;
import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.domain.UserRole;
import com.wmsdipl.core.mapper.UserMapper;
import com.wmsdipl.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController.
 * Tests user management REST API endpoints with security disabled.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    private User testUser;
    private UserDto testUserDto;
    private UserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.OPERATOR);
        testUser.setActive(true);

        testUserDto = new UserDto(
            1L,
            "testuser",
            "Test User",
            "test@example.com",
            "OPERATOR",
            true
        );

        createUserRequest = new UserRequest(
            "newuser",
            "password123",
            "New User",
            "new@example.com",
            "OPERATOR",
            true
        );
    }

    @Test
    void shouldListAllUsers_WhenCalled() throws Exception {
        // Given
        when(userService.findAll()).thenReturn(List.of(testUser));
        when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].fullName").value("Test User"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].role").value("OPERATOR"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(userService).findAll();
        verify(userMapper).toDto(testUser);
    }

    @Test
    void shouldGetUserById_WhenValidId() throws Exception {
        // Given
        when(userService.findById(1L)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(userService).findById(1L);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void shouldReturn404_WhenUserNotFound() throws Exception {
        // Given
        when(userService.findById(999L))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "User not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());

        verify(userService).findById(999L);
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    void shouldCreateUser_WhenValidRequest() throws Exception {
        // Given
        when(userMapper.toEntity(any(UserRequest.class))).thenReturn(testUser);
        when(userService.create(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(userMapper).toEntity(any(UserRequest.class));
        verify(userService).create(any(User.class));
        verify(userMapper).toDto(testUser);
    }

    @Test
    void shouldUpdateUser_WhenValidRequest() throws Exception {
        // Given
        UserRequest updateRequest = new UserRequest(
            "testuser",
            null, // password not updated
            "Updated Name",
            "updated@example.com",
            "SUPERVISOR",
            false
        );

        when(userService.findById(1L)).thenReturn(testUser);
        doNothing().when(userMapper).updateEntity(any(User.class), any(UserRequest.class));
        when(userService.update(eq(1L), any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(
            new UserDto(1L, "testuser", "Updated Name", "updated@example.com", "SUPERVISOR", false)
        );

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"))
                .andExpect(jsonPath("$.active").value(false));

        verify(userService).findById(1L);
        verify(userMapper).updateEntity(any(User.class), any(UserRequest.class));
        verify(userService).update(eq(1L), any(User.class));
    }

    @Test
    void shouldChangePassword_WhenValidPassword() throws Exception {
        // Given
        UserRequest passwordRequest = new UserRequest(
            null, // username
            "newPassword123", // new password
            null, null, null, null
        );

        doNothing().when(userService).updatePassword(1L, "newPassword123");

        // When & Then
        mockMvc.perform(post("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk());

        verify(userService).updatePassword(1L, "newPassword123");
    }

    @Test
    void shouldReturn400_WhenPasswordIsBlank() throws Exception {
        // Given
        UserRequest emptyPasswordRequest = new UserRequest(
            null, "", null, null, null, null
        );

        // When & Then
        mockMvc.perform(post("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void shouldReturn400_WhenPasswordIsNull() throws Exception {
        // Given
        UserRequest nullPasswordRequest = new UserRequest(
            null, null, null, null, null, null
        );

        // When & Then
        mockMvc.perform(post("/api/users/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void shouldDeleteUser_WhenValidId() throws Exception {
        // Given
        doNothing().when(userService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void shouldReturnEmptyList_WhenNoUsers() throws Exception {
        // Given
        when(userService.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userService).findAll();
    }

    @Test
    void shouldCreateAdminUser_WhenRoleIsAdmin() throws Exception {
        // Given
        UserRequest adminRequest = new UserRequest(
            "adminuser",
            "adminpass",
            "Admin User",
            "admin@example.com",
            "ADMIN",
            true
        );

        User adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setActive(true);

        UserDto adminDto = new UserDto(
            2L, "adminuser", "Admin User", "admin@example.com", "ADMIN", true
        );

        when(userMapper.toEntity(any(UserRequest.class))).thenReturn(adminUser);
        when(userService.create(any(User.class))).thenReturn(adminUser);
        when(userMapper.toDto(adminUser)).thenReturn(adminDto);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("adminuser"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(userService).create(any(User.class));
    }
}
