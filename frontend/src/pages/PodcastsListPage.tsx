import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { podcastsAPI } from '../api/endpoints';
import type { Podcast } from '../api/types';

export const PodcastsListPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Debounce search
  React.useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 500);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Fetch podcasts
  const { data, isLoading, error } = useQuery({
    queryKey: ['podcasts', debouncedSearch],
    queryFn: async () => {
      if (debouncedSearch.trim()) {
        const response = await podcastsAPI.searchPodcasts(debouncedSearch, undefined, 50);
        return response.data;
      } else {
        const response = await podcastsAPI.listPublicPodcasts(undefined, 50);
        return response.data;
      }
    },
  });

  const podcasts = data?.data || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">Explora Podcasts</h1>
        <p className="text-gray-600 text-lg">Descubre contenido increíble de creadores de todo el mundo</p>
      </div>

      {/* Search Bar */}
      <div className="max-w-2xl mx-auto">
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Buscar podcasts..."
            className="w-full px-6 py-4 pl-14 border border-gray-300 rounded-2xl focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm transition-all"
          />
          <svg
            className="absolute left-5 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400"
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

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="text-center py-20">
          <div className="inline-block p-4 bg-red-50 rounded-full mb-4">
            <svg className="w-12 h-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">Error al cargar podcasts</h3>
          <p className="text-gray-600">Por favor, intenta de nuevo más tarde</p>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && podcasts.length === 0 && (
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
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No se encontraron podcasts</h3>
          <p className="text-gray-600">
            {searchQuery ? 'Intenta con otros términos de búsqueda' : 'Aún no hay podcasts disponibles'}
          </p>
        </div>
      )}

      {/* Podcasts Grid */}
      {!isLoading && !error && podcasts.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {podcasts.map((podcast: Podcast) => (
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
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};


