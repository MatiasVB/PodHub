import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { podcastsAPI } from '../api/endpoints';
import type { CreatePodcastRequest } from '../api/types';
import { useAuth } from '../app/AuthProvider';

// Utility function to generate URL-friendly slug from title
function generateSlug(title: string): string {
  return title
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '') // Remove special characters
    .replace(/\s+/g, '-')      // Replace spaces with hyphens
    .replace(/-+/g, '-')       // Replace multiple hyphens with single hyphen
    .replace(/^-+|-+$/g, '');  // Trim hyphens from start and end
}

interface CreatePodcastModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
}

const PODCAST_CATEGORIES = [
  { value: 'TECHNOLOGY', label: 'Tecnología' },
  { value: 'EDUCATION', label: 'Educación' },
  { value: 'BUSINESS', label: 'Negocios' },
  { value: 'COMEDY', label: 'Comedia' },
  { value: 'NEWS', label: 'Noticias' },
  { value: 'SPORTS', label: 'Deportes' },
  { value: 'MUSIC', label: 'Música' },
  { value: 'HEALTH', label: 'Salud' },
  { value: 'SCIENCE', label: 'Ciencia' },
  { value: 'ARTS', label: 'Arte' },
  { value: 'HISTORY', label: 'Historia' },
  { value: 'TRUE_CRIME', label: 'Crimen Real' },
  { value: 'SOCIETY_CULTURE', label: 'Sociedad y Cultura' },
];

const LANGUAGES = [
  { code: 'es', name: 'Español' },
  { code: 'en', name: 'English' },
  { code: 'pt', name: 'Português' },
  { code: 'fr', name: 'Français' },
];

export const CreatePodcastModal: React.FC<CreatePodcastModalProps> = ({ isOpen, onClose, userId }) => {
  const queryClient = useQueryClient();
  const { refreshTokenAndUser } = useAuth();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [language, setLanguage] = useState('es');
  const [category, setCategory] = useState('TECHNOLOGY');
  const [coverImageUrl, setCoverImageUrl] = useState('');
  const [isPublic, setIsPublic] = useState(true);

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setTitle('');
      setDescription('');
      setLanguage('es');
      setCategory('TECHNOLOGY');
      setCoverImageUrl('');
      setIsPublic(true);
      createPodcastMutation.reset(); // Mutation object is stable, safe to use without dependency
    }
  }, [isOpen]); // createPodcastMutation is stable and doesn't need to be in dependencies

  // Create podcast mutation
  const createPodcastMutation = useMutation({
    mutationFn: async (data: CreatePodcastRequest) => {
      const response = await podcastsAPI.createPodcast(data);
      return response.data;
    },
    onSuccess: async () => {
      // Invalidate queries first
      queryClient.invalidateQueries({ queryKey: ['podcasts', 'creator', userId] });
      queryClient.invalidateQueries({ queryKey: ['podcasts'] });
      queryClient.invalidateQueries({ queryKey: ['user', userId] });

      try {
        // Wait for token refresh to complete and get updated roles
        await refreshTokenAndUser();
      } catch (error) {
        console.error('Error refreshing token after podcast creation:', error);
        // Podcast was created successfully, user might need to reload to see creator role
      }

      onClose();
    },
    onError: (error) => {
      console.error('Error creating podcast:', error);
      // Error message will be displayed automatically via isError state
    },
  });

  // Handle click outside to close
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (target.classList.contains('modal-overlay')) {
        onClose();
      }
    };

    const handleEscapeKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('click', handleClickOutside);
    document.addEventListener('keydown', handleEscapeKey);

    return () => {
      document.removeEventListener('click', handleClickOutside);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [isOpen, onClose]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const data: CreatePodcastRequest = {
      title: title.trim(),
      slug: generateSlug(title),
      description: description.trim(),
      language,
      category,
      isPublic,
    };

    if (coverImageUrl.trim()) {
      data.coverImageUrl = coverImageUrl.trim();
    }

    createPodcastMutation.mutate(data);
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 className="text-xl font-bold text-gray-900">Crear Nuevo Podcast</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            aria-label="Cerrar"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <form onSubmit={handleSubmit} id="create-podcast-form" className="space-y-4">
            {/* Title */}
            <div>
              <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
                Título <span className="text-red-500">*</span>
              </label>
              <input
                id="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                minLength={3}
                maxLength={100}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Nombre de tu podcast"
              />
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
                Descripción <span className="text-red-500">*</span>
              </label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                minLength={10}
                rows={4}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                placeholder="Describe de qué trata tu podcast..."
              />
            </div>

            {/* Language */}
            <div>
              <label htmlFor="language" className="block text-sm font-medium text-gray-700 mb-2">
                Idioma <span className="text-red-500">*</span>
              </label>
              <select
                id="language"
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {LANGUAGES.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Category */}
            <div>
              <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-2">
                Categoría <span className="text-red-500">*</span>
              </label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {PODCAST_CATEGORIES.map((cat) => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Cover Image URL */}
            <div>
              <label htmlFor="coverImageUrl" className="block text-sm font-medium text-gray-700 mb-2">
                URL de Imagen de Portada
              </label>
              <input
                id="coverImageUrl"
                type="url"
                value={coverImageUrl}
                onChange={(e) => setCoverImageUrl(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="https://ejemplo.com/imagen.jpg"
              />
            </div>

            {/* Is Public */}
            <div className="flex items-center">
              <input
                id="isPublic"
                type="checkbox"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <label htmlFor="isPublic" className="ml-2 text-sm text-gray-700">
                Hacer público (visible para todos)
              </label>
            </div>

            {/* Error Message */}
            {createPodcastMutation.isError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-600">
                  Error al crear el podcast. Por favor, intenta de nuevo.
                </p>
              </div>
            )}
          </form>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-6 border-t border-gray-200">
          <button
            type="button"
            onClick={onClose}
            disabled={createPodcastMutation.isPending}
            className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="create-podcast-form"
            disabled={createPodcastMutation.isPending || !title.trim() || !description.trim()}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {createPodcastMutation.isPending ? 'Creando...' : 'Crear Podcast'}
          </button>
        </div>
      </div>
    </div>
  );
};
