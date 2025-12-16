import axiosInstance from './axiosInstance';
import type {
  AuthRequest,
  AuthResponse,
  RegisterRequest,
  UserResponse,
  PaginatedResponse,
  Podcast,
  CreatePodcastRequest,
  Episode,
  CreateEpisodeRequest,
  Comment,
  CreateCommentRequest,
  Subscription,
  EpisodeLike,
  ListeningProgress,
  CountResponse,
} from './types';

// ==================== Auth API ====================
export const authAPI = {
  login: (data: AuthRequest) => 
    axiosInstance.post<AuthResponse>('/auth/login', data),
  
  register: (data: RegisterRequest) => 
    axiosInstance.post<UserResponse>('/auth/register', data),
  
  refresh: (refreshToken: string) => 
    axiosInstance.post<AuthResponse>('/auth/refresh', { refreshToken }),
};

// ==================== Users API ====================
export const usersAPI = {
  getUser: (id: string) =>
    axiosInstance.get<UserResponse>(`/users/${id}`),

  updateUser: (id: string, data: any) =>
    axiosInstance.put<UserResponse>(`/users/${id}`, data),

  // v3.0: Unified endpoint with query filters - GET /users?name=...&role=...&status=...
  listUsers: (params?: {
    name?: string;
    role?: string;
    status?: string;
    cursor?: string;
    limit?: number;
  }) =>
    axiosInstance.get<PaginatedResponse<UserResponse>>('/users', {
      params: { limit: 20, ...params },
    }),

  // Convenience method for searching by name (backward compatibility)
  searchUsers: (name: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<UserResponse>>('/users', {
      params: { name, cursor, limit },
    }),
};

// ==================== Podcasts API ====================
export const podcastsAPI = {
  createPodcast: (data: CreatePodcastRequest) =>
    axiosInstance.post<Podcast>('/podcasts', data),

  getPodcast: (id: string) =>
    axiosInstance.get<Podcast>(`/podcasts/${id}`),

  getPodcastBySlug: (slug: string) =>
    axiosInstance.get<Podcast>(`/podcasts/slug/${slug}`),

  updatePodcast: (id: string, data: Partial<CreatePodcastRequest>) =>
    axiosInstance.put<Podcast>(`/podcasts/${id}`, data),

  deletePodcast: (id: string) =>
    axiosInstance.delete(`/podcasts/${id}`),

  // v3.0: Unified endpoint with query filters - GET /podcasts?isPublic=...&creatorId=...&title=...
  listPodcasts: (params?: {
    isPublic?: boolean;
    creatorId?: string;
    title?: string;
    cursor?: string;
    limit?: number;
  }) =>
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts', {
      params: { limit: 20, ...params },
    }),

  // Convenience methods for backward compatibility
  listPublicPodcasts: (cursor?: string, limit: number = 50) =>
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts', {
      params: { isPublic: true, cursor, limit },
    }),

  listPodcastsByCreator: (creatorId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts', {
      params: { creatorId, cursor, limit },
    }),

  searchPodcasts: (title: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts', {
      params: { title, cursor, limit },
    }),
};

// ==================== Episodes API ====================
export const episodesAPI = {
  createEpisode: (data: CreateEpisodeRequest) =>
    axiosInstance.post<Episode>('/episodes', data),

  getEpisode: (id: string) =>
    axiosInstance.get<Episode>(`/episodes/${id}`),

  updateEpisode: (id: string, data: Partial<CreateEpisodeRequest>) =>
    axiosInstance.put<Episode>(`/episodes/${id}`, data),

  deleteEpisode: (id: string) =>
    axiosInstance.delete(`/episodes/${id}`),

  // v3.0: Unified endpoint with query filters - GET /episodes?isPublic=...&podcastId=...&title=...
  listEpisodes: (params?: {
    isPublic?: boolean;
    podcastId?: string;
    title?: string;
    cursor?: string;
    limit?: number;
  }) =>
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes', {
      params: { limit: 20, ...params },
    }),

  // Convenience methods for backward compatibility
  listPublicEpisodes: (cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes', {
      params: { isPublic: true, cursor, limit },
    }),

  listEpisodesByPodcast: (podcastId: string, cursor?: string, limit: number = 50) =>
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes', {
      params: { podcastId, cursor, limit },
    }),

  searchEpisodes: (title: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes', {
      params: { title, cursor, limit },
    }),
};

