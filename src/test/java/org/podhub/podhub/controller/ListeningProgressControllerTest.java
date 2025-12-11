package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ListeningProgressController
 * Tests all 6 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ListeningProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListeningProgressRepository listeningProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    private static String testProgressId;
    private static String testUserId;
    private static String testEpisodeId;
    private static String anotherEpisodeId;

    @BeforeAll
    static void setupTestData(@Autowired ListeningProgressRepository listeningProgressRepository,
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

        // Find any progress for testing
        var progress = listeningProgressRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test progress not found. Please run DataSeeder first."));
        testProgressId = progress.getId();
    }

    // ===========================
    // CREATE/UPDATE (UPSERT) TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/progress - Create progress successfully")
    void testCreateProgress() throws Exception {
        // First, ensure no progress exists
        try {
            mockMvc.perform(delete("/api/progress")
                    .param("userId", testUserId)
                    .param("episodeId", anotherEpisodeId));
        } catch (Exception ignored) {}

        // Now create progress
        mockMvc.perform(post("/api/progress")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId)
                        .param("positionSeconds", "300")
                        .param("completed", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(anotherEpisodeId))
                .andExpect(jsonPath("$.positionSeconds").value(300))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/progress - Update existing progress (upsert)")
    void testUpdateProgress() throws Exception {
        // Update progress
        mockMvc.perform(post("/api/progress")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId)
                        .param("positionSeconds", "600")
                        .param("completed", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(anotherEpisodeId))
                .andExpect(jsonPath("$.positionSeconds").value(600))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/progress - Mark episode as completed")
    void testMarkEpisodeCompleted() throws Exception {
        mockMvc.perform(post("/api/progress")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId)
                        .param("positionSeconds", "1800")
                        .param("completed", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.positionSeconds").value(1800))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/progress - Fail with non-existent user (404 Not Found)")
    void testCreateProgressInvalidUser() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(post("/api/progress")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId)
                        .param("positionSeconds", "100")
                        .param("completed", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/progress - Fail with invalid position (400 Bad Request)")
    void testCreateProgressInvalidPosition() throws Exception {
        mockMvc.perform(post("/api/progress")
                        .param("userId", testUserId)
                        .param("episodeId", testEpisodeId)
                        .param("positionSeconds", "-100") // Negative position
                        .param("completed", "false"))
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(6)
    @DisplayName("GET /api/progress/{id} - Get progress by ID successfully")
    void testGetProgressById() throws Exception {
        mockMvc.perform(get("/api/progress/{id}", testProgressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProgressId))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.episodeId").exists())
                .andExpect(jsonPath("$.positionSeconds").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/progress/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetProgressByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/progress/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // READ TESTS - SPECIFIC USER/EPISODE
    // ===========================

    @Test
    @Order(8)
    @DisplayName("GET /api/progress/one - Get specific progress successfully")
    void testGetSpecificProgress() throws Exception {
        mockMvc.perform(get("/api/progress/one")
                        .param("userId", testUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(testEpisodeId))
                .andExpect(jsonPath("$.positionSeconds").exists());
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/progress/one - Fail with non-existent progress (404 Not Found)")
    void testGetSpecificProgressNotFound() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(get("/api/progress/one")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE TESTS
    // ===========================

    @Test
    @Order(20)
    @DisplayName("DELETE /api/progress - Delete progress successfully")
    void testDeleteProgress() throws Exception {
        // First ensure progress exists
        try {
            mockMvc.perform(post("/api/progress")
                    .param("userId", testUserId)
                    .param("episodeId", anotherEpisodeId)
                    .param("positionSeconds", "100")
                    .param("completed", "false"));
        } catch (Exception ignored) {}

        // Now delete
        mockMvc.perform(delete("/api/progress")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/progress/one")
                        .param("userId", testUserId)
                        .param("episodeId", anotherEpisodeId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/progress - Fail with non-existent progress (404 Not Found)")
    void testDeleteProgressNotFound() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(delete("/api/progress")
                        .param("userId", nonExistentUserId)
                        .param("episodeId", testEpisodeId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // LIST TESTS - BY USER
    // ===========================

    @Test
    @Order(10)
    @DisplayName("GET /api/progress/user/{userId} - List progress by user")
    void testGetProgressByUser() throws Exception {
        mockMvc.perform(get("/api/progress/user/{userId}", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].userId").value(everyItem(is(testUserId))));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/progress/user/{userId} - Empty list for user with no progress")
    void testGetProgressByUserEmpty() throws Exception {
        String nonExistentUserId = "000000000000000000000000";

        mockMvc.perform(get("/api/progress/user/{userId}", nonExistentUserId)
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
    @Order(12)
    @DisplayName("GET /api/progress/episode/{episodeId} - List progress by episode")
    void testGetProgressByEpisode() throws Exception {
        mockMvc.perform(get("/api/progress/episode/{episodeId}", testEpisodeId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].episodeId").value(everyItem(is(testEpisodeId))));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/progress/episode/{episodeId} - Empty list for episode with no progress")
    void testGetProgressByEpisodeEmpty() throws Exception {
        String nonExistentEpisodeId = "000000000000000000000000";

        mockMvc.perform(get("/api/progress/episode/{episodeId}", nonExistentEpisodeId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }
}
