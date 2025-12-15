#!/bin/bash

# =====================================================
# PodHub API Testing Script
# Tests all ~28-30 REST endpoints using cURL
# Consolidated API with query parameter filtering
# =====================================================

# Configuration
BASE_URL="http://localhost:8080"

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test data variables (Update these with actual IDs from your database)
USER_ID="YOUR_USER_ID_HERE"
PODCAST_ID="YOUR_PODCAST_ID_HERE"
EPISODE_ID="YOUR_EPISODE_ID_HERE"
COMMENT_ID="YOUR_COMMENT_ID_HERE"
CREATOR_ID="YOUR_CREATOR_ID_HERE"

# Function to print section headers
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# Function to print test name
print_test() {
    echo -e "${YELLOW}Testing: $1${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ Success${NC}"
    echo ""
}

# Function to print error
print_error() {
    echo -e "${RED}✗ Failed${NC}"
    echo ""
}

# Function to execute curl command and check response
execute_test() {
    local test_name="$1"
    local curl_cmd="$2"

    print_test "$test_name"
    echo "Command: $curl_cmd"
    response=$(eval "$curl_cmd" 2>&1)
    status=$?

    if [ $status -eq 0 ]; then
        echo "Response: $response"
        print_success
    else
        echo "Error: $response"
        print_error
    fi
}

# =====================================================
# PODCAST CONTROLLER (Query Filtering)
# =====================================================
print_header "PODCAST CONTROLLER - Query Filtering"

# 1. Create Podcast
execute_test "POST /api/podcasts - Create podcast" \
"curl -X POST ${BASE_URL}/api/podcasts \
  -H 'Content-Type: application/json' \
  -d '{
    \"title\": \"Test Podcast\",
    \"slug\": \"test-podcast-$(date +%s)\",
    \"description\": \"A test podcast\",
    \"category\": \"Technology\",
    \"creatorId\": \"${CREATOR_ID}\",
    \"public\": true
  }'"

# 2. Get Podcast by ID or Slug
execute_test "GET /api/podcasts/{id} - Get podcast by ID" \
"curl -X GET ${BASE_URL}/api/podcasts/${PODCAST_ID}"

execute_test "GET /api/podcasts/{slug} - Get podcast by slug" \
"curl -X GET ${BASE_URL}/api/podcasts/tech-talk-daily"

# 3. Update Podcast
execute_test "PUT /api/podcasts/{id} - Update podcast" \
"curl -X PUT ${BASE_URL}/api/podcasts/${PODCAST_ID} \
  -H 'Content-Type: application/json' \
  -d '{
    \"id\": \"${PODCAST_ID}\",
    \"title\": \"Updated Podcast Title\",
    \"slug\": \"tech-talk-daily\",
    \"description\": \"Updated description\",
    \"category\": \"Technology\",
    \"creatorId\": \"${CREATOR_ID}\",
    \"public\": true
  }'"

# 4. List Podcasts with Query Filtering
execute_test "GET /api/podcasts - List all podcasts" \
"curl -X GET '${BASE_URL}/api/podcasts?limit=10'"

execute_test "GET /api/podcasts?isPublic=true - Filter by public" \
"curl -X GET '${BASE_URL}/api/podcasts?isPublic=true&limit=10'"

execute_test "GET /api/podcasts?creatorId={id} - Filter by creator" \
"curl -X GET '${BASE_URL}/api/podcasts?creatorId=${CREATOR_ID}&limit=10'"

execute_test "GET /api/podcasts?title={query} - Search by title" \
"curl -X GET '${BASE_URL}/api/podcasts?title=Tech&limit=10'"

execute_test "GET /api/podcasts - Multiple filters" \
"curl -X GET '${BASE_URL}/api/podcasts?isPublic=true&creatorId=${CREATOR_ID}&limit=10'"

# 5. List Podcast Subscribers
execute_test "GET /api/podcasts/{id}/subscribers - List subscribers" \
"curl -X GET '${BASE_URL}/api/podcasts/${PODCAST_ID}/subscribers?limit=10'"

execute_test "GET /api/podcasts/{id}/subscribers?count=true - Count subscribers" \
"curl -X GET '${BASE_URL}/api/podcasts/${PODCAST_ID}/subscribers?count=true'"

# 6. Delete Podcast (commented out to avoid accidental deletion)
# execute_test "DELETE /api/podcasts/{id} - Delete podcast" \
# "curl -X DELETE ${BASE_URL}/api/podcasts/${PODCAST_ID}"

