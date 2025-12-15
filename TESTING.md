# Podhub API Testing Guide

This guide explains how to test all ~28-30 REST API endpoints in the Podhub application using automated integration tests and manual cURL commands.

## Table of Contents
- [Overview](#overview)
- [Test Data Setup](#test-data-setup)
- [Running Integration Tests](#running-integration-tests)
- [Manual Testing with cURL](#manual-testing-with-curl)
- [Nested Resource Endpoints](#nested-resource-endpoints)
- [Query Parameter Filtering](#query-parameter-filtering)
- [Test Coverage](#test-coverage)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Podhub API testing suite includes:

- **DataSeeder**: Utility class to populate MongoDB Atlas with test data
- **10 Integration Test Classes**: Automated tests for all ~28-30 endpoints using JUnit 5 and Spring Boot Test
- **cURL Script**: Manual testing script for all endpoints
- **Consolidated API**: Query parameter filtering reduces endpoint count from 52 to ~28-30

### Endpoint Summary

| Controller | Description | Test Class |
|-----------|-------------|------------|
| PodcastController | Podcast CRUD + query filtering + subscribers endpoint | `PodcastControllerTest` |
| EpisodeController | Episode CRUD + query filtering + likes endpoint | `EpisodeControllerTest` |
| UserController | User management + query filtering | `UserControllerTest` |
| CommentController | Comment management + query filtering | `CommentControllerTest` |
| UserSubscriptionController | User subscriptions (nested resource) **[NEW]** | `UserSubscriptionControllerTest` |
| UserLikeController | User likes (nested resource) **[NEW]** | `UserLikeControllerTest` |
| UserProgressController | User listening progress (nested resource) **[NEW]** | `UserProgressControllerTest` |
| **Total** | **~28-30 endpoints** | **10 test classes** |

---

## Test Data Setup

Before running tests, you need to populate your MongoDB Atlas database with test data.

### Step 1: Run the DataSeeder

The `DataSeeder` class (`src/main/java/org/podhub/podhub/util/DataSeeder.java`) creates realistic test data including:
- 6 users (admin, creators, listeners, suspended user)
- 4 podcasts (3 public, 1 private)
- 7 episodes across different podcasts
- 4 comments (including replies)
- 5 subscriptions
- 5 episode likes
- 3 listening progress records

#### Option A: Programmatic Execution

Create a simple endpoint or add to your application startup:

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DataSeeder dataSeeder;

    @PostMapping("/seed-data")
    public ResponseEntity<String> seedData() {
        dataSeeder.seedData();
        return ResponseEntity.ok("Test data seeded successfully!");
    }
}
```

Then call:
```bash
curl -X POST http://localhost:8080/api/admin/seed-data
```

#### Option B: Enable Automatic Seeding

Uncomment the `@Component` annotation in `DataSeeder.java`:

```java
@Component  // Uncomment this line
public class DataSeeder implements CommandLineRunner {
    // ...
}
```

The seeder will run automatically on application startup.

#### Option C: Manual Test Execution

Create a test class to run the seeder:

```java
@SpringBootTest
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void seedTestData() {
        dataSeeder.seedData();
    }
}
```

### Step 2: Verify Data

After seeding, verify the data in your MongoDB Atlas database:
- Users collection should have 6 documents
- Podcasts collection should have 4 documents
- Episodes collection should have 7 documents
- And so on...

**Important:** The DataSeeder will skip seeding if data already exists to prevent duplicates.

---

## Running Integration Tests

The integration tests use `@SpringBootTest` and connect to your actual MongoDB Atlas database.

### Prerequisites

1. ✅ Test data has been seeded (see above)
2. ✅ Application is properly configured (`application.properties`)
3. ✅ MongoDB Atlas connection is working

### Run All Tests

```bash
# Using Gradle
./gradlew test

# Or run a specific test class
./gradlew test --tests PodcastControllerTest

# Or run with verbose output
./gradlew test --info
```

### Run Tests in IDE

**IntelliJ IDEA:**
1. Right-click on `src/test/java` folder
2. Select "Run 'All Tests'"

**Or run individual test classes:**
1. Navigate to a test class (e.g., `PodcastControllerTest`)
2. Right-click on the class name
3. Select "Run 'PodcastControllerTest'"

### Test Structure

Each test class follows this pattern:

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PodcastControllerTest {

    @BeforeAll
    static void setupTestData() {
        // Load test data IDs from database
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/podcasts - Create podcast successfully")
    void testCreatePodcast() {
        // Test implementation
    }

    // More tests...
}
```

### Test Coverage

Each controller test class includes:

✅ **Happy Path Tests**: Successful operations (CREATE, READ, UPDATE, LIST)
✅ **Error Handling Tests**: 404 Not Found, 409 Conflict, 400 Bad Request
✅ **Pagination Tests**: Cursor-based pagination with `limit` and `cursor` parameters
✅ **Search/Filter Tests**: Query parameters and filtering by various fields

**Total Test Methods**: Approximately 80-100 test methods across all classes

---

## Manual Testing with cURL

The `test-api.sh` script provides a convenient way to manually test all endpoints.

### Setup

1. **Update Test Data Variables**

   Edit `test-api.sh` and update the variables at the top with actual IDs from your database:

   ```bash
   USER_ID="YOUR_USER_ID_HERE"
   PODCAST_ID="YOUR_PODCAST_ID_HERE"
   EPISODE_ID="YOUR_EPISODE_ID_HERE"
   COMMENT_ID="YOUR_COMMENT_ID_HERE"
   CREATOR_ID="YOUR_CREATOR_ID_HERE"
   ```

   **Tip**: You can get these IDs from MongoDB Atlas or by querying the API:
   ```bash
   curl http://localhost:8080/api/users?limit=1
   curl http://localhost:8080/api/podcasts?limit=1
   ```

2. **Make Script Executable** (Linux/Mac)

   ```bash
   chmod +x test-api.sh
   ```

### Running the Script

```bash
# Run the entire script
./test-api.sh

# Or run specific sections by editing the script
```

### Script Features

- ✅ Tests all ~28-30 endpoints (reduced from 52 via query consolidation)
- ✅ Color-coded output (Green = success, Red = error, Blue = headers)
- ✅ Organized by controller
- ✅ Shows cURL command and response for each test
- ✅ Destructive operations (DELETE) are commented out by default

### Example Output

```
========================================
PODCAST CONTROLLER - 9 Endpoints
========================================

Testing: POST /api/podcasts - Create podcast
Command: curl -X POST http://localhost:8080/api/podcasts ...
Response: {"id":"123","title":"Test Podcast",...}
✓ Success

Testing: GET /api/podcasts/{id} - Get podcast by ID
Command: curl -X GET http://localhost:8080/api/podcasts/123
Response: {"id":"123","title":"Test Podcast",...}
✓ Success
```

---

## Nested Resource Endpoints

The API uses nested resources for user-centric operations. Here are cURL examples for testing these endpoints.

### Subscriptions

**Subscribe to a podcast:**
```bash
curl -X POST http://localhost:8080/api/users/{userId}/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"podcastId": "podcast-id-here"}'
```

**List user subscriptions:**
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/subscriptions?limit=20"
```

**Unsubscribe from a podcast:**
```bash
curl -X DELETE http://localhost:8080/api/users/{userId}/subscriptions/{podcastId}
```

### Likes

**Like an episode:**
```bash
curl -X POST http://localhost:8080/api/users/{userId}/likes \
  -H "Content-Type: application/json" \
  -d '{"episodeId": "episode-id-here"}'
```

**List user likes:**
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/likes?limit=20"
```

**Check if user liked an episode (HEAD method):**
```bash
curl -I http://localhost:8080/api/users/{userId}/likes/{episodeId}
# Returns: 200 OK (if liked) or 404 Not Found (if not liked)
```

**Unlike an episode:**
```bash
curl -X DELETE http://localhost:8080/api/users/{userId}/likes/{episodeId}
```

### Listening Progress

**Update listening progress (PUT, idempotent):**
```bash
curl -X PUT http://localhost:8080/api/users/{userId}/progress/{episodeId} \
  -H "Content-Type: application/json" \
  -d '{
    "positionSeconds": 120,
    "completed": false
  }'
```

**Get user progress for all episodes:**
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/progress?limit=20"
```

**Get progress for specific episode:**
```bash
curl -X GET http://localhost:8080/api/users/{userId}/progress/{episodeId}
```

**Delete progress:**
```bash
curl -X DELETE http://localhost:8080/api/users/{userId}/progress/{episodeId}
```

---

## Query Parameter Filtering

List endpoints now use query parameters for filtering instead of separate path-based endpoints. All filters are optional and combinable.

### Podcasts

**Get all podcasts:**
```bash
curl -X GET "http://localhost:8080/api/podcasts?limit=20"
```

**Filter by public:**
```bash
curl -X GET "http://localhost:8080/api/podcasts?isPublic=true&limit=20"
```

**Filter by creator:**
```bash
curl -X GET "http://localhost:8080/api/podcasts?creatorId={creatorId}&limit=20"
```

**Search by title:**
```bash
curl -X GET "http://localhost:8080/api/podcasts?title=tech&limit=20"
```

**Combine multiple filters:**
```bash
curl -X GET "http://localhost:8080/api/podcasts?isPublic=true&creatorId={creatorId}&limit=20"
```

**Get podcast by ID or slug:**
```bash
# By ID
curl -X GET http://localhost:8080/api/podcasts/{id}

# By slug
curl -X GET http://localhost:8080/api/podcasts/my-podcast-slug
```

**List podcast subscribers:**
```bash
# Get list with pagination
curl -X GET "http://localhost:8080/api/podcasts/{podcastId}/subscribers?limit=20"

# Get count only
curl -X GET "http://localhost:8080/api/podcasts/{podcastId}/subscribers?count=true"
# Response: {"count": 42}
```

### Episodes

**Get all episodes:**
```bash
curl -X GET "http://localhost:8080/api/episodes?limit=20"
```

**Filter by public:**
```bash
curl -X GET "http://localhost:8080/api/episodes?isPublic=true&limit=20"
```

**Filter by podcast:**
```bash
curl -X GET "http://localhost:8080/api/episodes?podcastId={podcastId}&limit=20"
```

**Search by title:**
```bash
curl -X GET "http://localhost:8080/api/episodes?title=interview&limit=20"
```

**Combine multiple filters:**
```bash
curl -X GET "http://localhost:8080/api/episodes?isPublic=true&podcastId={podcastId}&limit=20"
```

**List episode likes:**
```bash
# Get list with pagination
curl -X GET "http://localhost:8080/api/episodes/{episodeId}/likes?limit=20"

# Get count only
curl -X GET "http://localhost:8080/api/episodes/{episodeId}/likes?count=true"
# Response: {"count": 15}
```

### Users

**Get all users:**
```bash
curl -X GET "http://localhost:8080/api/users?limit=20"
```

**Filter by role (role name, auto-converted to roleId):**
```bash
curl -X GET "http://localhost:8080/api/users?role=CREATOR&limit=20"
```

**Filter by status:**
```bash
curl -X GET "http://localhost:8080/api/users?status=ACTIVE&limit=20"
```

**Search by name:**
```bash
curl -X GET "http://localhost:8080/api/users?name=john&limit=20"
```

**Combine multiple filters:**
```bash
curl -X GET "http://localhost:8080/api/users?role=USER&status=ACTIVE&limit=20"
```

### Comments

**Filter by podcast:**
```bash
curl -X GET "http://localhost:8080/api/comments?podcastId={podcastId}&limit=20"
```

**Filter by episode:**
```bash
curl -X GET "http://localhost:8080/api/comments?episodeId={episodeId}&limit=20"
```

**Filter by parent (get replies):**
```bash
curl -X GET "http://localhost:8080/api/comments?parentId={commentId}&limit=20"
```

**Filter by status (ADMIN only):**
```bash
curl -X GET "http://localhost:8080/api/comments?status=APPROVED&limit=20"
```

**Note:** Comment endpoint requires at least one filter parameter. Calling without filters returns an empty list.

---

## Test Coverage

### Endpoint Coverage: ~28-30 endpoints (100%)

#### PodcastController
- ✅ Create podcast
- ✅ Get podcast by ID or slug
- ✅ Update podcast
- ✅ Delete podcast
- ✅ List podcasts with query filtering (isPublic, creatorId, title)
- ✅ List podcast subscribers (with pagination and count)

#### EpisodeController
- ✅ Create episode
- ✅ Get episode by ID
- ✅ Update episode
- ✅ Delete episode
- ✅ List episodes with query filtering (isPublic, podcastId, title)
- ✅ List episode likes (with pagination and count)

#### UserController
- ✅ Create user
- ✅ Get user by ID
- ✅ Update user
- ✅ Delete user
- ✅ List users with query filtering (name, role, status)

#### CommentController
- ✅ Create comment
- ✅ Get comment by ID
- ✅ Update comment
- ✅ Delete comment
- ✅ List comments with query filtering (podcastId, episodeId, parentId, status)

#### UserSubscriptionController (Nested Resource) **[NEW]**
- ✅ Subscribe to podcast (POST)
- ✅ List user subscriptions (GET with pagination)
- ✅ Unsubscribe from podcast (DELETE)

#### UserLikeController (Nested Resource) **[NEW]**
- ✅ Like episode (POST)
- ✅ List user likes (GET with pagination)
- ✅ Check if user liked episode (HEAD)
- ✅ Unlike episode (DELETE)

#### UserProgressController (Nested Resource) **[NEW]**
- ✅ Upsert listening progress (PUT, idempotent)
- ✅ List user progress (GET with pagination)
- ✅ Get specific episode progress (GET)
- ✅ Delete progress (DELETE)

### Test Scenario Coverage

✅ **Happy Path**: All successful operations
✅ **Error Handling**: 404 Not Found, 409 Conflict, 400 Bad Request
✅ **Pagination**: Cursor-based pagination with edge cases
✅ **Validation**: Request body validation and constraint checks

---

## Troubleshooting

### Issue: Tests Fail with "Test data not found"

**Solution**: Run the DataSeeder to populate test data (see [Test Data Setup](#test-data-setup))

```bash
# Verify data exists
curl http://localhost:8080/api/podcasts?limit=1
curl http://localhost:8080/api/users?limit=1
```

### Issue: Connection Refused

**Solution**: Ensure the application is running

```bash
./gradlew bootRun
```

Then run tests in a separate terminal.

### Issue: MongoDB Connection Error

**Solution**: Check your `application.properties` for correct MongoDB Atlas connection string

```properties
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/PodHub?retryWrites=true&w=majority
spring.data.mongodb.database=PodHub
```

### Issue: cURL Script Variables Not Set

**Solution**: Edit `test-api.sh` and replace placeholder IDs with actual IDs from your database

```bash
# Get IDs from the API
curl http://localhost:8080/api/users?limit=1
curl http://localhost:8080/api/podcasts?limit=1
curl http://localhost:8080/api/episodes?limit=1
```

### Issue: Tests Pass But Create Duplicate Data

**Solution**: The DataSeeder checks for existing data and skips if data exists. Tests create new entities with unique identifiers to avoid conflicts.

If you want to clean up test data:
1. Manually delete test entries from MongoDB Atlas
2. Or use the DataSeeder's check to prevent re-seeding

### Issue: Date Format Errors in cURL Script (Windows)

**Solution**: Windows doesn't have the `date` command in the same format. Update the script:

```bash
# Replace this:
\"publishAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"

# With a fixed timestamp:
\"publishAt\": \"2024-01-15T10:00:00Z\"
```

---

## Additional Resources

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [MongoDB Atlas Documentation](https://www.mongodb.com/docs/atlas/)
- [cURL Documentation](https://curl.se/docs/)

---

## Summary

You now have a comprehensive testing suite for all Podhub API endpoints:

1. **DataSeeder** for populating realistic test data
2. **10 Integration Test Classes** with comprehensive automated tests covering:
   - CRUD operations for all resources
   - Query parameter filtering
   - Nested resource endpoints (subscriptions, likes, progress)
   - Error scenarios and validation
3. **cURL Script** for manual testing and exploration
4. **Consolidated API** with ~28-30 endpoints (reduced from 52) via query parameter filtering

**Key Improvements:**
- Nested resources for user-centric operations
- Query parameters instead of path-based filtering
- Reduced endpoint count with more flexible filtering
- Idempotent PUT operations for progress tracking
- HEAD method for efficient existence checks

Happy testing! 🚀