// ==================== Comments API ====================
export const commentsAPI = {
  createComment: (data: CreateCommentRequest) =>
    axiosInstance.post<Comment>('/comments', data),

  getComment: (id: string) =>
    axiosInstance.get<Comment>(`/comments/${id}`),

  updateComment: (id: string, data: Partial<CreateCommentRequest>) =>
    axiosInstance.put<Comment>(`/comments/${id}`, data),

  deleteComment: (id: string) =>
    axiosInstance.delete(`/comments/${id}`),

  // v3.0: Unified endpoint with query filters - GET /comments?podcastId=...&episodeId=...&parentId=...
  listComments: (params: {
    podcastId?: string;
    episodeId?: string;
    parentId?: string;
    status?: string;
    cursor?: string;
    limit?: number;
  }) =>
    axiosInstance.get<PaginatedResponse<Comment>>('/comments', {
      params: { limit: 50, ...params },
    }),

  // Convenience methods for backward compatibility
  listCommentsByPodcast: (podcastId: string, cursor?: string, limit: number = 50) =>
    axiosInstance.get<PaginatedResponse<Comment>>('/comments', {
      params: { podcastId, cursor, limit },
    }),

  listCommentsByEpisode: (episodeId: string, cursor?: string, limit: number = 50) =>
    axiosInstance.get<PaginatedResponse<Comment>>('/comments', {
      params: { episodeId, cursor, limit },
    }),

  listCommentsByParent: (parentId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Comment>>('/comments', {
      params: { parentId, cursor, limit },
    }),
};

// ==================== Subscriptions API ====================
export const subscriptionsAPI = {
  // v3.0: Nested resource - POST /users/{userId}/subscriptions
  subscribe: (userId: string, podcastId: string) =>
    axiosInstance.post<Subscription>(`/users/${userId}/subscriptions`, { podcastId }),

  // v3.0: Nested resource - DELETE /users/{userId}/subscriptions/{podcastId}
  unsubscribe: (userId: string, podcastId: string) =>
    axiosInstance.delete(`/users/${userId}/subscriptions/${podcastId}`),

  // v3.0: GET /users/{userId}/subscriptions
  listSubscriptionsByUser: (userId: string, cursor?: string, limit: number = 200) =>
    axiosInstance.get<PaginatedResponse<Subscription>>(`/users/${userId}/subscriptions`, {
      params: { cursor, limit },
    }),

  // v3.0: Secondary endpoint - GET /podcasts/{podcastId}/subscribers
  listSubscriptionsByPodcast: (podcastId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<Subscription>>(`/podcasts/${podcastId}/subscribers`, {
      params: { cursor, limit },
    }),

  // v3.0: Count endpoint - GET /podcasts/{podcastId}/subscribers?count=true
  getSubscriptionCount: (podcastId: string) =>
    axiosInstance.get<CountResponse>(`/podcasts/${podcastId}/subscribers`, {
      params: { count: true },
    }),
};

// ==================== Likes API ====================
export const likesAPI = {
  // v3.0: Nested resource - POST /users/{userId}/likes
  like: (userId: string, episodeId: string) =>
    axiosInstance.post<EpisodeLike>(`/users/${userId}/likes`, { episodeId }),

  // v3.0: Nested resource - DELETE /users/{userId}/likes/{episodeId}
  unlike: (userId: string, episodeId: string) =>
    axiosInstance.delete(`/users/${userId}/likes/${episodeId}`),

  // v3.0: GET /users/{userId}/likes
  listLikesByUser: (userId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<EpisodeLike>>(`/users/${userId}/likes`, {
      params: { cursor, limit },
    }),

  // v3.0: Secondary endpoint - GET /episodes/{episodeId}/likes
  listLikesByEpisode: (episodeId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<EpisodeLike>>(`/episodes/${episodeId}/likes`, {
      params: { cursor, limit },
    }),

  // v3.0: HEAD request - HEAD /users/{userId}/likes/{episodeId} (returns 200 OK or 404)
  checkLikeExists: (userId: string, episodeId: string) =>
    axiosInstance.head(`/users/${userId}/likes/${episodeId}`),

  // v3.0: Count endpoint - GET /episodes/{episodeId}/likes?count=true
  getLikeCount: (episodeId: string) =>
    axiosInstance.get<CountResponse>(`/episodes/${episodeId}/likes`, {
      params: { count: true },
    }),
};

// ==================== Progress API ====================
export const progressAPI = {
  // v3.0: Nested resource - PUT /users/{userId}/progress/{episodeId} (idempotent upsert)
  updateProgress: (
    userId: string,
    episodeId: string,
    positionSeconds: number,
    completed: boolean
  ) =>
    axiosInstance.put<ListeningProgress>(`/users/${userId}/progress/${episodeId}`, {
      positionSeconds,
      completed,
    }),

  // v3.0: GET /users/{userId}/progress/{episodeId}
  getProgressByUserAndEpisode: (userId: string, episodeId: string) =>
    axiosInstance.get<ListeningProgress>(`/users/${userId}/progress/${episodeId}`),

  // v3.0: DELETE /users/{userId}/progress/{episodeId}
  deleteProgress: (userId: string, episodeId: string) =>
    axiosInstance.delete(`/users/${userId}/progress/${episodeId}`),

  // v3.0: GET /users/{userId}/progress
  listProgressByUser: (userId: string, cursor?: string, limit: number = 20) =>
    axiosInstance.get<PaginatedResponse<ListeningProgress>>(`/users/${userId}/progress`, {
      params: { cursor, limit },
    }),
};


