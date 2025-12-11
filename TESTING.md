# Podhub API Testing Guide

This guide explains how to test all 52 REST API endpoints in the Podhub application using automated integration tests and manual cURL commands.

## Table of Contents
- [Overview](#overview)
- [Test Data Setup](#test-data-setup)
- [Running Integration Tests](#running-integration-tests)
- [Manual Testing with cURL](#manual-testing-with-curl)
- [Test Coverage](#test-coverage)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Podhub API testing suite includes:

- **DataSeeder**: Utility class to populate MongoDB Atlas with test data
- **7 Integration Test Classes**: Automated tests for all 52 endpoints using JUnit 5 and Spring Boot Test
- **cURL Script**: Manual testing script for all 52 endpoints

### Endpoint Summary

| Controller | Endpoints | Test Class |
|-----------|-----------|------------|
| PodcastController | 9 | `PodcastControllerTest` |
| EpisodeController | 8 | `EpisodeControllerTest` |
| UserController | 8 | `UserControllerTest` |
| CommentController | 8 | `CommentControllerTest` |
| SubscriptionController | 6 | `SubscriptionControllerTest` |
| EpisodeLikeController | 7 | `EpisodeLikeControllerTest` |
| ListeningProgressController | 6 | `ListeningProgressControllerTest` |
| **Total** | **52** | **7 test classes** |

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

- ✅ Tests all 52 endpoints
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

## Test Coverage

### Endpoint Coverage: 52/52 (100%)

#### PodcastController (9/9)
- ✅ Create podcast
- ✅ Get podcast by ID
- ✅ Get podcast by slug
- ✅ Update podcast
- ✅ Delete podcast
- ✅ List all podcasts (with pagination)
- ✅ List public podcasts
- ✅ List podcasts by creator
- ✅ Search podcasts by title

#### EpisodeController (8/8)
- ✅ Create episode
- ✅ Get episode by ID
- ✅ Update episode
- ✅ Delete episode
- ✅ List all episodes (with pagination)
- ✅ List public episodes
- ✅ List episodes by podcast
- ✅ Search episodes by title

#### UserController (8/8)
- ✅ Create user
- ✅ Get user by ID
- ✅ Update user
- ✅ Delete user
- ✅ List all users (with pagination)
- ✅ Search users by name
- ✅ List users by role
- ✅ List users by status

#### CommentController (8/8)
- ✅ Create comment
- ✅ Get comment by ID
- ✅ Update comment
- ✅ Delete comment
- ✅ List comments on podcast
- ✅ List comments on episode
- ✅ List replies to comment
- ✅ List comments by status

#### SubscriptionController (6/6)
- ✅ Subscribe to podcast
- ✅ Unsubscribe from podcast
- ✅ Get subscription by ID
- ✅ List subscriptions by user
- ✅ List subscriptions by podcast
- ✅ Get subscriber count

#### EpisodeLikeController (7/7)
- ✅ Like episode
- ✅ Unlike episode
- ✅ Get like by ID
- ✅ List likes by user
- ✅ List likes by episode
- ✅ Check if user liked episode
- ✅ Get like count

#### ListeningProgressController (6/6)
- ✅ Create/update listening progress (upsert)
- ✅ Get progress by ID
- ✅ Get specific user progress on episode
- ✅ Delete listening progress
- ✅ List progress by user
- ✅ List progress by episode

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

You now have a comprehensive testing suite for all 52 Podhub API endpoints:

1. **DataSeeder** for populating realistic test data
2. **7 Integration Test Classes** with ~80-100 automated tests
3. **cURL Script** for manual testing and exploration

Happy testing! 🚀
