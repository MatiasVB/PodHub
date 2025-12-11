package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EpisodeLikeController
 * Tests all 7 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EpisodeLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EpisodeLikeRepository episodeLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    private static String testLikeId;
    private static String testUserId;
    private static String testEpisodeId;
    private static String anotherEpisodeId;

    @BeforeAll
    static void setupTestData(@Autowired EpisodeLikeRepository episodeLikeRepository,
                              @Autowired UserRepository userRepository,
                              @Autowired EpisodeRepository episodeRepository,
                              @Autowired PodcastRepository podcastRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        var user = userRepository.findByUsername("alice_listener")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = user.getId();

        var podcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));

        var episodes = episodeRepository.findFirstEpisodesByPodcast(podcast.getId(), 10);
        testEpisodeId = episodes.get(0).getId();
        if (episodes.size() > 1) {
            anotherEpisodeId = episodes.get(1).getId();
        } else {
            anotherEpisodeId = testEpisodeId;
        }

        // Find any like for testing
        var like = episodeLikeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test like not found. Please run DataSeeder first."));
        testLikeId = like.getId();
    }

    // ===========================
    // LIKE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/likes/like - Like episode successfully")
    void testLikeEpisode() throws Exception {
        // First, ensure we're not already liked
        try {
            mockMvc.perform(delete("/api/likes/unlike")
                    .param("userId", testUserId)
                    .param("episodeId", anotherEpisodeId));
        } catch (Exception ignored) {}

        // Now like
        mockMvc.perform(post("/api/likes/like")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(anotherEpisodeId))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/likes/like - Fail with duplicate like (409 Conflict)")
    void testLikeDuplicate() throws Exception {
        // Try to like again
        mockMvc.perform(post("/api/likes/like")
                        .param("userId", testUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/likes/like - Fail with non-existent user (404 Not Found)")
    void testLikeInvalidUser() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(post("/api/likes/like")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // UNLIKE TESTS
    // ===========================

    @Test
    @Order(10)
    @DisplayName("DELETE /api/likes/unlike - Unlike episode successfully")
    void testUnlikeEpisode() throws Exception {
        // First ensure we're liked
        try {
            mockMvc.perform(post("/api/likes/like")
                    .param("userId", testUserId)
                    .param("episodeId", anotherEpisodeId));
        } catch (Exception ignored) {}

        // Now unlike
        mockMvc.perform(delete("/api/likes/unlike")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/likes/unlike - Fail with non-existent like (404 Not Found)")
    void testUnlikeNotFound() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(delete("/api/likes/unlike")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(4)
    @DisplayName("GET /api/likes/{id} - Get like by ID successfully")
    void testGetLikeById() throws Exception {
        mockMvc.perform(get("/api/likes/{id}", testLikeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLikeId))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.episodeId").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/likes/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetLikeByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/likes/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // LIST TESTS - BY USER
    // ===========================

    @Test
    @Order(6)
    @DisplayName("GET /api/likes/user/{userId} - List likes by user")
    void testGetLikesByUser() throws Exception {
        mockMvc.perform(get("/api/likes/user/{userId}", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].userId").value(everyItem(is(testUserId))));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/likes/user/{userId} - Empty list for user with no likes")
    void testGetLikesByUserEmpty() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(get("/api/likes/user/{userId}", nonExistentUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    // ===========================
    // LIST TESTS - BY EPISODE
    // ===========================

    @Test
    @Order(8)
    @DisplayName("GET /api/likes/episode/{episodeId} - List likes by episode")
    void testGetLikesByEpisode() throws Exception {
        mockMvc.perform(get("/api/likes/episode/{episodeId}", testEpisodeId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].episodeId").value(everyItem(is(testEpisodeId))));
    }

    // ===========================
    // EXISTS TESTS
    // ===========================

    @Test
    @Order(9)
    @DisplayName("GET /api/likes/exists - Check if user liked episode (true)")
    void testCheckLikeExists() throws Exception {
        mockMvc.perform(get("/api/likes/exists")
                        .param("userId", testUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/likes/exists - Check if user liked episode (false)")
    void testCheckLikeNotExists() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(get("/api/likes/exists")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean())
                .andExpect(jsonPath("$").value(false));
    }

    // ===========================
    // COUNT TESTS
    // ===========================

    @Test
    @Order(13)
    @DisplayName("GET /api/likes/episode/{episodeId}/count - Get like count for episode")
    void testGetLikeCount() throws Exception {
        mockMvc.perform(get("/api/likes/episode/{episodeId}/count", testEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$", greaterThanOrEqualTo(0)));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/likes/episode/{episodeId}/count - Zero count for episode with no likes")
    void testGetLikeCountZero() throws Exception {
        String nonExistentEpisodeId = "000000000000000000000000";

        mockMvc.perform(get("/api/likes/episode/{episodeId}/count", nonExistentEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }
}
