package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.repository.EpisodeRepository;
import org.podhub.podhub.repository.PodcastRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EpisodeController
 * Tests all 8 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EpisodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    private static String testEpisodeId;
    private static String testPodcastId;

    @BeforeAll
    static void setupTestData(@Autowired EpisodeRepository episodeRepository,
                              @Autowired PodcastRepository podcastRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        var podcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));
        testPodcastId = podcast.getId();

        var episode = episodeRepository.findFirstEpisodesByPodcast(testPodcastId, 1).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test episode not found. Please run DataSeeder first."));
        testEpisodeId = episode.getId();
    }

    // ===========================
    // CREATE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/episodes - Create episode successfully")
    void testCreateEpisode() throws Exception {
        Episode newEpisode = new Episode();
        newEpisode.setTitle("New Test Episode");
        newEpisode.setPodcastId(testPodcastId);
        newEpisode.setAudioUrl("https://example.com/audio/test.mp3");
        newEpisode.setDurationSec(1200);
        newEpisode.setPublishAt(Instant.now());

        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEpisode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Test Episode"))
                .andExpect(jsonPath("$.podcastId").value(testPodcastId))
                .andExpect(jsonPath("$.durationSec").value(1200))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/episodes - Fail with invalid data (400 Bad Request)")
    void testCreateEpisodeInvalidData() throws Exception {
        Episode invalidEpisode = new Episode();
        // Missing required fields

        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEpisode)))
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(3)
    @DisplayName("GET /api/episodes/{id} - Get episode by ID successfully")
    void testGetEpisodeById() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", testEpisodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEpisodeId))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.podcastId").exists())
                .andExpect(jsonPath("$.audioUrl").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/episodes/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetEpisodeByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/episodes/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // UPDATE TESTS
    // ===========================

    @Test
    @Order(5)
    @DisplayName("PUT /api/episodes/{id} - Update episode successfully")
    void testUpdateEpisode() throws Exception {
        Episode existingEpisode = episodeRepository.findById(testEpisodeId)
                .orElseThrow();

        existingEpisode.setTitle("Updated Episode Title");
        existingEpisode.setDurationSec(2400);

        mockMvc.perform(put("/api/episodes/{id}", testEpisodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingEpisode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEpisodeId))
                .andExpect(jsonPath("$.title").value("Updated Episode Title"))
                .andExpect(jsonPath("$.durationSec").value(2400));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/episodes/{id} - Fail with non-existent ID (404 Not Found)")
    void testUpdateEpisodeNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";
        Episode episode = new Episode();
        episode.setTitle("Should Fail");
        episode.setPodcastId(testPodcastId);
        episode.setAudioUrl("https://example.com/fail.mp3");
        episode.setDurationSec(1000);
        episode.setPublishAt(Instant.now());

        mockMvc.perform(put("/api/episodes/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(episode)))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE TESTS
    // ===========================

    @Test
    @Order(20)
    @DisplayName("DELETE /api/episodes/{id} - Delete episode successfully")
    void testDeleteEpisode() throws Exception {
        // Create an episode specifically for deletion
        Episode toDelete = new Episode();
        toDelete.setTitle("Episode to Delete");
        toDelete.setPodcastId(testPodcastId);
        toDelete.setAudioUrl("https://example.com/delete.mp3");
        toDelete.setDurationSec(900);
        toDelete.setPublishAt(Instant.now());
        toDelete.setCreatedAt(Instant.now());
        toDelete.setUpdatedAt(Instant.now());
        Episode saved = episodeRepository.save(toDelete);

        mockMvc.perform(delete("/api/episodes/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/episodes/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/episodes/{id} - Fail with non-existent ID (404 Not Found)")
    void testDeleteEpisodeNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(delete("/api/episodes/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // LIST TESTS - WITH PAGINATION
    // ===========================

    @Test
    @Order(7)
    @DisplayName("GET /api/episodes - List all episodes with pagination")
    void testGetAllEpisodes() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/episodes - List with cursor pagination")
    void testGetAllEpisodesWithCursor() throws Exception {
        // First request
        String response = mockMvc.perform(get("/api/episodes")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(3))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract nextCursor if hasMore is true
        String nextCursor = objectMapper.readTree(response).path("nextCursor").asText();
        boolean hasMore = objectMapper.readTree(response).path("hasMore").asBoolean();

        if (hasMore && !nextCursor.isEmpty()) {
            // Second request with cursor
            mockMvc.perform(get("/api/episodes")
                            .param("cursor", nextCursor)
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ===========================
    // LIST TESTS - QUERY FILTERING (NEW)
    // ===========================

    @Test
    @Order(9)
    @DisplayName("GET /api/episodes?isPublic=true - Filter by public")
    void testGetEpisodesFilterByPublic() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("isPublic", "true")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/episodes?podcastId={id} - Filter by podcast")
    void testGetEpisodesFilterByPodcast() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("podcastId", testPodcastId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].podcastId").value(testPodcastId));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/episodes?podcastId={id} - Empty list for non-existent podcast")
    void testGetEpisodesFilterByPodcastEmpty() throws Exception {
        String nonExistentPodcastId = "000000000000000000000000";

        mockMvc.perform(get("/api/episodes")
                        .param("podcastId", nonExistentPodcastId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/episodes?title={query} - Filter by title")
    void testGetEpisodesFilterByTitle() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("title", "AI")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/episodes?title={query} - Empty results for non-matching search")
    void testGetEpisodesFilterByTitleNoResults() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("title", "NonExistentEpisodeXYZ123")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/episodes?isPublic=true&podcastId={id} - Multiple filters")
    void testGetEpisodesMultipleFilters() throws Exception {
        mockMvc.perform(get("/api/episodes")
                        .param("isPublic", "true")
                        .param("podcastId", testPodcastId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ===========================
    // LIST TESTS - LIKES ENDPOINT (NEW)
    // ===========================

    @Test
    @Order(15)
    @DisplayName("GET /api/episodes/{id}/likes - List likes")
    void testGetEpisodeLikes() throws Exception {
        mockMvc.perform(get("/api/episodes/{episodeId}/likes", testEpisodeId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/episodes/{id}/likes?count=true - Count likes")
    void testGetEpisodeLikesCount() throws Exception {
        mockMvc.perform(get("/api/episodes/{episodeId}/likes", testEpisodeId)
                        .param("count", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }
}
