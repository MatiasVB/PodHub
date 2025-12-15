package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.dto.SubscriptionRequest;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.model.User;
import org.podhub.podhub.repository.PodcastRepository;
import org.podhub.podhub.repository.SubscriptionRepository;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserSubscriptionController
 * Tests nested resource endpoints for user subscriptions
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    private static String testUserId;
    private static String testPodcastId;
    private static String testSubscriptionId;

    @BeforeAll
    static void setupTestData(@Autowired UserRepository userRepository,
                               @Autowired PodcastRepository podcastRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        User testUser = userRepository.findByEmail("listener1@podhub.com")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = testUser.getId();

        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));
        testPodcastId = testPodcast.getId();
    }

    // ===========================
    // CREATE SUBSCRIPTION TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/users/{userId}/subscriptions - Subscribe successfully")
    void testSubscribeToPodcast() throws Exception {
        // Create a new test podcast for this subscription
        Podcast newPodcast = new Podcast();
        newPodcast.setTitle("Test Subscription Podcast");
        newPodcast.setSlug("test-subscription-podcast");
        newPodcast.setDescription("For testing subscriptions");
        newPodcast.setCategory("Test");
        newPodcast.setCreatorId(testUserId);
        newPodcast.setIsPublic(true);
        Podcast savedPodcast = podcastRepository.save(newPodcast);

        SubscriptionRequest request = SubscriptionRequest.builder()
                .podcastId(savedPodcast.getId())
                .build();

        String result = mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.podcastId").value(savedPodcast.getId()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Subscription created = objectMapper.readValue(result, Subscription.class);
        testSubscriptionId = created.getId();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users/{userId}/subscriptions - Fail with invalid podcastId (404)")
    void testSubscribeToPodcastInvalidPodcastId() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .podcastId("invalid-podcast-id-12345")
                .build();

        mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users/{userId}/subscriptions - Fail with duplicate subscription (409 Conflict)")
    void testSubscribeToPodcastAlreadySubscribed() throws Exception {
        // Try to subscribe to the podcast we subscribed to in test 1
        SubscriptionRequest request = SubscriptionRequest.builder()
                .podcastId(testPodcastId)
                .build();

        // First subscription should succeed
        mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second subscription should fail with 409 Conflict
        mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/users/{userId}/subscriptions - Fail with blank podcastId (400 Bad Request)")
    void testSubscribeToPodcastBlankPodcastId() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .podcastId("")
                .build();

        mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ===========================
    // READ SUBSCRIPTION TESTS
    // ===========================

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{userId}/subscriptions - Get user subscriptions")
    void testGetUserSubscriptions() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/subscriptions", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{userId}/subscriptions - Get with cursor pagination")
    void testGetUserSubscriptionsWithCursor() throws Exception {
        // Get first page
        String firstPage = mockMvc.perform(get("/api/users/{userId}/subscriptions", testUserId)
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cursor = objectMapper.readTree(firstPage).get("nextCursor").asText();

        // Get second page with cursor
        mockMvc.perform(get("/api/users/{userId}/subscriptions", testUserId)
                        .param("limit", "1")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ===========================
    // DELETE SUBSCRIPTION TESTS
    // ===========================

    @Test
    @Order(7)
    @DisplayName("DELETE /api/users/{userId}/subscriptions/{podcastId} - Unsubscribe successfully")
    void testUnsubscribeFromPodcast() throws Exception {
        // First subscribe to a podcast
        Podcast newPodcast = new Podcast();
        newPodcast.setTitle("Test Unsubscribe Podcast");
        newPodcast.setSlug("test-unsubscribe-podcast");
        newPodcast.setDescription("For testing unsubscribe");
        newPodcast.setCategory("Test");
        newPodcast.setCreatorId(testUserId);
        newPodcast.setIsPublic(true);
        Podcast savedPodcast = podcastRepository.save(newPodcast);

        SubscriptionRequest request = SubscriptionRequest.builder()
                .podcastId(savedPodcast.getId())
                .build();

        mockMvc.perform(post("/api/users/{userId}/subscriptions", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then unsubscribe
        mockMvc.perform(delete("/api/users/{userId}/subscriptions/{podcastId}", testUserId, savedPodcast.getId()))
                .andExpect(status().isNoContent());

        // Verify subscription is deleted
        mockMvc.perform(delete("/api/users/{userId}/subscriptions/{podcastId}", testUserId, savedPodcast.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/users/{userId}/subscriptions/{podcastId} - Fail when not subscribed (404)")
    void testUnsubscribeNotSubscribed() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/subscriptions/{podcastId}",
                        testUserId, "non-existent-podcast-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // AUTHENTICATION TESTS
    // ===========================

    // Note: These tests would require authentication to be enabled
    // Currently security is disabled in application.properties
    // When security is enabled, add @PreAuthorize tests here
}
