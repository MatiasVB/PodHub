import React from 'react';
import { useParams, Link, Navigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { subscriptionsAPI, podcastsAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import type { Podcast } from '../api/types';

export const UserSubscriptionsPage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const { user: currentUser } = useAuth();

  if (!userId || !currentUser) {
    return <div>Error al cargar suscripciones</div>;
  }

  // Check if viewing own subscriptions
  const isOwnSubscriptions = currentUser.id === userId;

  // Redirect to own subscriptions if trying to view someone else's
  if (!isOwnSubscriptions) {
    return <Navigate to={`/users/${currentUser.id}/subscriptions`} replace />;
  }

  // Fetch subscriptions
  const { data: subscriptionsData, isLoading: subscriptionsLoading } = useQuery({
    queryKey: ['subscriptions', userId],
    queryFn: async () => {
      const response = await subscriptionsAPI.listSubscriptionsByUser(userId, undefined, 200);
      return response.data;
    },
  });

  const subscriptions = subscriptionsData?.data || [];
  const podcastIds = subscriptions.map((sub) => sub.podcastId);

  // Fetch podcasts for all subscriptions
  const podcastQueries = useQuery({
    queryKey: ['subscribed-podcasts', podcastIds],
    queryFn: async () => {
      if (podcastIds.length === 0) return [];
      
      const promises = podcastIds.map((id) =>
        podcastsAPI.getPodcast(id).then((res) => res.data).catch(() => null)
      );
      
      const results = await Promise.all(promises);
      return results.filter((podcast): podcast is Podcast => podcast !== null);
    },
    enabled: podcastIds.length > 0,
  });

  const podcasts = podcastQueries.data || [];

  if (subscriptionsLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">Mis Suscripciones</h1>
        <p className="text-gray-600 text-lg">
          {subscriptions.length === 0
            ? 'Aún no estás suscrito a ningún podcast'
            : `Tienes ${subscriptions.length} ${subscriptions.length === 1 ? 'suscripción' : 'suscripciones'}`}
        </p>
      </div>

      {/* Loading State */}
      {podcastQueries.isLoading && subscriptions.length > 0 && (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      )}

      {/* Empty State */}
      {subscriptions.length === 0 && (
        <div className="text-center py-20">
          <div className="inline-block p-4 bg-gray-100 rounded-full mb-4">
            <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"
              />
            </svg>
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No tienes suscripciones</h3>
          <p className="text-gray-600 mb-6">Explora podcasts y suscríbete a tus favoritos</p>
          <Link
            to="/podcasts"
            className="inline-block px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
          >
            Explorar Podcasts
          </Link>
        </div>
      )}

      {/* Podcasts Grid */}
      {!podcastQueries.isLoading && podcasts.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {podcasts.map((podcast) => (
            <Link
              key={podcast.id}
              to={`/podcasts/${podcast.id}`}
              className="group bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 overflow-hidden"
            >
              {/* Cover Image */}
              <div className="aspect-square overflow-hidden bg-gradient-to-br from-blue-100 to-purple-100">
                <img
                  src={podcast.coverImageUrl || '/Imagenes/fotopodcast.svg'}
                  alt={podcast.title}
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                  onError={(e) => {
                    e.currentTarget.src = '/Imagenes/fotopodcast.svg';
                  }}
                />
              </div>

              {/* Content */}
              <div className="p-4">
                <h3 className="font-bold text-lg text-gray-900 mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors">
                  {podcast.title}
                </h3>
                <p className="text-gray-600 text-sm line-clamp-2 mb-3">{podcast.description}</p>

                {/* Metadata */}
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <span className="px-2 py-1 bg-blue-50 text-blue-600 rounded-full font-medium">
                    {podcast.category}
                  </span>
                  <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded-full">
                    {(podcast.language ?? '—').toUpperCase()}
                  </span>
                </div>
              </div>

              {/* Subscribed Badge */}
              <div className="px-4 pb-4">
                <div className="flex items-center gap-2 text-green-600 text-sm font-medium">
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                      clipRule="evenodd"
                    />
                  </svg>
                  Suscrito
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};