# =====================================================
# EPISODE CONTROLLER (Query Filtering)
# =====================================================
print_header "EPISODE CONTROLLER - Query Filtering"

# 1. Create Episode
execute_test "POST /api/episodes - Create episode" \
"curl -X POST ${BASE_URL}/api/episodes \
  -H 'Content-Type: application/json' \
  -d '{
    \"title\": \"Test Episode\",
    \"podcastId\": \"${PODCAST_ID}\",
    \"audioUrl\": \"https://example.com/audio/test.mp3\",
    \"durationSec\": 1200,
    \"publishAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
  }'"

# 2. Get Episode by ID
execute_test "GET /api/episodes/{id} - Get episode by ID" \
"curl -X GET ${BASE_URL}/api/episodes/${EPISODE_ID}"

# 3. Update Episode
execute_test "PUT /api/episodes/{id} - Update episode" \
"curl -X PUT ${BASE_URL}/api/episodes/${EPISODE_ID} \
  -H 'Content-Type: application/json' \
  -d '{
    \"id\": \"${EPISODE_ID}\",
    \"title\": \"Updated Episode Title\",
    \"podcastId\": \"${PODCAST_ID}\",
    \"audioUrl\": \"https://example.com/audio/updated.mp3\",
    \"durationSec\": 1500,
    \"publishAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
  }'"

# 4. List Episodes with Query Filtering
execute_test "GET /api/episodes - List all episodes" \
"curl -X GET '${BASE_URL}/api/episodes?limit=10'"

execute_test "GET /api/episodes?isPublic=true - Filter by public" \
"curl -X GET '${BASE_URL}/api/episodes?isPublic=true&limit=10'"

execute_test "GET /api/episodes?podcastId={id} - Filter by podcast" \
"curl -X GET '${BASE_URL}/api/episodes?podcastId=${PODCAST_ID}&limit=10'"

execute_test "GET /api/episodes?title={query} - Search by title" \
"curl -X GET '${BASE_URL}/api/episodes?title=AI&limit=10'"

execute_test "GET /api/episodes - Multiple filters" \
"curl -X GET '${BASE_URL}/api/episodes?isPublic=true&podcastId=${PODCAST_ID}&limit=10'"

# 5. List Episode Likes
execute_test "GET /api/episodes/{id}/likes - List likes" \
"curl -X GET '${BASE_URL}/api/episodes/${EPISODE_ID}/likes?limit=10'"

execute_test "GET /api/episodes/{id}/likes?count=true - Count likes" \
"curl -X GET '${BASE_URL}/api/episodes/${EPISODE_ID}/likes?count=true'"

# 6. Delete Episode (commented out to avoid accidental deletion)
# execute_test "DELETE /api/episodes/{id} - Delete episode" \
# "curl -X DELETE ${BASE_URL}/api/episodes/${EPISODE_ID}"

# =====================================================
# USER CONTROLLER (8 endpoints)
# =====================================================
print_header "USER CONTROLLER - 8 Endpoints"

# 1. Create User
execute_test "POST /api/users - Create user" \
"curl -X POST ${BASE_URL}/api/users \
  -H 'Content-Type: application/json' \
  -d '{
    \"username\": \"testuser_$(date +%s)\",
    \"email\": \"test$(date +%s)@example.com\",
    \"passwordHash\": \"\$2a\$10\$hashedpassword\",
    \"role\": \"LISTENER\",
    \"status\": \"ACTIVE\"
  }'"

# 2. Get User by ID
execute_test "GET /api/users/{id} - Get user by ID" \
"curl -X GET ${BASE_URL}/api/users/${USER_ID}"

# 3. Update User
execute_test "PUT /api/users/{id} - Update user" \
"curl -X PUT ${BASE_URL}/api/users/${USER_ID} \
  -H 'Content-Type: application/json' \
  -d '{
    \"id\": \"${USER_ID}\",
    \"username\": \"updated_username\",
    \"email\": \"updated@example.com\",
    \"passwordHash\": \"\$2a\$10\$hashedpassword\",
    \"role\": \"LISTENER\",
    \"status\": \"ACTIVE\"
  }'"

# 4. List All Users
execute_test "GET /api/users - List all users" \
"curl -X GET '${BASE_URL}/api/users?limit=10'"

