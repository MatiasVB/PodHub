package org.podhub.podhub.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.*;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class to seed the MongoDB database with test data.
 * Can be run on application startup or manually triggered.
 *
 * To enable automatic seeding on startup, uncomment the @Component annotation.
 * To run manually, call the seed() method from a REST endpoint or test.
 */
@Slf4j
@RequiredArgsConstructor
// @Component  // Uncomment to run automatically on startup
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PodcastRepository podcastRepository;
    private final EpisodeRepository episodeRepository;
    private final CommentRepository commentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EpisodeLikeRepository episodeLikeRepository;
    private final ListeningProgressRepository listeningProgressRepository;

    // Store IDs for cross-referencing
    private final List<String> userIds = new ArrayList<>();
    private final List<String> podcastIds = new ArrayList<>();
    private final List<String> episodeIds = new ArrayList<>();
    private final List<String> commentIds = new ArrayList<>();

    @Override
    public void run(String... args) {
        // Only run if explicitly enabled
        log.info("DataSeeder: Use seedData() method to populate test data");
    }

    /**
     * Main method to seed all test data.
     * Call this method to populate the database with test data.
     */
    public void seedData() {
        log.info("Starting data seeding...");

        // Check if data already exists
        if (userRepository.count() > 0) {
            log.warn("Database already contains data. Skipping seeding to avoid duplicates.");
            log.info("If you want to re-seed, please clear the database first.");
            return;
        }

        seedUsers();
        seedPodcasts();
        seedEpisodes();
        seedComments();
        seedSubscriptions();
        seedLikes();
        seedListeningProgress();

        log.info("Data seeding completed successfully!");
        log.info("Created: {} users, {} podcasts, {} episodes, {} comments",
                userIds.size(), podcastIds.size(), episodeIds.size(), commentIds.size());
    }

    private void seedUsers() {
        log.debug("Seeding users...");

        // Pre-load roles
        Role adminRole = roleRepository.findByName(UserRole.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found. Run DataInitializer first."));
        Role creatorRole = roleRepository.findByName(UserRole.CREATOR)
                .orElseThrow(() -> new RuntimeException("Creator role not found. Run DataInitializer first."));
        Role userRole = roleRepository.findByName(UserRole.USER)
                .orElseThrow(() -> new RuntimeException("User role not found. Run DataInitializer first."));

        // Admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@podhub.com");
        admin.setPasswordHash("$2a$10$hashedpassword"); // In real app, use BCrypt
        admin.setRoleIds(Set.of(adminRole.getId()));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(Instant.now().minusSeconds(86400 * 30)); // 30 days ago
        admin.setUpdatedAt(admin.getCreatedAt());
        admin = userRepository.save(admin);
        userIds.add(admin.getId());

        // Creator users
        User creator1 = new User();
        creator1.setUsername("john_creator");
        creator1.setEmail("john@podhub.com");
        creator1.setPasswordHash("$2a$10$hashedpassword");
        creator1.setRoleIds(Set.of(creatorRole.getId()));
        creator1.setStatus(UserStatus.ACTIVE);
        creator1.setCreatedAt(Instant.now().minusSeconds(86400 * 25));
        creator1.setUpdatedAt(creator1.getCreatedAt());
        creator1 = userRepository.save(creator1);
        userIds.add(creator1.getId());

        User creator2 = new User();
        creator2.setUsername("jane_creator");
        creator2.setEmail("jane@podhub.com");
        creator2.setPasswordHash("$2a$10$hashedpassword");
        creator2.setRoleIds(Set.of(creatorRole.getId()));
        creator2.setStatus(UserStatus.ACTIVE);
        creator2.setCreatedAt(Instant.now().minusSeconds(86400 * 20));
        creator2.setUpdatedAt(creator2.getCreatedAt());
        creator2 = userRepository.save(creator2);
        userIds.add(creator2.getId());

        // Listener users
        User listener1 = new User();
        listener1.setUsername("alice_listener");
        listener1.setEmail("alice@podhub.com");
        listener1.setPasswordHash("$2a$10$hashedpassword");
        listener1.setRoleIds(Set.of(userRole.getId()));
        listener1.setStatus(UserStatus.ACTIVE);
        listener1.setCreatedAt(Instant.now().minusSeconds(86400 * 15));
        listener1.setUpdatedAt(listener1.getCreatedAt());
        listener1 = userRepository.save(listener1);
        userIds.add(listener1.getId());

        User listener2 = new User();
        listener2.setUsername("bob_listener");
        listener2.setEmail("bob@podhub.com");
        listener2.setPasswordHash("$2a$10$hashedpassword");
        listener2.setRoleIds(Set.of(userRole.getId()));
        listener2.setStatus(UserStatus.ACTIVE);
        listener2.setCreatedAt(Instant.now().minusSeconds(86400 * 10));
        listener2.setUpdatedAt(listener2.getCreatedAt());
        listener2 = userRepository.save(listener2);
        userIds.add(listener2.getId());

        // Suspended user
        User suspended = new User();
        suspended.setUsername("suspended_user");
        suspended.setEmail("suspended@podhub.com");
        suspended.setPasswordHash("$2a$10$hashedpassword");
        suspended.setRoleIds(Set.of(userRole.getId()));
        suspended.setStatus(UserStatus.SUSPENDED);
        suspended.setCreatedAt(Instant.now().minusSeconds(86400 * 5));
        suspended.setUpdatedAt(Instant.now());
        suspended = userRepository.save(suspended);
        userIds.add(suspended.getId());

        log.debug("Created {} users", userIds.size());
    }

    private void seedPodcasts() {
        log.debug("Seeding podcasts...");

        String creator1Id = userIds.get(1); // john_creator
        String creator2Id = userIds.get(2); // jane_creator

        // Public podcasts
        Podcast tech = new Podcast();
        tech.setTitle("Tech Talk Daily");
        tech.setSlug("tech-talk-daily");
        tech.setDescription("Daily discussions about the latest in technology");
        tech.setCategory("Technology");
        tech.setCreatorId(creator1Id);
        tech.setIsPublic(true);
        tech.setCreatedAt(Instant.now().minusSeconds(86400 * 20));
        tech.setUpdatedAt(tech.getCreatedAt());
        tech = podcastRepository.save(tech);
        podcastIds.add(tech.getId());

        Podcast business = new Podcast();
        business.setTitle("Business Insights");
        business.setSlug("business-insights");
        business.setDescription("Insights from successful entrepreneurs and business leaders");
        business.setCategory("Business");
        business.setCreatorId(creator1Id);
        business.setIsPublic(true);
        business.setCreatedAt(Instant.now().minusSeconds(86400 * 18));
        business.setUpdatedAt(business.getCreatedAt());
        business = podcastRepository.save(business);
        podcastIds.add(business.getId());

        Podcast comedy = new Podcast();
        comedy.setTitle("The Comedy Hour");
        comedy.setSlug("the-comedy-hour");
        comedy.setDescription("Stand-up comedy and funny stories");
        comedy.setCategory("Comedy");
        comedy.setCreatorId(creator2Id);
        comedy.setIsPublic(true);
        comedy.setCreatedAt(Instant.now().minusSeconds(86400 * 15));
        comedy.setUpdatedAt(comedy.getCreatedAt());
        comedy = podcastRepository.save(comedy);
        podcastIds.add(comedy.getId());

        // Private podcast
        Podcast premium = new Podcast();
        premium.setTitle("Premium Content");
        premium.setSlug("premium-content");
        premium.setDescription("Exclusive content for subscribers only");
        premium.setCategory("Education");
        premium.setCreatorId(creator2Id);
        premium.setIsPublic(false);
        premium.setCreatedAt(Instant.now().minusSeconds(86400 * 10));
        premium.setUpdatedAt(premium.getCreatedAt());
        premium = podcastRepository.save(premium);
        podcastIds.add(premium.getId());

        log.debug("Created {} podcasts", podcastIds.size());
    }

    private void seedEpisodes() {
        log.debug("Seeding episodes...");

        String podcast1Id = podcastIds.get(0); // Tech Talk Daily
        String podcast2Id = podcastIds.get(1); // Business Insights
        String podcast3Id = podcastIds.get(2); // The Comedy Hour

        // Episodes for Tech Talk Daily
        Episode ep1 = new Episode();
        ep1.setTitle("Introduction to AI and Machine Learning");
        ep1.setPodcastId(podcast1Id);
        ep1.setAudioUrl("https://example.com/audio/ep1.mp3");
        ep1.setDurationSec(1800);
        ep1.setPublishAt(Instant.now().minusSeconds(86400 * 15));
        ep1.setCreatedAt(ep1.getPublishAt());
        ep1.setUpdatedAt(ep1.getCreatedAt());
        ep1 = episodeRepository.save(ep1);
        episodeIds.add(ep1.getId());

        Episode ep2 = new Episode();
        ep2.setTitle("Cloud Computing Trends 2024");
        ep2.setPodcastId(podcast1Id);
        ep2.setAudioUrl("https://example.com/audio/ep2.mp3");
        ep2.setDurationSec(2100);
        ep2.setPublishAt(Instant.now().minusSeconds(86400 * 10));
        ep2.setCreatedAt(ep2.getPublishAt());
        ep2.setUpdatedAt(ep2.getCreatedAt());
        ep2 = episodeRepository.save(ep2);
        episodeIds.add(ep2.getId());

        Episode ep3 = new Episode();
        ep3.setTitle("Cybersecurity Best Practices");
        ep3.setPodcastId(podcast1Id);
        ep3.setAudioUrl("https://example.com/audio/ep3.mp3");
        ep3.setDurationSec(1950);
        ep3.setPublishAt(Instant.now().minusSeconds(86400 * 5));
        ep3.setCreatedAt(ep3.getPublishAt());
        ep3.setUpdatedAt(ep3.getCreatedAt());
        ep3 = episodeRepository.save(ep3);
        episodeIds.add(ep3.getId());

        // Episodes for Business Insights
        Episode ep4 = new Episode();
        ep4.setTitle("Startup Funding Strategies");
        ep4.setPodcastId(podcast2Id);
        ep4.setAudioUrl("https://example.com/audio/ep4.mp3");
        ep4.setDurationSec(2400);
        ep4.setPublishAt(Instant.now().minusSeconds(86400 * 12));
        ep4.setCreatedAt(ep4.getPublishAt());
        ep4.setUpdatedAt(ep4.getCreatedAt());
        ep4 = episodeRepository.save(ep4);
        episodeIds.add(ep4.getId());

        Episode ep5 = new Episode();
        ep5.setTitle("Leadership in the Modern Workplace");
        ep5.setPodcastId(podcast2Id);
        ep5.setAudioUrl("https://example.com/audio/ep5.mp3");
        ep5.setDurationSec(2700);
        ep5.setPublishAt(Instant.now().minusSeconds(86400 * 7));
        ep5.setCreatedAt(ep5.getPublishAt());
        ep5.setUpdatedAt(ep5.getCreatedAt());
        ep5 = episodeRepository.save(ep5);
        episodeIds.add(ep5.getId());

        // Episodes for The Comedy Hour
        Episode ep6 = new Episode();
        ep6.setTitle("Life's Awkward Moments");
        ep6.setPodcastId(podcast3Id);
        ep6.setAudioUrl("https://example.com/audio/ep6.mp3");
        ep6.setDurationSec(1500);
        ep6.setPublishAt(Instant.now().minusSeconds(86400 * 8));
        ep6.setCreatedAt(ep6.getPublishAt());
        ep6.setUpdatedAt(ep6.getCreatedAt());
        ep6 = episodeRepository.save(ep6);
        episodeIds.add(ep6.getId());

        Episode ep7 = new Episode();
        ep7.setTitle("Travel Mishaps and Adventures");
        ep7.setPodcastId(podcast3Id);
        ep7.setAudioUrl("https://example.com/audio/ep7.mp3");
        ep7.setDurationSec(1650);
        ep7.setPublishAt(Instant.now().minusSeconds(86400 * 3));
        ep7.setCreatedAt(ep7.getPublishAt());
        ep7.setUpdatedAt(ep7.getCreatedAt());
        ep7 = episodeRepository.save(ep7);
        episodeIds.add(ep7.getId());

        log.debug("Created {} episodes", episodeIds.size());
    }

    private void seedComments() {
        log.debug("Seeding comments...");

        String listener1Id = userIds.get(3); // alice_listener
        String listener2Id = userIds.get(4); // bob_listener
        String podcast1Id = podcastIds.get(0);
        String episode1Id = episodeIds.get(0);
        String episode2Id = episodeIds.get(1);

        // Comment on podcast
        Comment comment1 = new Comment();
        comment1.setUserId(listener1Id);
        CommentTarget target1 = new CommentTarget();
        target1.setType(CommentTargetType.PODCAST);
        target1.setId(podcast1Id);
        comment1.setTarget(target1);
        comment1.setContent("Great podcast! Love the content.");
        comment1.setStatus(CommentStatus.VISIBLE);
        comment1.setCreatedAt(Instant.now().minusSeconds(86400 * 10));
        comment1.setEditedAt(comment1.getCreatedAt());
        comment1 = commentRepository.save(comment1);
        commentIds.add(comment1.getId());

        // Comment on episode
        Comment comment2 = new Comment();
        comment2.setUserId(listener2Id);
        CommentTarget target2 = new CommentTarget();
        target2.setType(CommentTargetType.EPISODE);
        target2.setId(episode1Id);
        comment2.setTarget(target2);
        comment2.setContent("Very informative episode!");
        comment2.setStatus(CommentStatus.VISIBLE);
        comment2.setCreatedAt(Instant.now().minusSeconds(86400 * 8));
        comment2.setEditedAt(comment2.getCreatedAt());
        comment2 = commentRepository.save(comment2);
        commentIds.add(comment2.getId());

        // Reply to comment
        Comment reply1 = new Comment();
        reply1.setUserId(listener1Id);
        reply1.setTarget(target2);
        reply1.setContent("I agree! Can't wait for the next one.");
        reply1.setParentId(comment2.getId());
        reply1.setStatus(CommentStatus.VISIBLE);
        reply1.setCreatedAt(Instant.now().minusSeconds(86400 * 7));
        reply1.setEditedAt(reply1.getCreatedAt());
        reply1 = commentRepository.save(reply1);
        commentIds.add(reply1.getId());

        // Pending comment
        Comment comment3 = new Comment();
        comment3.setUserId(listener2Id);
        CommentTarget target3 = new CommentTarget();
        target3.setType(CommentTargetType.EPISODE);
        target3.setId(episode2Id);
        comment3.setTarget(target3);
        comment3.setContent("This is awaiting moderation...");
        comment3.setStatus(CommentStatus.HIDDEN);
        comment3.setCreatedAt(Instant.now().minusSeconds(86400 * 2));
        comment3.setEditedAt(comment3.getCreatedAt());
        comment3 = commentRepository.save(comment3);
        commentIds.add(comment3.getId());

        log.debug("Created {} comments", commentIds.size());
    }

    private void seedSubscriptions() {
        log.debug("Seeding subscriptions...");

        String listener1Id = userIds.get(3); // alice_listener
        String listener2Id = userIds.get(4); // bob_listener
        String podcast1Id = podcastIds.get(0); // Tech Talk Daily
        String podcast2Id = podcastIds.get(1); // Business Insights
        String podcast3Id = podcastIds.get(2); // The Comedy Hour

        // Alice subscribes to Tech Talk Daily and Business Insights
        Subscription sub1 = new Subscription();
        sub1.setUserId(listener1Id);
        sub1.setPodcastId(podcast1Id);
        sub1.setCreatedAt(Instant.now().minusSeconds(86400 * 12));
        subscriptionRepository.save(sub1);

        Subscription sub2 = new Subscription();
        sub2.setUserId(listener1Id);
        sub2.setPodcastId(podcast2Id);
        sub2.setCreatedAt(Instant.now().minusSeconds(86400 * 9));
        subscriptionRepository.save(sub2);

        // Bob subscribes to all three podcasts
        Subscription sub3 = new Subscription();
        sub3.setUserId(listener2Id);
        sub3.setPodcastId(podcast1Id);
        sub3.setCreatedAt(Instant.now().minusSeconds(86400 * 8));
        subscriptionRepository.save(sub3);

        Subscription sub4 = new Subscription();
        sub4.setUserId(listener2Id);
        sub4.setPodcastId(podcast2Id);
        sub4.setCreatedAt(Instant.now().minusSeconds(86400 * 6));
        subscriptionRepository.save(sub4);

        Subscription sub5 = new Subscription();
        sub5.setUserId(listener2Id);
        sub5.setPodcastId(podcast3Id);
        sub5.setCreatedAt(Instant.now().minusSeconds(86400 * 4));
        subscriptionRepository.save(sub5);

        log.debug("Created 5 subscriptions");
    }

    private void seedLikes() {
        log.debug("Seeding episode likes...");

        String listener1Id = userIds.get(3); // alice_listener
        String listener2Id = userIds.get(4); // bob_listener
        String episode1Id = episodeIds.get(0);
        String episode2Id = episodeIds.get(1);
        String episode3Id = episodeIds.get(2);
        String episode4Id = episodeIds.get(3);

        // Alice likes episodes 1, 2, 3
        EpisodeLike like1 = new EpisodeLike();
        like1.setUserId(listener1Id);
        like1.setEpisodeId(episode1Id);
        like1.setCreatedAt(Instant.now().minusSeconds(86400 * 10));
        episodeLikeRepository.save(like1);

        EpisodeLike like2 = new EpisodeLike();
        like2.setUserId(listener1Id);
        like2.setEpisodeId(episode2Id);
        like2.setCreatedAt(Instant.now().minusSeconds(86400 * 8));
        episodeLikeRepository.save(like2);

        EpisodeLike like3 = new EpisodeLike();
        like3.setUserId(listener1Id);
        like3.setEpisodeId(episode3Id);
        like3.setCreatedAt(Instant.now().minusSeconds(86400 * 4));
        episodeLikeRepository.save(like3);

        // Bob likes episodes 1, 4
        EpisodeLike like4 = new EpisodeLike();
        like4.setUserId(listener2Id);
        like4.setEpisodeId(episode1Id);
        like4.setCreatedAt(Instant.now().minusSeconds(86400 * 9));
        episodeLikeRepository.save(like4);

        EpisodeLike like5 = new EpisodeLike();
        like5.setUserId(listener2Id);
        like5.setEpisodeId(episode4Id);
        like5.setCreatedAt(Instant.now().minusSeconds(86400 * 5));
        episodeLikeRepository.save(like5);

        log.debug("Created 5 episode likes");
    }

    private void seedListeningProgress() {
        log.debug("Seeding listening progress...");

        String listener1Id = userIds.get(3); // alice_listener
        String listener2Id = userIds.get(4); // bob_listener
        String episode1Id = episodeIds.get(0); // 1800 sec
        String episode2Id = episodeIds.get(1); // 2100 sec
        String episode4Id = episodeIds.get(3); // 2400 sec

        // Alice's progress - episode 1 completed
        ListeningProgress progress1 = new ListeningProgress();
        progress1.setUserId(listener1Id);
        progress1.setEpisodeId(episode1Id);
        progress1.setPositionSeconds(1800);
        progress1.setCompleted(true);
        progress1.setCreatedAt(Instant.now().minusSeconds(86400 * 10));
        progress1.setUpdatedAt(Instant.now().minusSeconds(86400 * 10));
        listeningProgressRepository.save(progress1);

        // Alice's progress - episode 2 in progress (50%)
        ListeningProgress progress2 = new ListeningProgress();
        progress2.setUserId(listener1Id);
        progress2.setEpisodeId(episode2Id);
        progress2.setPositionSeconds(1050);
        progress2.setCompleted(false);
        progress2.setCreatedAt(Instant.now().minusSeconds(86400 * 5));
        progress2.setUpdatedAt(Instant.now().minusSeconds(3600)); // Updated 1 hour ago
        listeningProgressRepository.save(progress2);

        // Bob's progress - episode 4 in progress (75%)
        ListeningProgress progress3 = new ListeningProgress();
        progress3.setUserId(listener2Id);
        progress3.setEpisodeId(episode4Id);
        progress3.setPositionSeconds(1800);
        progress3.setCompleted(false);
        progress3.setCreatedAt(Instant.now().minusSeconds(86400 * 6));
        progress3.setUpdatedAt(Instant.now().minusSeconds(7200)); // Updated 2 hours ago
        listeningProgressRepository.save(progress3);

        log.debug("Created 3 listening progress records");
    }
}
