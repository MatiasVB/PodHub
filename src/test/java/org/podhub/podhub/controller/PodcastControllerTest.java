package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.Podcast;
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
 * Integration tests for PodcastController
 * Tests all 9 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PodcastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PodcastRepository podcastRepository;

    private static String testPodcastId;
    private static String testCreatorId;
    private static String testSlug;

    @BeforeAll
    static void setupTestData(@Autowired PodcastRepository podcastRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        Podcast testPodcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test data not found. Please run DataSeeder first."));
        testPodcastId = testPodcast.getId();
        testCreatorId = testPodcast.getCreatorId();
        testSlug = testPodcast.getSlug();
    }

    // ===========================
    // CREATE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/podcasts - Create podcast successfully")
    void testCreatePodcast() throws Exception {
        Podcast newPodcast = new Podcast();
        newPodcast.setTitle("New Test Podcast");
        newPodcast.setSlug("new-test-podcast");
        newPodcast.setDescription("This is a test podcast");
        newPodcast.setCategory("Test");
        newPodcast.setCreatorId(testCreatorId);
        newPodcast.setIsPublic(true);

        mockMvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPodcast)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Test Podcast"))
                .andExpect(jsonPath("$.slug").value("new-test-podcast"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/podcasts - Fail with duplicate slug (409 Conflict)")
    void testCreatePodcastDuplicateSlug() throws Exception {
        Podcast duplicatePodcast = new Podcast();
        duplicatePodcast.setTitle("Another Podcast");
        duplicatePodcast.setSlug(testSlug); // Use existing slug
        duplicatePodcast.setDescription("This should fail");
        duplicatePodcast.setCategory("Test");
        duplicatePodcast.setCreatorId(testCreatorId);
        duplicatePodcast.setIsPublic(true);

        mockMvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePodcast)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/podcasts - Fail with invalid data (400 Bad Request)")
    void testCreatePodcastInvalidData() throws Exception {
        Podcast invalidPodcast = new Podcast();
        // Missing required fields like title, slug

        mockMvc.perform(post("/api/podcasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPodcast)))
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(4)
    @DisplayName("GET /api/podcasts/{id} - Get podcast by ID successfully")
    void testGetPodcastById() throws Exception {
        mockMvc.perform(get("/api/podcasts/{id}", testPodcastId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPodcastId))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.slug").value(testSlug))
                .andExpect(jsonPath("$.creatorId").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/podcasts/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetPodcastByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/podcasts/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // READ TESTS - BY SLUG
    // ===========================

    @Test
    @Order(6)
    @DisplayName("GET /api/podcasts/slug/{slug} - Get podcast by slug successfully")
    void testGetPodcastBySlug() throws Exception {
        mockMvc.perform(get("/api/podcasts/slug/{slug}", testSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(testSlug))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/podcasts/slug/{slug} - Fail with non-existent slug (404 Not Found)")
    void testGetPodcastBySlugNotFound() throws Exception {
        String nonExistentSlug = "non-existent-slug-xyz";

        mockMvc.perform(get("/api/podcasts/slug/{slug}", nonExistentSlug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ===========================
    // UPDATE TESTS
    // ===========================

    @Test
    @Order(8)
    @DisplayName("PUT /api/podcasts/{id} - Update podcast successfully")
    void testUpdatePodcast() throws Exception {
        Podcast existingPodcast = podcastRepository.findById(testPodcastId)
                .orElseThrow();

        existingPodcast.setTitle("Updated Title");
        existingPodcast.setDescription("Updated description");

        mockMvc.perform(put("/api/podcasts/{id}", testPodcastId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingPodcast)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPodcastId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @Order(9)
    @DisplayName("PUT /api/podcasts/{id} - Fail with non-existent ID (404 Not Found)")
    void testUpdatePodcastNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";
        Podcast podcast = new Podcast();
        podcast.setTitle("Should Fail");
        podcast.setSlug("should-fail");
        podcast.setCreatorId(testCreatorId);

        mockMvc.perform(put("/api/podcasts/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(podcast)))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE TESTS
    // ===========================

    @Test
    @Order(20)
    @DisplayName("DELETE /api/podcasts/{id} - Delete podcast successfully")
    void testDeletePodcast() throws Exception {
        // Create a podcast specifically for deletion
        Podcast toDelete = new Podcast();
        toDelete.setTitle("Podcast to Delete");
        toDelete.setSlug("podcast-to-delete-" + System.currentTimeMillis());
        toDelete.setDescription("Will be deleted");
        toDelete.setCategory("Test");
        toDelete.setCreatorId(testCreatorId);
        toDelete.setIsPublic(true);
        toDelete.setCreatedAt(Instant.now());
        toDelete.setUpdatedAt(Instant.now());
        Podcast saved = podcastRepository.save(toDelete);

        mockMvc.perform(delete("/api/podcasts/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/podcasts/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/podcasts/{id} - Fail with non-existent ID (404 Not Found)")
    void testDeletePodcastNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(delete("/api/podcasts/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // LIST TESTS - WITH PAGINATION
    // ===========================

    @Test
    @Order(10)
    @DisplayName("GET /api/podcasts - List all podcasts with pagination")
    void testGetAllPodcasts() throws Exception {
        mockMvc.perform(get("/api/podcasts")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/podcasts - List with cursor pagination")
    void testGetAllPodcastsWithCursor() throws Exception {
        // First request
        String response = mockMvc.perform(get("/api/podcasts")
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
            mockMvc.perform(get("/api/podcasts")
                            .param("cursor", nextCursor)
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ===========================
    // LIST TESTS - PUBLIC ONLY
    // ===========================

    @Test
    @Order(12)
    @DisplayName("GET /api/podcasts/public - List only public podcasts")
    void testGetPublicPodcasts() throws Exception {
        mockMvc.perform(get("/api/podcasts/public")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].public").value(everyItem(is(true))));
    }

    // ===========================
    // LIST TESTS - BY CREATOR
    // ===========================

    @Test
    @Order(13)
    @DisplayName("GET /api/podcasts/creator/{creatorId} - List podcasts by creator")
    void testGetPodcastsByCreator() throws Exception {
        mockMvc.perform(get("/api/podcasts/creator/{creatorId}", testCreatorId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].creatorId").value(everyItem(is(testCreatorId))));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/podcasts/creator/{creatorId} - Empty list for non-existent creator")
    void testGetPodcastsByCreatorEmpty() throws Exception {
        String nonExistentCreatorId = "000000000000000000000000";

        mockMvc.perform(get("/api/podcasts/creator/{creatorId}", nonExistentCreatorId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    // ===========================
    // SEARCH TESTS
    // ===========================

    @Test
    @Order(15)
    @DisplayName("GET /api/podcasts/search - Search podcasts by title")
    void testSearchPodcastsByTitle() throws Exception {
        mockMvc.perform(get("/api/podcasts/search")
                        .param("title", "Tech")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].title", containsStringIgnoringCase("Tech")));
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/podcasts/search - Empty results for non-matching search")
    void testSearchPodcastsNoResults() throws Exception {
        mockMvc.perform(get("/api/podcasts/search")
                        .param("title", "NonExistentPodcastXYZ123")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @Order(17)
    @DisplayName("GET /api/podcasts/search - Fail without required title parameter (400 Bad Request)")
    void testSearchPodcastsMissingTitle() throws Exception {
        mockMvc.perform(get("/api/podcasts/search")
                        .param("limit", "20"))
                .andExpect(status().isBadRequest());
    }
}
