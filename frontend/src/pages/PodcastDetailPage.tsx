import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { podcastsAPI, episodesAPI, commentsAPI, subscriptionsAPI, usersAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { CommentSection } from '../components/CommentSection';
import { SubscribersModal } from '../components/SubscribersModal';
import { EditPodcastModal } from '../components/EditPodcastModal';
import { CreateEpisodeModal } from '../components/CreateEpisodeModal';
import type { Episode } from '../api/types';

export const PodcastDetailPage: React.FC = () => {
  const { podcastId } = useParams<{ podcastId: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [episodeSearchQuery, setEpisodeSearchQuery] = useState('');
  const [debouncedEpisodeSearch, setDebouncedEpisodeSearch] = useState('');
  const [showSubscribersModal, setShowSubscribersModal] = useState(false);
  const [showEditPodcastModal, setShowEditPodcastModal] = useState(false);
  const [showCreateEpisodeModal, setShowCreateEpisodeModal] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

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

  // Fetch podcast creator
  const { data: creator } = useQuery({
    queryKey: ['user', podcast?.creatorId],
    queryFn: async () => {
      if (!podcast?.creatorId) return null;
      const response = await usersAPI.getUser(podcast.creatorId);
      return response.data;
    },
    enabled: !!podcast?.creatorId,
  });

  // Debounce episode search
  React.useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedEpisodeSearch(episodeSearchQuery);
    }, 500);
    return () => clearTimeout(timer);
  }, [episodeSearchQuery]);

  // Fetch episodes
  const { data: episodesData, isLoading: episodesLoading } = useQuery({
    queryKey: ['episodes', podcastId, debouncedEpisodeSearch],
    queryFn: async () => {
      if (debouncedEpisodeSearch.trim()) {
        // Search episodes by title within this podcast
        const response = await episodesAPI.listEpisodes({
          podcastId,
          title: debouncedEpisodeSearch,
          limit: 50
        });
        return response.data;
      } else {
        // Default: all episodes for this podcast
        const response = await episodesAPI.listEpisodesByPodcast(podcastId, undefined, 50);
        return response.data;
      }
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

  // Fetch subscriber count (v3.0 enhancement)
  const { data: subscriberCountData } = useQuery({
    queryKey: ['podcast-subscribers-count', podcastId],
    queryFn: async () => {
      const response = await subscriptionsAPI.getSubscriptionCount(podcastId);
      return response.data;
    },
  });

  const subscriberCount = subscriberCountData?.count;

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
      queryClient.invalidateQueries({ queryKey: ['podcast-subscribers-count', podcastId] });
    },
  });

  // Delete podcast mutation
  const deletePodcastMutation = useMutation({
    mutationFn: async () => {
      await podcastsAPI.deletePodcast(podcastId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['podcasts'] });
      navigate(`/users/${user?.id}`);
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

  // Check if current user is the creator of this podcast
  const isCreator = user && podcast && user.id === podcast.creatorId;

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
                <div className="flex flex-wrap gap-3 mb-4">
                  <span className="px-4 py-2 bg-blue-100 text-blue-700 rounded-full font-medium">
                    {podcast.category}
                  </span>
                  <span className="px-4 py-2 bg-gray-100 text-gray-700 rounded-full font-medium">
                    {(podcast.language ?? '—').toUpperCase()}
                  </span>
                  {/* Creator Info */}
                  {creator && (
                    <Link
                      to={`/users/${creator.id}`}
                      className="flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-700 rounded-full font-medium hover:bg-purple-200 transition-colors"
                    >
                      <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" />
                      </svg>
                      <span>Por {creator.displayName || creator.username}</span>
                    </Link>
                  )}
                </div>

                {/* Subscriber count - CLICKABLE */}
                {typeof subscriberCount === 'number' && (
                  <button
                    onClick={() => setShowSubscribersModal(true)}
                    className="text-gray-600 text-sm hover:text-blue-600 hover:underline transition-colors mb-6"
                  >
                    {subscriberCount.toLocaleString('es-ES')} {subscriberCount === 1 ? 'suscriptor' : 'suscriptores'}
                  </button>
                )}
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

            {/* Creator Actions - Only visible to podcast owner */}
            {isCreator && (
              <div className="mt-6 pt-6 border-t border-gray-200">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Acciones del Creador</h3>
                <div className="flex flex-wrap gap-3">
                  <button
                    onClick={() => setShowEditPodcastModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-100 text-blue-700 rounded-lg font-medium hover:bg-blue-200 transition-colors"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                    Editar Podcast
                  </button>
                  <button
                    onClick={() => setShowCreateEpisodeModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-green-100 text-green-700 rounded-lg font-medium hover:bg-green-200 transition-colors"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    Subir Episodio
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirmation(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-red-100 text-red-700 rounded-lg font-medium hover:bg-red-200 transition-colors"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    Eliminar Podcast
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Episodes Section */}
      <div className="bg-white rounded-xl shadow-md p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Episodios</h2>

        {/* Episode Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <input
              type="text"
              value={episodeSearchQuery}
              onChange={(e) => setEpisodeSearchQuery(e.target.value)}
              placeholder="Buscar episodios..."
              className="w-full px-6 py-3 pl-12 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm transition-all"
            />
            <svg
              className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </div>
        </div>

        {episodesLoading ? (
          <div className="flex justify-center py-10">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
          </div>
        ) : Object.keys(episodesBySeason).length === 0 ? (
          <p className="text-center text-gray-500 py-10">
            {episodeSearchQuery.trim()
              ? `No se encontraron episodios con "${episodeSearchQuery}"`
              : 'Aún no hay episodios disponibles'}
          </p>
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
          podcastCreatorId={podcast.creatorId}
        />
      )}

      {/* Subscribers Modal */}
      <SubscribersModal
        isOpen={showSubscribersModal}
        onClose={() => setShowSubscribersModal(false)}
        podcastId={podcastId}
      />

      {/* Edit Podcast Modal */}
      {showEditPodcastModal && podcast && (
        <EditPodcastModal
          isOpen={showEditPodcastModal}
          onClose={() => setShowEditPodcastModal(false)}
          podcast={podcast}
          userId={user?.id || ''}
        />
      )}

      {/* Create Episode Modal */}
      {showCreateEpisodeModal && (
        <CreateEpisodeModal
          isOpen={showCreateEpisodeModal}
          onClose={() => setShowCreateEpisodeModal(false)}
          podcastId={podcastId}
        />
      )}

      {/* Delete Podcast Confirmation */}
      {showDeleteConfirmation && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6">
            <h3 className="text-xl font-bold text-gray-900 mb-4">Confirmar Eliminación</h3>
            <p className="text-gray-600 mb-6">
              ¿Estás seguro que deseas eliminar <strong>{podcast?.title}</strong>? Esta acción no se puede deshacer.
              Todos los episodios ({episodesData?.data.length || 0}) y comentarios asociados también podrían verse afectados.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowDeleteConfirmation(false)}
                disabled={deletePodcastMutation.isPending}
                className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={() => deletePodcastMutation.mutate()}
                disabled={deletePodcastMutation.isPending}
                className="px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {deletePodcastMutation.isPending ? 'Eliminando...' : 'Eliminar Definitivamente'}
              </button>
            </div>

            {/* Error display */}
            {deletePodcastMutation.isError && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-600">
                  Error al eliminar el podcast. Por favor, intenta de nuevo.
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};