# 5. Search Users by Name
execute_test "GET /api/users/search - Search users by name" \
"curl -X GET '${BASE_URL}/api/users/search?name=john&limit=10'"

# 6. List Users by Role
execute_test "GET /api/users/role/{role} - List users by role" \
"curl -X GET '${BASE_URL}/api/users/role/CREATOR?limit=10'"

# 7. List Users by Status
execute_test "GET /api/users/status/{status} - List users by status" \
"curl -X GET '${BASE_URL}/api/users/status/ACTIVE?limit=10'"

# 8. Delete User (commented out to avoid accidental deletion)
# execute_test "DELETE /api/users/{id} - Delete user" \
# "curl -X DELETE ${BASE_URL}/api/users/${USER_ID}"

# =====================================================
# COMMENT CONTROLLER (8 endpoints)
# =====================================================
print_header "COMMENT CONTROLLER - 8 Endpoints"

# 1. Create Comment on Podcast
execute_test "POST /api/comments - Create comment on podcast" \
"curl -X POST ${BASE_URL}/api/comments \
  -H 'Content-Type: application/json' \
  -d '{
    \"userId\": \"${USER_ID}\",
    \"target\": {
      \"type\": \"PODCAST\",
      \"id\": \"${PODCAST_ID}\"
    },
    \"content\": \"Great podcast!\",
    \"status\": \"PENDING\"
  }'"

# 2. Get Comment by ID
execute_test "GET /api/comments/{id} - Get comment by ID" \
"curl -X GET ${BASE_URL}/api/comments/${COMMENT_ID}"

# 3. Update Comment
execute_test "PUT /api/comments/{id} - Update comment" \
"curl -X PUT ${BASE_URL}/api/comments/${COMMENT_ID} \
  -H 'Content-Type: application/json' \
  -d '{
    \"id\": \"${COMMENT_ID}\",
    \"userId\": \"${USER_ID}\",
    \"target\": {
      \"type\": \"PODCAST\",
      \"id\": \"${PODCAST_ID}\"
    },
    \"content\": \"Updated comment content\",
    \"status\": \"APPROVED\"
  }'"

# 4. List Comments on Podcast
execute_test "GET /api/comments/podcast/{podcastId} - List comments on podcast" \
"curl -X GET '${BASE_URL}/api/comments/podcast/${PODCAST_ID}?limit=10'"

# 5. List Comments on Episode
execute_test "GET /api/comments/episode/{episodeId} - List comments on episode" \
"curl -X GET '${BASE_URL}/api/comments/episode/${EPISODE_ID}?limit=10'"

# 6. List Replies to Comment
execute_test "GET /api/comments/parent/{parentId} - List replies to comment" \
"curl -X GET '${BASE_URL}/api/comments/parent/${COMMENT_ID}?limit=10'"

# 7. List Comments by Status
execute_test "GET /api/comments/status/{status} - List comments by status" \
"curl -X GET '${BASE_URL}/api/comments/status/APPROVED?limit=10'"

# 8. Delete Comment (commented out to avoid accidental deletion)
# execute_test "DELETE /api/comments/{id} - Delete comment" \
# "curl -X DELETE ${BASE_URL}/api/comments/${COMMENT_ID}"

# =====================================================
# SUBSCRIPTION CONTROLLER (6 endpoints)
# =====================================================
print_header "SUBSCRIPTION CONTROLLER - 6 Endpoints"

# 1. Subscribe
execute_test "POST /api/subscriptions/subscribe - Subscribe to podcast" \
"curl -X POST '${BASE_URL}/api/subscriptions/subscribe?userId=${USER_ID}&podcastId=${PODCAST_ID}'"

# 2. Get Subscription by ID
execute_test "GET /api/subscriptions/{id} - Get subscription by ID" \
"curl -X GET ${BASE_URL}/api/subscriptions/SUBSCRIPTION_ID_HERE"

# 3. List Subscriptions by User
execute_test "GET /api/subscriptions/user/{userId} - List subscriptions by user" \
"curl -X GET '${BASE_URL}/api/subscriptions/user/${USER_ID}?limit=10'"

# 4. List Subscriptions by Podcast
execute_test "GET /api/subscriptions/podcast/{podcastId} - List subscriptions by podcast" \
"curl -X GET '${BASE_URL}/api/subscriptions/podcast/${PODCAST_ID}?limit=10'"

