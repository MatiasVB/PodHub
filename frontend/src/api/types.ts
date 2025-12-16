// ==================== Base Types ====================
export interface PaginatedResponse<T> {
  data: T[];
  nextCursor: string | null;
  hasMore: boolean;
  count: number;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// ==================== Auth Types ====================
export interface AuthRequest {
  identifier: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
  avatarUrl?: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  roles: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
  authorities: string[];
}

// ==================== Podcast Types ====================
export interface Podcast {
  id: string;
  creatorId: string;
  title: string;
  slug: string;
  description: string;
  language: string;
  category: string;
  coverImageUrl: string | null;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePodcastRequest {
  title: string;
  slug: string;
  description: string;
  language: string;
  category: string;
  coverImageUrl?: string;
  isPublic?: boolean;
}

// ==================== Episode Types ====================
export interface Episode {
  id: string;
  podcastId: string;
  title: string;
  season: number | null;
  number: number;
  description: string;
  audioUrl: string;
  durationSec: number;
  publishAt: string;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEpisodeRequest {
  podcastId: string;
  title: string;
  season?: number;
  number: number;
  description: string;
  audioUrl: string;
  durationSec: number;
  publishAt?: string;
  isPublic?: boolean;
}

// ==================== Comment Types ====================
export interface CommentTarget {
  type: 'PODCAST' | 'EPISODE';
  id: string;
}

export interface Comment {
  id: string;
  userId: string;
  target: CommentTarget;
  content: string;
  parentId: string | null;
  status: 'VISIBLE' | 'HIDDEN' | 'DELETED';
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentRequest {
  userId: string;
  target: CommentTarget;
  content: string;
  parentId?: string | null;
  status?: 'VISIBLE' | 'HIDDEN';
}

// ==================== Subscription Types ====================
export interface Subscription {
  id: string;
  userId: string;
  podcastId: string;
  subscribedAt: string;
}

// ==================== Like Types ====================
export interface EpisodeLike {
  id: string;
  userId: string;
  episodeId: string;
  likedAt: string;
}

// ==================== Progress Types ====================
export interface ListeningProgress {
  id: string;
  userId: string;
  episodeId: string;
  positionSeconds: number;
  completed: boolean;
  lastListenedAt: string;
}

// ==================== Request Types (v3.0) ====================
export interface SubscriptionRequest {
  podcastId: string;
}

export interface LikeRequest {
  episodeId: string;
}

export interface ProgressRequest {
  positionSeconds: number;
  completed: boolean;
}

export interface CountResponse {
  count: number;
}
