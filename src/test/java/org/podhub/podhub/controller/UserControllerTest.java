package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController
 * Tests all 8 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String testUserId;
    private static String testUsername;

    @BeforeAll
    static void setupTestData(@Autowired UserRepository userRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        User testUser = userRepository.findByUsername("john_creator")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = testUser.getId();
        testUsername = testUser.getUsername();
    }

    // ===========================
    // CREATE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/users - Create user successfully")
    void testCreateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("testuser_" + System.currentTimeMillis());
        newUser.setEmail("testuser" + System.currentTimeMillis() + "@podhub.com");
        newUser.setPasswordHash("$2a$10$hashedpassword");
        newUser.setRoleIds(Set.of("test-user-role-id")); // Mock role ID for testing
        newUser.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(newUser.getUsername()))
                .andExpect(jsonPath("$.email").value(newUser.getEmail()))
                .andExpect(jsonPath("$.roleIds").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users - Fail with duplicate email (409 Conflict)")
    void testCreateUserDuplicateEmail() throws Exception {
        User duplicateUser = new User();
        duplicateUser.setUsername("newuser");
        duplicateUser.setEmail("john@podhub.com"); // Already exists
        duplicateUser.setPasswordHash("$2a$10$hashedpassword");
        duplicateUser.setRoleIds(Set.of("test-user-role-id")); // Mock role ID for testing
        duplicateUser.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users - Fail with invalid data (400 Bad Request)")
    void testCreateUserInvalidData() throws Exception {
        User invalidUser = new User();
        // Missing required fields

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(4)
    @DisplayName("GET /api/users/{id} - Get user by ID successfully")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId))
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.roleIds").exists())
                .andExpect(jsonPath("$.roleIds").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetUserByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // UPDATE TESTS
    // ===========================

    @Test
    @Order(6)
    @DisplayName("PUT /api/users/{id} - Update user successfully")
    void testUpdateUser() throws Exception {
        User existingUser = userRepository.findById(testUserId)
                .orElseThrow();

        existingUser.setUsername("updated_username_" + System.currentTimeMillis());

        mockMvc.perform(put("/api/users/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId))
                .andExpect(jsonPath("$.username").value(existingUser.getUsername()));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/users/{id} - Fail with non-existent ID (404 Not Found)")
    void testUpdateUserNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";
        User user = new User();
        user.setUsername("shouldfail");
        user.setEmail("fail@test.com");
        user.setPasswordHash("hash");
        user.setRoleIds(Set.of("test-user-role-id")); // Mock role ID for testing
        user.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(put("/api/users/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE TESTS
    // ===========================

    @Test
    @Order(20)
    @DisplayName("DELETE /api/users/{id} - Delete user successfully")
    void testDeleteUser() throws Exception {
        // Create a user specifically for deletion
        User toDelete = new User();
        toDelete.setUsername("user_to_delete_" + System.currentTimeMillis());
        toDelete.setEmail("delete" + System.currentTimeMillis() + "@test.com");
        toDelete.setPasswordHash("$2a$10$hashedpassword");
        toDelete.setRoleIds(Set.of("test-user-role-id")); // Mock role ID for testing
        toDelete.setStatus(UserStatus.ACTIVE);
        toDelete.setCreatedAt(Instant.now());
        toDelete.setUpdatedAt(Instant.now());
        User saved = userRepository.save(toDelete);

        mockMvc.perform(delete("/api/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/users/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/users/{id} - Fail with non-existent ID (404 Not Found)")
    void testDeleteUserNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(delete("/api/users/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // LIST TESTS - WITH PAGINATION
    // ===========================

    @Test
    @Order(8)
    @DisplayName("GET /api/users - List all users with pagination")
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/users - List with cursor pagination")
    void testGetAllUsersWithCursor() throws Exception {
        // First request
        String response = mockMvc.perform(get("/api/users")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(2))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract nextCursor if hasMore is true
        String nextCursor = objectMapper.readTree(response).path("nextCursor").asText();
        boolean hasMore = objectMapper.readTree(response).path("hasMore").asBoolean();

        if (hasMore && !nextCursor.isEmpty()) {
            // Second request with cursor
            mockMvc.perform(get("/api/users")
                            .param("cursor", nextCursor)
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ===========================
    // LIST TESTS - QUERY FILTERING (NEW)
    // ===========================

    @Test
    @Order(10)
    @DisplayName("GET /api/users?role={roleName} - Filter by role (CREATOR)")
    void testGetUsersFilterByRole() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "CREATOR")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].roleIds").exists());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/users?role={roleName} - Filter by role (USER)")
    void testGetUsersFilterByRoleUser() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "USER")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].roleIds").exists());
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/users?status={status} - Filter by status (ACTIVE)")
    void testGetUsersFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("status", "ACTIVE")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].status").value(everyItem(is("ACTIVE"))));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/users?status={status} - Filter by status (SUSPENDED)")
    void testGetUsersFilterByStatusSuspended() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("status", "SUSPENDED")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].status").value(everyItem(is("SUSPENDED"))));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/users?name={query} - Filter by name")
    void testGetUsersFilterByName() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("name", "john")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/users?name={query} - Empty results for non-matching search")
    void testGetUsersFilterByNameNoResults() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("name", "NonExistentUserXYZ123")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/users?role={roleName}&status={status} - Multiple filters")
    void testGetUsersMultipleFilters() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "USER")
                        .param("status", "ACTIVE")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
