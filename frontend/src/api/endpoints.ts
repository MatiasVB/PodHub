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
  
  listUsers: (cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<UserResponse>>('/users', {
      params: { cursor, limit },
    }),
  
  searchUsers: (name: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<UserResponse>>('/users/search', {
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
  
  listPodcasts: (cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts', {
      params: { cursor, limit },
    }),
  
  listPublicPodcasts: (cursor?: string, limit: number = 50) => 
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts/public', {
      params: { cursor, limit },
    }),
  
  listPodcastsByCreator: (creatorId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Podcast>>(`/podcasts/creator/${creatorId}`, {
      params: { cursor, limit },
    }),
  
  searchPodcasts: (title: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Podcast>>('/podcasts/search', {
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
  
  listEpisodes: (cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes', {
      params: { cursor, limit },
    }),
  
  listPublicEpisodes: (cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes/public', {
      params: { cursor, limit },
    }),
  
  listEpisodesByPodcast: (podcastId: string, cursor?: string, limit: number = 50) => 
    axiosInstance.get<PaginatedResponse<Episode>>(`/episodes/podcast/${podcastId}`, {
      params: { cursor, limit },
    }),
  
  searchEpisodes: (title: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Episode>>('/episodes/search', {
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
  
  listCommentsByPodcast: (podcastId: string, cursor?: string, limit: number = 50) => 
    axiosInstance.get<PaginatedResponse<Comment>>(`/comments/podcast/${podcastId}`, {
      params: { cursor, limit },
    }),
  
  listCommentsByEpisode: (episodeId: string, cursor?: string, limit: number = 50) => 
    axiosInstance.get<PaginatedResponse<Comment>>(`/comments/episode/${episodeId}`, {
      params: { cursor, limit },
    }),
  
  listCommentsByParent: (parentId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Comment>>(`/comments/parent/${parentId}`, {
      params: { cursor, limit },
    }),
};

// ==================== Subscriptions API ====================
export const subscriptionsAPI = {
  subscribe: (userId: string, podcastId: string) => 
    axiosInstance.post<Subscription>('/subscriptions/subscribe', null, {
      params: { userId, podcastId },
    }),
  
  unsubscribe: (userId: string, podcastId: string) => 
    axiosInstance.delete('/subscriptions/unsubscribe', {
      params: { userId, podcastId },
    }),
  
  getSubscription: (id: string) => 
    axiosInstance.get<Subscription>(`/subscriptions/${id}`),
  
  listSubscriptionsByUser: (userId: string, cursor?: string, limit: number = 200) => 
    axiosInstance.get<PaginatedResponse<Subscription>>(`/subscriptions/user/${userId}`, {
      params: { cursor, limit },
    }),
  
  listSubscriptionsByPodcast: (podcastId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<Subscription>>(`/subscriptions/podcast/${podcastId}`, {
      params: { cursor, limit },
    }),
  
  getSubscriptionCount: (podcastId: string) => 
    axiosInstance.get<number>(`/subscriptions/podcast/${podcastId}/count`),
};

// ==================== Likes API ====================
export const likesAPI = {
  like: (userId: string, episodeId: string) => 
    axiosInstance.post<EpisodeLike>('/likes/like', null, {
      params: { userId, episodeId },
    }),
  
  unlike: (userId: string, episodeId: string) => 
    axiosInstance.delete('/likes/unlike', {
      params: { userId, episodeId },
    }),
  
  getLike: (id: string) => 
    axiosInstance.get<EpisodeLike>(`/likes/${id}`),
  
  listLikesByUser: (userId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<EpisodeLike>>(`/likes/user/${userId}`, {
      params: { cursor, limit },
    }),
  
  listLikesByEpisode: (episodeId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<EpisodeLike>>(`/likes/episode/${episodeId}`, {
      params: { cursor, limit },
    }),
  
  checkLikeExists: (userId: string, episodeId: string) => 
    axiosInstance.get<boolean>('/likes/exists', {
      params: { userId, episodeId },
    }),
  
  getLikeCount: (episodeId: string) => 
    axiosInstance.get<number>(`/likes/episode/${episodeId}/count`),
};

// ==================== Progress API ====================
export const progressAPI = {
  updateProgress: (
    userId: string,
    episodeId: string,
    positionSeconds: number,
    completed: boolean
  ) =>
    axiosInstance.post<ListeningProgress>('/progress', null, {
      params: { userId, episodeId, positionSeconds, completed },
    }),
  
  getProgress: (id: string) => 
    axiosInstance.get<ListeningProgress>(`/progress/${id}`),
  
  getProgressByUserAndEpisode: (userId: string, episodeId: string) => 
    axiosInstance.get<ListeningProgress>('/progress/one', {
      params: { userId, episodeId },
    }),
  
  deleteProgress: (userId: string, episodeId: string) => 
    axiosInstance.delete('/progress', {
      params: { userId, episodeId },
    }),
  
  listProgressByUser: (userId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<ListeningProgress>>(`/progress/user/${userId}`, {
      params: { cursor, limit },
    }),
  
  listProgressByEpisode: (episodeId: string, cursor?: string, limit: number = 20) => 
    axiosInstance.get<PaginatedResponse<ListeningProgress>>(`/progress/episode/${episodeId}`, {
      params: { cursor, limit },
    }),
};


