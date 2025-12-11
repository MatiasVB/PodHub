package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SubscriptionController
 * Tests all 6 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriptionControllerTest {

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

    private static String testSubscriptionId;
    private static String testUserId;
    private static String testPodcastId;
    private static String anotherPodcastId;

    @BeforeAll
    static void setupTestData(@Autowired SubscriptionRepository subscriptionRepository,
                              @Autowired UserRepository userRepository,
                              @Autowired PodcastRepository podcastRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        var user = userRepository.findByUsername("alice_listener")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = user.getId();

        var podcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));
        testPodcastId = podcast.getId();

        var anotherPodcast = podcastRepository.findBySlug("the-comedy-hour")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));
        anotherPodcastId = anotherPodcast.getId();

        // Find any subscription for testing
        var subscription = subscriptionRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test subscription not found. Please run DataSeeder first."));
        testSubscriptionId = subscription.getId();
    }

    // ===========================
    // SUBSCRIBE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/subscriptions/subscribe - Subscribe successfully")
    void testSubscribe() throws Exception {
        // First, ensure we're unsubscribed
        try {
            mockMvc.perform(delete("/api/subscriptions/unsubscribe")
                    .param("userId", testUserId)
                    .param("podcastId", anotherPodcastId));
        } catch (Exception ignored) {}

        // Now subscribe
        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .param("userId", testUserId)
                        .param("podcastId", anotherPodcastId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.podcastId").value(anotherPodcastId))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/subscriptions/subscribe - Fail with duplicate subscription (409 Conflict)")
    void testSubscribeDuplicate() throws Exception {
        // Try to subscribe again to the same podcast
        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .param("userId", testUserId)
                        .param("podcastId", testPodcastId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/subscriptions/subscribe - Fail with non-existent user (404 Not Found)")
    void testSubscribeInvalidUser() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .param("userId", nonExistentUserId)
                        .param("podcastId", testPodcastId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // UNSUBSCRIBE TESTS
    // ===========================

    @Test
    @Order(10)
    @DisplayName("DELETE /api/subscriptions/unsubscribe - Unsubscribe successfully")
    void testUnsubscribe() throws Exception {
        // First ensure we're subscribed
        try {
            mockMvc.perform(post("/api/subscriptions/subscribe")
                    .param("userId", testUserId)
                    .param("podcastId", anotherPodcastId));
        } catch (Exception ignored) {}

        // Now unsubscribe
        mockMvc.perform(delete("/api/subscriptions/unsubscribe")
                        .param("userId", testUserId)
                        .param("podcastId", anotherPodcastId))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/subscriptions/unsubscribe - Fail with non-existent subscription (404 Not Found)")
    void testUnsubscribeNotFound() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(delete("/api/subscriptions/unsubscribe")
                        .param("userId", nonExistentUserId)
                        .param("podcastId", testPodcastId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(4)
    @DisplayName("GET /api/subscriptions/{id} - Get subscription by ID successfully")
    void testGetSubscriptionById() throws Exception {
        mockMvc.perform(get("/api/subscriptions/{id}", testSubscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSubscriptionId))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.podcastId").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/subscriptions/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetSubscriptionByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/subscriptions/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // LIST TESTS - BY USER
    // ===========================

    @Test
    @Order(6)
    @DisplayName("GET /api/subscriptions/user/{userId} - List subscriptions by user")
    void testGetSubscriptionsByUser() throws Exception {
        mockMvc.perform(get("/api/subscriptions/user/{userId}", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].userId").value(everyItem(is(testUserId))));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/subscriptions/user/{userId} - Empty list for user with no subscriptions")
    void testGetSubscriptionsByUserEmpty() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(get("/api/subscriptions/user/{userId}", nonExistentUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    // ===========================
    // LIST TESTS - BY PODCAST
    // ===========================

    @Test
    @Order(8)
    @DisplayName("GET /api/subscriptions/podcast/{podcastId} - List subscriptions by podcast")
    void testGetSubscriptionsByPodcast() throws Exception {
        mockMvc.perform(get("/api/subscriptions/podcast/{podcastId}", testPodcastId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].podcastId").value(everyItem(is(testPodcastId))));
    }

    // ===========================
    // COUNT TESTS
    // ===========================

    @Test
    @Order(9)
    @DisplayName("GET /api/subscriptions/podcast/{podcastId}/count - Get subscriber count")
    void testGetSubscriberCount() throws Exception {
        mockMvc.perform(get("/api/subscriptions/podcast/{podcastId}/count", testPodcastId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$", greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/subscriptions/podcast/{podcastId}/count - Zero count for podcast with no subscribers")
    void testGetSubscriberCountZero() throws Exception {
        String nonExistentPodcastId = "000000000000000000000000";

        mockMvc.perform(get("/api/subscriptions/podcast/{podcastId}/count", nonExistentPodcastId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }
}
