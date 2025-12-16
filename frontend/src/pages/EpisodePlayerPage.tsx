import React, { useRef, useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  episodesAPI,
  commentsAPI,
  likesAPI,
  progressAPI,
  podcastsAPI,
} from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { CommentSection } from '../components/CommentSection';

export const EpisodePlayerPage: React.FC = () => {
  const { episodeId } = useParams<{ episodeId: string }>();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const progressTimerRef = useRef<number | null>(null);

  if (!episodeId || !user) {
    return <div>Error: Episodio no encontrado</div>;
  }

  // Fetch episode details
  const { data: episode, isLoading: episodeLoading } = useQuery({
    queryKey: ['episode', episodeId],
    queryFn: async () => {
      const response = await episodesAPI.getEpisode(episodeId);
      return response.data;
    },
  });

  // Fetch podcast details
  const { data: podcast } = useQuery({
    queryKey: ['podcast', episode?.podcastId],
    queryFn: async () => {
      if (!episode?.podcastId) return null;
      const response = await podcastsAPI.getPodcast(episode.podcastId);
      return response.data;
    },
    enabled: !!episode?.podcastId,
  });

  // Fetch comments
  const { data: commentsData } = useQuery({
    queryKey: ['comments', 'episode', episodeId],
    queryFn: async () => {
      const response = await commentsAPI.listCommentsByEpisode(episodeId, undefined, 50);
      return response.data;
    },
  });

  // Fetch like status (v3.0: HEAD request returns 200 OK or 404)
  const { data: isLiked, refetch: refetchLikeStatus } = useQuery({
    queryKey: ['like', user.id, episodeId],
    queryFn: async () => {
      try {
        await likesAPI.checkLikeExists(user.id, episodeId);
        return true; // 200 OK means liked
      } catch (error) {
        return false; // 404 means not liked
      }
    },
  });

  // Fetch like count (v3.0: Returns CountResponse { count: number })
  const { data: likeCountData } = useQuery({
    queryKey: ['likeCount', episodeId],
    queryFn: async () => {
      const response = await likesAPI.getLikeCount(episodeId);
      return response.data;
    },
  });

  const likeCount = likeCountData?.count;

  // Fetch saved progress
  const { data: savedProgress } = useQuery({
    queryKey: ['progress', user.id, episodeId],
    queryFn: async () => {
      try {
        const response = await progressAPI.getProgressByUserAndEpisode(user.id, episodeId);
        return response.data;
      } catch (error) {
        return null;
      }
    },
  });

  // Load saved progress when audio is ready
  useEffect(() => {
    if (audioRef.current && savedProgress && !isPlaying) {
      audioRef.current.currentTime = savedProgress.positionSeconds;
    }
  }, [savedProgress, episodeId]);

  // Toggle like mutation
  const toggleLikeMutation = useMutation({
    mutationFn: async () => {
      if (isLiked) {
        await likesAPI.unlike(user.id, episodeId);
      } else {
        await likesAPI.like(user.id, episodeId);
      }
    },
    onSuccess: () => {
      refetchLikeStatus();
      queryClient.invalidateQueries({ queryKey: ['likeCount', episodeId] });
    },
  });

  // Save progress mutation
  const saveProgressMutation = useMutation({
    mutationFn: async (data: { position: number; completed: boolean }) => {
      await progressAPI.updateProgress(user.id, episodeId, data.position, data.completed);
    },
  });

  // Save progress periodically
  const saveProgress = (completed: boolean = false) => {
    if (audioRef.current) {
      const position = Math.floor(audioRef.current.currentTime);
      saveProgressMutation.mutate({ position, completed });
    }
  };

  // Setup progress saving
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const handlePlay = () => {
      setIsPlaying(true);
      // Save progress every 15 seconds
      progressTimerRef.current = setInterval(() => {
        saveProgress(false);
      }, 15000);
    };

    const handlePause = () => {
      setIsPlaying(false);
      if (progressTimerRef.current) {
        clearInterval(progressTimerRef.current);
      }
      saveProgress(false);
    };

    const handleEnded = () => {
      setIsPlaying(false);
      if (progressTimerRef.current) {
        clearInterval(progressTimerRef.current);
      }
      saveProgress(true);
    };

    audio.addEventListener('play', handlePlay);
    audio.addEventListener('pause', handlePause);
    audio.addEventListener('ended', handleEnded);

    return () => {
      if (progressTimerRef.current) {
        clearInterval(progressTimerRef.current);
      }
      audio.removeEventListener('play', handlePlay);
      audio.removeEventListener('pause', handlePause);
      audio.removeEventListener('ended', handleEnded);
    };
  }, [episodeId]);

  if (episodeLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!episode) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-gray-900">Episodio no encontrado</h2>
      </div>
    );
  }

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="space-y-8">
      {/* Episode Player Card */}
      <div className="bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl shadow-2xl overflow-hidden text-white">
        <div className="p-8">
          {/* Back to Podcast */}
          {podcast && (
            <Link
              to={`/podcasts/${podcast.id}`}
              className="inline-flex items-center gap-2 text-white/80 hover:text-white mb-6 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              Volver a {podcast.title}
            </Link>
          )}

          <div className="md:flex gap-8">
            {/* Cover Image */}
            <div className="md:w-1/3 lg:w-1/4 mb-6 md:mb-0">
              <img
                src={podcast?.coverImageUrl || '/Imagenes/fotopodcast.svg'}
                alt={episode.title}
                className="w-full rounded-xl shadow-xl"
                onError={(e) => {
                  e.currentTarget.src = '/Imagenes/fotopodcast.svg';
                }}
              />
            </div>

            {/* Episode Info */}
            <div className="md:w-2/3 lg:w-3/4">
              <div className="mb-2">
                <span className="text-sm font-medium text-white/80">
                  {episode.season !== null ? `Temporada ${episode.season} • ` : ''}
                  Episodio {episode.number}
                </span>
              </div>
              <h1 className="text-3xl md:text-4xl font-bold mb-4">{episode.title}</h1>
              <p className="text-white/90 text-lg mb-6">{episode.description}</p>

              {/* Metadata */}
              <div className="flex items-center gap-4 text-sm text-white/80 mb-6">
                <span>{formatDuration(episode.durationSec)}</span>
                <span>•</span>
                <span>
                  {new Date(episode.publishAt).toLocaleDateString('es-ES', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </span>
              </div>

              {/* Audio Player */}
              <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 mb-6">
                <audio ref={audioRef} controls className="w-full" src={episode.audioUrl}>
                  Tu navegador no soporta el elemento de audio.
                </audio>
              </div>

              {/* Like Button */}
              <div className="flex items-center gap-4">
                <button
                  onClick={() => toggleLikeMutation.mutate()}
                  disabled={toggleLikeMutation.isPending}
                  className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium transition-all ${
                    isLiked
                      ? 'bg-red-500 hover:bg-red-600'
                      : 'bg-white/20 hover:bg-white/30 backdrop-blur-sm'
                  }`}
                >
                  <svg
                    className="w-6 h-6"
                    fill={isLiked ? 'currentColor' : 'none'}
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                    />
                  </svg>
                  {isLiked ? 'Te gusta' : 'Me gusta'}
                </button>
                {typeof likeCount === 'number' && (
                  <span className="text-white/90 font-medium">
                    {likeCount} {likeCount === 1 ? 'me gusta' : 'me gusta'}
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Comments Section */}
      {commentsData && (
        <CommentSection
          comments={commentsData.data}
          target={{ type: 'EPISODE', id: episodeId }}
          queryKey={['comments', 'episode', episodeId]}
          podcastCreatorId={podcast?.creatorId}
        />
      )}
    </div>
  );
};


