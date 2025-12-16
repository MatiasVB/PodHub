import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { episodesAPI } from '../api/endpoints';
import type { CreateEpisodeRequest } from '../api/types';

interface CreateEpisodeModalProps {
  isOpen: boolean;
  onClose: () => void;
  podcastId: string;
}

export const CreateEpisodeModal: React.FC<CreateEpisodeModalProps> = ({ isOpen, onClose, podcastId }) => {
  const queryClient = useQueryClient();

  // Form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [season, setSeason] = useState<string>(''); // Empty string for null
  const [episodeNumber, setEpisodeNumber] = useState<string>('1');
  const [audioUrl, setAudioUrl] = useState('');
  const [durationMinutes, setDurationMinutes] = useState<string>('');
  const [durationSeconds, setDurationSeconds] = useState<string>('0');
  const [publishDate, setPublishDate] = useState<string>(
    new Date().toISOString().split('T')[0] // Today's date in YYYY-MM-DD format
  );
  const [publishTime, setPublishTime] = useState<string>('12:00');
  const [isPublic, setIsPublic] = useState(true);

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setTitle('');
      setDescription('');
      setSeason('');
      setEpisodeNumber('1');
      setAudioUrl('');
      setDurationMinutes('');
      setDurationSeconds('0');
      setPublishDate(new Date().toISOString().split('T')[0]);
      setPublishTime('12:00');
      setIsPublic(true);
    }
  }, [isOpen]);

  // Create episode mutation
  const createEpisodeMutation = useMutation({
    mutationFn: async (data: CreateEpisodeRequest) => {
      const response = await episodesAPI.createEpisode(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['episodes', podcastId] });
      queryClient.invalidateQueries({ queryKey: ['episodes'] });
      onClose();
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

    // Calculate total duration in seconds
    const minutes = parseInt(durationMinutes) || 0;
    const seconds = parseInt(durationSeconds) || 0;
    const totalSeconds = (minutes * 60) + seconds;

    // Combine date and time into ISO-8601 timestamp
    const publishAtTimestamp = `${publishDate}T${publishTime}:00Z`;

    const data: CreateEpisodeRequest = {
      podcastId,
      title: title.trim(),
      description: description.trim(),
      season: season ? parseInt(season) : undefined,
      number: parseInt(episodeNumber),
      audioUrl: audioUrl.trim(),
      durationSec: totalSeconds,
      publishAt: publishAtTimestamp,
      isPublic,
    };

    createEpisodeMutation.mutate(data);
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 className="text-xl font-bold text-gray-900">Subir Nuevo Episodio</h3>
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
          <form onSubmit={handleSubmit} id="create-episode-form" className="space-y-4">
            {/* Title */}
            <div>
              <label htmlFor="episode-title" className="block text-sm font-medium text-gray-700 mb-2">
                Título del Episodio <span className="text-red-500">*</span>
              </label>
              <input
                id="episode-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                minLength={3}
                maxLength={200}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Título del episodio"
              />
            </div>

            {/* Description */}
            <div>
              <label htmlFor="episode-description" className="block text-sm font-medium text-gray-700 mb-2">
                Descripción <span className="text-red-500">*</span>
              </label>
              <textarea
                id="episode-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                minLength={10}
                rows={4}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                placeholder="Describe de qué trata este episodio..."
              />
            </div>

            {/* Season and Episode Number (side by side) */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="season" className="block text-sm font-medium text-gray-700 mb-2">
                  Temporada (opcional)
                </label>
                <input
                  id="season"
                  type="number"
                  value={season}
                  onChange={(e) => setSeason(e.target.value)}
                  min="1"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Ej: 1"
                />
              </div>
              <div>
                <label htmlFor="episode-number" className="block text-sm font-medium text-gray-700 mb-2">
                  Número de Episodio <span className="text-red-500">*</span>
                </label>
                <input
                  id="episode-number"
                  type="number"
                  value={episodeNumber}
                  onChange={(e) => setEpisodeNumber(e.target.value)}
                  required
                  min="1"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Ej: 1"
                />
              </div>
            </div>

            {/* Audio URL */}
            <div>
              <label htmlFor="audio-url" className="block text-sm font-medium text-gray-700 mb-2">
                URL del Audio <span className="text-red-500">*</span>
              </label>
              <input
                id="audio-url"
                type="url"
                value={audioUrl}
                onChange={(e) => setAudioUrl(e.target.value)}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="https://ejemplo.com/audio.mp3"
              />
              <p className="mt-1 text-xs text-gray-500">
                Enlace directo al archivo de audio (MP3, M4A, WAV, etc.)
              </p>
            </div>

            {/* Duration (Minutes and Seconds) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Duración <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="duration-minutes" className="block text-xs text-gray-600 mb-1">
                    Minutos
                  </label>
                  <input
                    id="duration-minutes"
                    type="number"
                    value={durationMinutes}
                    onChange={(e) => setDurationMinutes(e.target.value)}
                    required
                    min="0"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="0"
                  />
                </div>
                <div>
                  <label htmlFor="duration-seconds" className="block text-xs text-gray-600 mb-1">
                    Segundos
                  </label>
                  <input
                    id="duration-seconds"
                    type="number"
                    value={durationSeconds}
                    onChange={(e) => setDurationSeconds(e.target.value)}
                    min="0"
                    max="59"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="0"
                  />
                </div>
              </div>
            </div>

            {/* Publish Date and Time */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Fecha y Hora de Publicación <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="publish-date" className="block text-xs text-gray-600 mb-1">
                    Fecha
                  </label>
                  <input
                    id="publish-date"
                    type="date"
                    value={publishDate}
                    onChange={(e) => setPublishDate(e.target.value)}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                <div>
                  <label htmlFor="publish-time" className="block text-xs text-gray-600 mb-1">
                    Hora (UTC)
                  </label>
                  <input
                    id="publish-time"
                    type="time"
                    value={publishTime}
                    onChange={(e) => setPublishTime(e.target.value)}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            {/* Is Public */}
            <div className="flex items-center">
              <input
                id="episode-isPublic"
                type="checkbox"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <label htmlFor="episode-isPublic" className="ml-2 text-sm text-gray-700">
                Hacer público (visible para todos)
              </label>
            </div>

            {/* Error Message */}
            {createEpisodeMutation.isError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-600">
                  Error al crear el episodio. Verifica que todos los campos sean válidos e intenta de nuevo.
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
            disabled={createEpisodeMutation.isPending}
            className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            form="create-episode-form"
            disabled={createEpisodeMutation.isPending || !title.trim() || !description.trim() || !audioUrl.trim() || !durationMinutes}
            className="px-6 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {createEpisodeMutation.isPending ? 'Subiendo...' : 'Subir Episodio'}
          </button>
        </div>
      </div>
    </div>
  );
};
