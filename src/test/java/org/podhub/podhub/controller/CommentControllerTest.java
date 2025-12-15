package org.podhub.podhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.CommentTarget;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.podhub.podhub.repository.*;
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
 * Integration tests for CommentController
 * Tests all 8 endpoints with happy path and error scenarios
 *
 * Prerequisites: Run DataSeeder to populate test data in MongoDB Atlas
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    private static String testCommentId;
    private static String testUserId;
    private static String testPodcastId;
    private static String testEpisodeId;

    @BeforeAll
    static void setupTestData(@Autowired CommentRepository commentRepository,
                              @Autowired UserRepository userRepository,
                              @Autowired PodcastRepository podcastRepository,
                              @Autowired EpisodeRepository episodeRepository) {
        // Get test data from database (assumes DataSeeder has been run)
        var user = userRepository.findByUsername("alice_listener")
                .orElseThrow(() -> new RuntimeException("Test user not found. Please run DataSeeder first."));
        testUserId = user.getId();

        var podcast = podcastRepository.findBySlug("tech-talk-daily")
                .orElseThrow(() -> new RuntimeException("Test podcast not found. Please run DataSeeder first."));
        testPodcastId = podcast.getId();

        var episode = episodeRepository.findFirstEpisodesByPodcast(testPodcastId, 1).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test episode not found. Please run DataSeeder first."));
        testEpisodeId = episode.getId();

        // Find any comment for testing
        var comment = commentRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test comment not found. Please run DataSeeder first."));
        testCommentId = comment.getId();
    }

    // ===========================
    // CREATE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("POST /api/comments - Create comment on podcast successfully")
    void testCreateCommentOnPodcast() throws Exception {
        Comment newComment = new Comment();
        newComment.setUserId(testUserId);
        CommentTarget target = new CommentTarget();
        target.setType(CommentTargetType.PODCAST);
        target.setId(testPodcastId);
        newComment.setTarget(target);
        newComment.setContent("This is a test comment on podcast");
        newComment.setStatus(CommentStatus.HIDDEN);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.content").value("This is a test comment on podcast"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/comments - Create comment on episode successfully")
    void testCreateCommentOnEpisode() throws Exception {
        Comment newComment = new Comment();
        newComment.setUserId(testUserId);
        CommentTarget target = new CommentTarget();
        target.setType(CommentTargetType.EPISODE);
        target.setId(testEpisodeId);
        newComment.setTarget(target);
        newComment.setContent("This is a test comment on episode");
        newComment.setStatus(CommentStatus.VISIBLE);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("This is a test comment on episode"))
                .andExpect(jsonPath("$.target.type").value("EPISODE"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/comments - Create reply to comment successfully")
    void testCreateReply() throws Exception {
        Comment reply = new Comment();
        reply.setUserId(testUserId);
        CommentTarget target = new CommentTarget();
        target.setType(CommentTargetType.EPISODE);
        target.setId(testEpisodeId);
        reply.setTarget(target);
        reply.setContent("This is a reply to a comment");
        reply.setParentId(testCommentId);
        reply.setStatus(CommentStatus.VISIBLE);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.parentId").value(testCommentId))
                .andExpect(jsonPath("$.content").value("This is a reply to a comment"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/comments - Fail with invalid data (400 Bad Request)")
    void testCreateCommentInvalidData() throws Exception {
        Comment invalidComment = new Comment();
        // Missing required fields

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidComment)))
                .andExpect(status().isBadRequest());
    }

    // ===========================
    // READ TESTS - BY ID
    // ===========================

    @Test
    @Order(5)
    @DisplayName("GET /api/comments/{id} - Get comment by ID successfully")
    void testGetCommentById() throws Exception {
        mockMvc.perform(get("/api/comments/{id}", testCommentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCommentId))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.target").exists());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/comments/{id} - Fail with non-existent ID (404 Not Found)")
    void testGetCommentByIdNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(get("/api/comments/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ===========================
    // UPDATE TESTS
    // ===========================

    @Test
    @Order(7)
    @DisplayName("PUT /api/comments/{id} - Update comment successfully")
    void testUpdateComment() throws Exception {
        Comment existingComment = commentRepository.findById(testCommentId)
                .orElseThrow();

        existingComment.setContent("Updated comment content");
        existingComment.setStatus(CommentStatus.VISIBLE);

        mockMvc.perform(put("/api/comments/{id}", testCommentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCommentId))
                .andExpect(jsonPath("$.content").value("Updated comment content"));
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/comments/{id} - Fail with non-existent ID (404 Not Found)")
    void testUpdateCommentNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";
        Comment comment = new Comment();
        comment.setUserId(testUserId);
        CommentTarget target = new CommentTarget();
        target.setType(CommentTargetType.PODCAST);
        target.setId(testPodcastId);
        comment.setTarget(target);
        comment.setContent("Should fail");
        comment.setStatus(CommentStatus.HIDDEN);

        mockMvc.perform(put("/api/comments/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE TESTS
    // ===========================

    @Test
    @Order(20)
    @DisplayName("DELETE /api/comments/{id} - Delete comment successfully")
    void testDeleteComment() throws Exception {
        // Create a comment specifically for deletion
        Comment toDelete = new Comment();
        toDelete.setUserId(testUserId);
        CommentTarget target = new CommentTarget();
        target.setType(CommentTargetType.PODCAST);
        target.setId(testPodcastId);
        toDelete.setTarget(target);
        toDelete.setContent("Comment to delete");
        toDelete.setStatus(CommentStatus.HIDDEN);
        toDelete.setCreatedAt(Instant.now());
        toDelete.setEditedAt(Instant.now());
        Comment saved = commentRepository.save(toDelete);

        mockMvc.perform(delete("/api/comments/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/comments/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(21)
    @DisplayName("DELETE /api/comments/{id} - Fail with non-existent ID (404 Not Found)")
    void testDeleteCommentNotFound() throws Exception {
        String nonExistentId = "000000000000000000000000";

        mockMvc.perform(delete("/api/comments/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // LIST TESTS - QUERY FILTERING (NEW)
    // ===========================

    @Test
    @Order(9)
    @DisplayName("GET /api/comments?podcastId={id} - Filter by podcast")
    void testGetCommentsFilterByPodcast() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("podcastId", testPodcastId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/comments?episodeId={id} - Filter by episode")
    void testGetCommentsFilterByEpisode() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("episodeId", testEpisodeId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/comments?parentId={id} - Filter by parent (replies)")
    void testGetCommentsFilterByParent() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("parentId", testCommentId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/comments?status={status} - Filter by status (APPROVED)")
    void testGetCommentsFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("status", "APPROVED")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].status").value(everyItem(is("APPROVED"))));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/comments?status={status} - Filter by status (PENDING)")
    void testGetCommentsFilterByStatusPending() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("status", "PENDING")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/comments - No filters returns empty list")
    void testGetCommentsNoFilters() throws Exception {
        mockMvc.perform(get("/api/comments")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }
}