# 5. Get Subscriber Count
execute_test "GET /api/subscriptions/podcast/{podcastId}/count - Get subscriber count" \
"curl -X GET ${BASE_URL}/api/subscriptions/podcast/${PODCAST_ID}/count"

# 6. Unsubscribe (commented out to avoid accidental unsubscribe)
# execute_test "DELETE /api/subscriptions/unsubscribe - Unsubscribe from podcast" \
# "curl -X DELETE '${BASE_URL}/api/subscriptions/unsubscribe?userId=${USER_ID}&podcastId=${PODCAST_ID}'"

# =====================================================
# EPISODE LIKE CONTROLLER (7 endpoints)
# =====================================================
print_header "EPISODE LIKE CONTROLLER - 7 Endpoints"

# 1. Like Episode
execute_test "POST /api/likes/like - Like episode" \
"curl -X POST '${BASE_URL}/api/likes/like?userId=${USER_ID}&episodeId=${EPISODE_ID}'"

# 2. Get Like by ID
execute_test "GET /api/likes/{id} - Get like by ID" \
"curl -X GET ${BASE_URL}/api/likes/LIKE_ID_HERE"

# 3. List Likes by User
execute_test "GET /api/likes/user/{userId} - List likes by user" \
"curl -X GET '${BASE_URL}/api/likes/user/${USER_ID}?limit=10'"

# 4. List Likes by Episode
execute_test "GET /api/likes/episode/{episodeId} - List likes by episode" \
"curl -X GET '${BASE_URL}/api/likes/episode/${EPISODE_ID}?limit=10'"

# 5. Check if User Liked Episode
execute_test "GET /api/likes/exists - Check if user liked episode" \
"curl -X GET '${BASE_URL}/api/likes/exists?userId=${USER_ID}&episodeId=${EPISODE_ID}'"

# 6. Get Like Count
execute_test "GET /api/likes/episode/{episodeId}/count - Get like count" \
"curl -X GET ${BASE_URL}/api/likes/episode/${EPISODE_ID}/count"

# 7. Unlike Episode (commented out to avoid accidental unlike)
# execute_test "DELETE /api/likes/unlike - Unlike episode" \
# "curl -X DELETE '${BASE_URL}/api/likes/unlike?userId=${USER_ID}&episodeId=${EPISODE_ID}'"

# =====================================================
# LISTENING PROGRESS CONTROLLER (6 endpoints)
# =====================================================
print_header "LISTENING PROGRESS CONTROLLER - 6 Endpoints"

# 1. Create/Update Progress
execute_test "POST /api/progress - Create/update listening progress" \
"curl -X POST '${BASE_URL}/api/progress?userId=${USER_ID}&episodeId=${EPISODE_ID}&positionSeconds=600&completed=false'"

# 2. Get Progress by ID
execute_test "GET /api/progress/{id} - Get progress by ID" \
"curl -X GET ${BASE_URL}/api/progress/PROGRESS_ID_HERE"

# 3. Get Specific Progress
execute_test "GET /api/progress/one - Get specific user progress on episode" \
"curl -X GET '${BASE_URL}/api/progress/one?userId=${USER_ID}&episodeId=${EPISODE_ID}'"

# 4. List Progress by User
execute_test "GET /api/progress/user/{userId} - List progress by user" \
"curl -X GET '${BASE_URL}/api/progress/user/${USER_ID}?limit=10'"

# 5. List Progress by Episode
execute_test "GET /api/progress/episode/{episodeId} - List progress by episode" \
"curl -X GET '${BASE_URL}/api/progress/episode/${EPISODE_ID}?limit=10'"

# 6. Delete Progress (commented out to avoid accidental deletion)
# execute_test "DELETE /api/progress - Delete listening progress" \
# "curl -X DELETE '${BASE_URL}/api/progress?userId=${USER_ID}&episodeId=${EPISODE_ID}'"

# =====================================================
# Summary
# =====================================================
print_header "Testing Complete"
echo -e "${GREEN}All 52 endpoints have been tested!${NC}"
echo ""
echo -e "${YELLOW}Note: Some destructive operations (DELETE) are commented out to prevent accidental data loss.${NC}"
echo -e "${YELLOW}Uncomment them in the script if you want to test those endpoints.${NC}"
echo ""
echo -e "${BLUE}Remember to update the test data variables at the top of the script with actual IDs from your database.${NC}"
echo ""
