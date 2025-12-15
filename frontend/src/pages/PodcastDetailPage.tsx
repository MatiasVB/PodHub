import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { podcastsAPI, episodesAPI, commentsAPI, subscriptionsAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { CommentSection } from '../components/CommentSection';
import type { Episode } from '../api/types';

export const PodcastDetailPage: React.FC = () => {
  const { podcastId } = useParams<{ podcastId: string }>();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  if (!podcastId) {
    return <div>Podcast no encontrado</div>;
  }

  // Fetch podcast details
  const { data: podcast, isLoading: podcastLoading } = useQuery({
    queryKey: ['podcast', podcastId],
    queryFn: async () => {
      const response = await podcastsAPI.getPodcast(podcastId);
      return response.data;
    },
  });

  // Fetch episodes
  const { data: episodesData, isLoading: episodesLoading } = useQuery({
    queryKey: ['episodes', podcastId],
    queryFn: async () => {
      const response = await episodesAPI.listEpisodesByPodcast(podcastId, undefined, 50);
      return response.data;
    },
  });

  // Fetch comments
  const { data: commentsData } = useQuery({
    queryKey: ['comments', 'podcast', podcastId],
    queryFn: async () => {
      const response = await commentsAPI.listCommentsByPodcast(podcastId, undefined, 50);
      return response.data;
    },
  });

  // Fetch user subscriptions
  const { data: subscriptionsData } = useQuery({
    queryKey: ['subscriptions', user?.id],
    queryFn: async () => {
      if (!user) return null;
      const response = await subscriptionsAPI.listSubscriptionsByUser(user.id, undefined, 200);
      return response.data;
    },
    enabled: !!user,
  });

  // Check if subscribed
  const isSubscribed = subscriptionsData?.data.some((sub) => sub.podcastId === podcastId) || false;

  // Subscribe mutation
  const subscribeMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error('Usuario no autenticado');
      if (isSubscribed) {
        await subscriptionsAPI.unsubscribe(user.id, podcastId);
      } else {
        await subscriptionsAPI.subscribe(user.id, podcastId);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions', user?.id] });
    },
  });

  // Group episodes by season
  const episodesBySeason = React.useMemo(() => {
    if (!episodesData?.data) return {};

    const grouped: Record<string, Episode[]> = {};
    episodesData.data.forEach((episode: Episode) => {
      const seasonKey = episode.season !== null ? `Temporada ${episode.season}` : 'Sin temporada';
      if (!grouped[seasonKey]) {
        grouped[seasonKey] = [];
      }
      grouped[seasonKey].push(episode);
    });

    // Sort episodes by number within each season
    Object.keys(grouped).forEach((season) => {
      grouped[season].sort((a, b) => b.number - a.number);
    });

    return grouped;
  }, [episodesData]);

  if (podcastLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!podcast) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-gray-900">Podcast no encontrado</h2>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Podcast Header */}
      <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
        <div className="md:flex">
          {/* Cover Image */}
          <div className="md:w-1/3 lg:w-1/4">
            <img
              src={podcast.coverImageUrl || '/Imagenes/fotopodcast.svg'}
              alt={podcast.title}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.currentTarget.src = '/Imagenes/fotopodcast.svg';
              }}
            />
          </div>

          {/* Podcast Info */}
          <div className="md:w-2/3 lg:w-3/4 p-8">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <h1 className="text-4xl font-bold text-gray-900 mb-4">{podcast.title}</h1>
                <p className="text-gray-600 text-lg mb-6">{podcast.description}</p>

                {/* Metadata */}
                <div className="flex flex-wrap gap-3 mb-6">
                  <span className="px-4 py-2 bg-blue-100 text-blue-700 rounded-full font-medium">
                    {podcast.category}
                  </span>
                  <span className="px-4 py-2 bg-gray-100 text-gray-700 rounded-full font-medium">
                    {(podcast.language ?? '—').toUpperCase()}
                  </span>
                </div>
              </div>

              {/* Subscribe Button */}
              <button
                onClick={() => subscribeMutation.mutate()}
                disabled={subscribeMutation.isPending}
                className={`px-6 py-3 rounded-lg font-medium transition-colors whitespace-nowrap ${
                  isSubscribed
                    ? 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                    : 'bg-blue-600 text-white hover:bg-blue-700'
                }`}
              >
                {subscribeMutation.isPending ? (
                  'Procesando...'
                ) : isSubscribed ? (
                  <>
                    <span className="inline-block mr-2">✓</span>
                    Suscrito
                  </>
                ) : (
                  'Suscribirse'
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Episodes Section */}
      <div className="bg-white rounded-xl shadow-md p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Episodios</h2>

        {episodesLoading ? (
          <div className="flex justify-center py-10">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
          </div>
        ) : Object.keys(episodesBySeason).length === 0 ? (
          <p className="text-center text-gray-500 py-10">Aún no hay episodios disponibles</p>
        ) : (
          <div className="space-y-8">
            {Object.entries(episodesBySeason).map(([season, episodes]) => (
              <div key={season}>
                <h3 className="text-xl font-bold text-gray-800 mb-4">{season}</h3>
                <div className="space-y-3">
                  {episodes.map((episode: Episode) => (
                    <Link
                      key={episode.id}
                      to={`/episodes/${episode.id}`}
                      className="block p-4 bg-gray-50 hover:bg-blue-50 rounded-lg transition-colors group"
                    >
                      <div className="flex items-start gap-4">
                        <span className="text-2xl font-bold text-gray-400 group-hover:text-blue-600 transition-colors">
                          {episode.number}
                        </span>
                        <div className="flex-1">
                          <h4 className="font-semibold text-gray-900 group-hover:text-blue-600 mb-1">
                            {episode.title}
                          </h4>
                          <p className="text-gray-600 text-sm line-clamp-2 mb-2">{episode.description}</p>
                          <div className="flex items-center gap-4 text-xs text-gray-500">
                            <span>{Math.floor(episode.durationSec / 60)} min</span>
                            <span>
                              {new Date(episode.publishAt).toLocaleDateString('es-ES', {
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric',
                              })}
                            </span>
                          </div>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Comments Section */}
      {commentsData && (
        <CommentSection
          comments={commentsData.data}
          target={{ type: 'PODCAST', id: podcastId }}
          queryKey={['comments', 'podcast', podcastId]}
        />
      )}
    </div>
  );
};


