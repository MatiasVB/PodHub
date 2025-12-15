package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.dto.ProgressRequest;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.model.User;
import org.podhub.podhub.repository.EpisodeRepository;
import org.podhub.podhub.repository.ListeningProgressRepository;
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
 * Integration tests for UserProgressController
 * Tests nested resource endpoints for user listening progress including PUT idempotency
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserProgressControllerTest {

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

    private static String testUserId;
    private static String testEpisodeId;
    private static String testProgressId;

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
    // CREATE/UPDATE PROGRESS TESTS (PUT)
    // ===========================

    @Test
    @Order(1)
    @DisplayName("PUT /api/users/{userId}/progress/{episodeId} - Create progress successfully")
    void testUpsertProgressCreate() throws Exception {
        // Create a new test episode for this progress
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Progress Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-progress.mp3");
        newEpisode.setDescription("For testing progress");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        ProgressRequest request = ProgressRequest.builder()
                .positionSeconds(120)
                .completed(false)
                .build();

        String result = mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}", testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(savedEpisode.getId()))
                .andExpect(jsonPath("$.progressSec").value(120))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ListeningProgress created = objectMapper.readValue(result, ListeningProgress.class);
        testProgressId = created.getId();
    }

    @Test
    @Order(2)
    @DisplayName("PUT /api/users/{userId}/progress/{episodeId} - Update progress (idempotent)")
    void testUpsertProgressUpdate() throws Exception {
        // Create a new episode for idempotency test
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Idempotency Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-idem.mp3");
        newEpisode.setDescription("For testing idempotency");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        ProgressRequest request1 = ProgressRequest.builder()
                .positionSeconds(60)
                .completed(false)
                .build();

        // First PUT - Create
        String firstResult = mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                                testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressSec").value(60))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ListeningProgress firstProgress = objectMapper.readValue(firstResult, ListeningProgress.class);
        String firstId = firstProgress.getId();

        // Second PUT - Update (idempotent - same request)
        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                                testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.progressSec").value(60))
                .andExpect(jsonPath("$.completed").value(false));

        // Third PUT - Update with different data
        ProgressRequest request2 = ProgressRequest.builder()
                .positionSeconds(180)
                .completed(true)
                .build();

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                                testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.progressSec").value(180))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @Order(3)
    @DisplayName("PUT /api/users/{userId}/progress/{episodeId} - Fail with invalid episodeId (404)")
    void testUpsertProgressInvalidEpisodeId() throws Exception {
        ProgressRequest request = ProgressRequest.builder()
                .positionSeconds(120)
                .completed(false)
                .build();

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                        testUserId, "invalid-episode-id-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/users/{userId}/progress/{episodeId} - Fail with negative positionSeconds (400)")
    void testUpsertProgressInvalidPosition() throws Exception {
        ProgressRequest request = ProgressRequest.builder()
                .positionSeconds(-10) // Negative position
                .completed(false)
                .build();

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                        testUserId, testEpisodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/users/{userId}/progress/{episodeId} - Fail with null positionSeconds (400)")
    void testUpsertProgressNullPosition() throws Exception {
        // Create JSON manually with null positionSeconds
        String invalidJson = "{\"completed\": false}";

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                        testUserId, testEpisodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ===========================
    // READ PROGRESS TESTS
    // ===========================

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{userId}/progress - Get user progress")
    void testGetUserProgress() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/progress", testUserId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/users/{userId}/progress - Get with cursor pagination")
    void testGetUserProgressWithCursor() throws Exception {
        // Get first page
        String firstPage = mockMvc.perform(get("/api/users/{userId}/progress", testUserId)
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cursor = objectMapper.readTree(firstPage).get("nextCursor").asText();

        // Get second page with cursor
        mockMvc.perform(get("/api/users/{userId}/progress", testUserId)
                        .param("limit", "1")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/users/{userId}/progress/{episodeId} - Get specific episode progress")
    void testGetEpisodeProgress() throws Exception {
        // First create progress
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Get Progress Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-get.mp3");
        newEpisode.setDescription("For testing get progress");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        ProgressRequest request = ProgressRequest.builder()
                .positionSeconds(90)
                .completed(false)
                .build();

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                        testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then get progress
        mockMvc.perform(get("/api/users/{userId}/progress/{episodeId}",
                        testUserId, savedEpisode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.episodeId").value(savedEpisode.getId()))
                .andExpect(jsonPath("$.progressSec").value(90))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/users/{userId}/progress/{episodeId} - Not found (404)")
    void testGetEpisodeProgressNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/progress/{episodeId}",
                        testUserId, "non-existent-episode-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // DELETE PROGRESS TESTS
    // ===========================

    @Test
    @Order(10)
    @DisplayName("DELETE /api/users/{userId}/progress/{episodeId} - Delete successfully")
    void testDeleteProgress() throws Exception {
        // First create progress
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found."));

        Episode newEpisode = new Episode();
        newEpisode.setTitle("Test Delete Progress Episode");
        newEpisode.setPodcastId(testPodcast.getId());
        newEpisode.setAudioUrl("https://example.com/audio-delete.mp3");
        newEpisode.setDescription("For testing delete progress");
        newEpisode.setDurationSec(600);
        newEpisode.setIsPublic(true);
        newEpisode.setPublishAt(Instant.now());
        Episode savedEpisode = episodeRepository.save(newEpisode);

        ProgressRequest request = ProgressRequest.builder()
                .positionSeconds(45)
                .completed(false)
                .build();

        mockMvc.perform(put("/api/users/{userId}/progress/{episodeId}",
                        testUserId, savedEpisode.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then delete
        mockMvc.perform(delete("/api/users/{userId}/progress/{episodeId}",
                        testUserId, savedEpisode.getId()))
                .andExpect(status().isNoContent());

        // Verify progress is deleted
        mockMvc.perform(delete("/api/users/{userId}/progress/{episodeId}",
                        testUserId, savedEpisode.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/users/{userId}/progress/{episodeId} - Fail when not found (404)")
    void testDeleteProgressNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/progress/{episodeId}",
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
