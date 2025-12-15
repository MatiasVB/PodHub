package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.dto.LikeRequest;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.model.User;
import org.podhub.podhub.repository.EpisodeLikeRepository;
import org.podhub.podhub.repository.EpisodeRepository;
import org.podhub.podhub.repository.PodcastRepository;
import org.podhub.podhub.repository.UserRepository;
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
 * Integration tests for UserLikeController
 * Tests nested resource endpoints for user likes including HEAD method
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserLikeControllerTest {

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

    private static String testUserId;
    private static String testEpisodeId;
    private static String testLikeId;

    @BeforeAll
    static void setupTestData(@Autowired UserRepository userRepository,
                               @Autowired EpisodeRepository episodeRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        User testUser = userRepository.findByEmail("listener1@podhub.com")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = testUser.getId();

        Episode testEpisode = episodeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test episode not found. Please run DataSeeder first."));
        testEpisodeId = testEpisode.getId();
    }

    // ===========================
    // CREATE LIKE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/users/{userId}/likes - Like episode successfully")
    void testLikeEpisode() throws Exception {
        // Create a new test episode for this like
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Like Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio.mp3");
        newEpisode.setDescription("For testing likes");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        LikeRequest request = LikeRequest.builder()
                .episodeId(savedEpisode.getId())
                .build();

        String result = mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(savedEpisode.getId()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EpisodeLike created = objectMapper.readValue(result, EpisodeLike.class);
        testLikeId = created.getId();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users/{userId}/likes - Fail with invalid episodeId (404)")
    void testLikeEpisodeInvalidEpisodeId() throws Exception {
        LikeRequest request = LikeRequest.builder()
                .episodeId("invalid-episode-id-12345")
                .build();

        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users/{userId}/likes - Fail with duplicate like (409 Conflict)")
    void testLikeEpisodeAlreadyLiked() throws Exception {
        LikeRequest request = LikeRequest.builder()
                .episodeId(testEpisodeId)
                .build();

        // First like should succeed
        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second like should fail with 409 Conflict
        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/users/{userId}/likes - Fail with blank episodeId (400 Bad Request)")
    void testLikeEpisodeBlankEpisodeId() throws Exception {
        LikeRequest request = LikeRequest.builder()
                .episodeId("")
                .build();

        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ===========================
    // READ LIKE TESTS
    // ===========================

    @Test
    @Order(5)
    @DisplayName("GET /api/users/{userId}/likes - Get user likes")
    void testGetUserLikes() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/likes", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{userId}/likes - Get with cursor pagination")
    void testGetUserLikesWithCursor() throws Exception {
        // Get first page
        String firstPage = mockMvc.perform(get("/api/users/{userId}/likes", testUserId)
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cursor = objectMapper.readTree(firstPage).get("nextCursor").asText();

        // Get second page with cursor
        mockMvc.perform(get("/api/users/{userId}/likes", testUserId)
                        .param("limit", "1")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ===========================
    // HEAD LIKE EXISTENCE TESTS
    // ===========================

    @Test
    @Order(7)
    @DisplayName("HEAD /api/users/{userId}/likes/{episodeId} - Check like exists (200 OK)")
    void testCheckLikeExists() throws Exception {
        // First create a like
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test HEAD Like Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-head.mp3");
        newEpisode.setDescription("For testing HEAD method");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        LikeRequest request = LikeRequest.builder()
                .episodeId(savedEpisode.getId())
                .build();

        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Check like exists with HEAD
        mockMvc.perform(head("/api/users/{userId}/likes/{episodeId}", testUserId, savedEpisode.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("HEAD /api/users/{userId}/likes/{episodeId} - Check like not exists (404 Not Found)")
    void testCheckLikeNotExists() throws Exception {
        mockMvc.perform(head("/api/users/{userId}/likes/{episodeId}",
                        testUserId, "non-existent-episode-id"))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE LIKE TESTS
    // ===========================

    @Test
    @Order(9)
    @DisplayName("DELETE /api/users/{userId}/likes/{episodeId} - Unlike successfully")
    void testUnlikeEpisode() throws Exception {
        // First like an episode
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Unlike Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-unlike.mp3");
        newEpisode.setDescription("For testing unlike");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        LikeRequest request = LikeRequest.builder()
                .episodeId(savedEpisode.getId())
                .build();

        mockMvc.perform(post("/api/users/{userId}/likes", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then unlike
        mockMvc.perform(delete("/api/users/{userId}/likes/{episodeId}", testUserId, savedEpisode.getId()))
                .andExpect(status().isNoContent());

        // Verify like is deleted
        mockMvc.perform(delete("/api/users/{userId}/likes/{episodeId}", testUserId, savedEpisode.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/users/{userId}/likes/{episodeId} - Fail when not liked (404)")
    void testUnlikeNotLiked() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/likes/{episodeId}",
                        testUserId, "non-existent-episode-id"))
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
