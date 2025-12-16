import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersAPI, podcastsAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { CreatePodcastModal } from '../components/CreatePodcastModal';
import { EditPodcastModal } from '../components/EditPodcastModal';
import type { Podcast } from '../api/types';

export const UserProfilePage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const { user: currentUser, refreshUser } = useAuth();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [editedDisplayName, setEditedDisplayName] = useState('');
  const [editedBio, setEditedBio] = useState('');
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [showCreatePodcastModal, setShowCreatePodcastModal] = useState(false);
  const [podcastToDelete, setPodcastToDelete] = useState<string | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [podcastToEdit, setPodcastToEdit] = useState<Podcast | null>(null);

  if (!userId) {
    return <div>Usuario no encontrado</div>;
  }

  const isOwnProfile = currentUser?.id === userId;

  // Fetch user details
  const { data: user, isLoading, error } = useQuery({
    queryKey: ['user', userId],
    queryFn: async () => {
      const response = await usersAPI.getUser(userId);
      return response.data;
    },
  });

  // Fetch user's podcasts (will show if user has created any podcasts)
  const { data: userPodcasts } = useQuery({
    queryKey: ['podcasts', 'creator', userId],
    queryFn: async () => {
      const response = await podcastsAPI.listPodcastsByCreator(userId, undefined, 20);
      return response.data;
    },
    enabled: !!user, // Always try to fetch podcasts if user exists
  });

  // Update user mutation
  const updateUserMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error('Usuario no encontrado');

      let avatarUrl = user.avatarUrl;

      // Convert image to base64 if a new file was selected
      if (avatarFile) {
        avatarUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result as string);
          reader.readAsDataURL(avatarFile);
        });
      }

      // Update only the allowed fields
      const updatedUser = {
        ...user,
        displayName: editedDisplayName || user.displayName,
        bio: editedBio,
        avatarUrl,
      };

      const response = await usersAPI.updateUser(userId, updatedUser);
      return response.data;
    },
    onSuccess: (updatedUser) => {
      queryClient.invalidateQueries({ queryKey: ['user', userId] });
      
      // Update current user in auth context if editing own profile
      if (isOwnProfile) {
        const rememberMe = localStorage.getItem('rememberMe') === 'true';
        const storage = rememberMe ? localStorage : sessionStorage;
        storage.setItem('user', JSON.stringify(updatedUser));
        refreshUser();
      }
      
      setIsEditing(false);
      setAvatarFile(null);
      setAvatarPreview(null);
    },
  });

  // Delete podcast mutation
  const deletePodcastMutation = useMutation({
    mutationFn: async (podcastId: string) => {
      await podcastsAPI.deletePodcast(podcastId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['podcasts', 'creator', userId] });
      queryClient.invalidateQueries({ queryKey: ['podcasts'] });
      setPodcastToDelete(null);
    },
  });

  const handleEditClick = () => {
    if (user) {
      setEditedDisplayName(user.displayName);
      setEditedBio(user.bio || '');
      setIsEditing(true);
    }
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setAvatarFile(null);
    setAvatarPreview(null);
  };

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setAvatarFile(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setAvatarPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateUserMutation.mutate();
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
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
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Error al cargar perfil</h2>
        <p className="text-gray-600">No se pudo cargar la información del usuario</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="text-center py-20">
        <h2 className="text-2xl font-bold text-gray-900">Usuario no encontrado</h2>
      </div>
    );
  }

  const isCreator = user.roles?.some(role =>
    role === 'CREATOR' || role === 'ROLE_CREATOR' || role.includes('CREATOR')
  ) || false;

  return (
    <div className="space-y-8">
      {/* Profile Header */}
      <div className="bg-white rounded-2xl shadow-xl p-8">
        <div className="flex flex-col md:flex-row gap-8">
          {/* Avatar */}
          <div className="flex flex-col items-center">
            <img
              src={avatarPreview || user.avatarUrl || '/Imagenes/avatar.jpg'}
              alt={user.displayName}
              className="w-32 h-32 rounded-full object-cover border-4 border-gray-200 shadow-lg"
              onError={(e) => {
                e.currentTarget.src = '/Imagenes/avatar.jpg';
              }}
            />
            {isEditing && (
              <label className="mt-4 px-4 py-2 bg-blue-100 text-blue-600 rounded-lg cursor-pointer hover:bg-blue-200 transition-colors text-sm font-medium">
                Cambiar Foto
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="hidden"
                />
              </label>
            )}
          </div>

          {/* User Info */}
          <div className="flex-1">
            {isEditing ? (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Nombre para mostrar
                  </label>
                  <input
                    type="text"
                    value={editedDisplayName}
                    onChange={(e) => setEditedDisplayName(e.target.value)}
                    required
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Biografía
                  </label>
                  <textarea
                    value={editedBio}
                    onChange={(e) => setEditedBio(e.target.value)}
                    rows={4}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                    placeholder="Cuéntanos algo sobre ti..."
                  />
                </div>

                <div className="flex gap-3">
                  <button
                    type="submit"
                    disabled={updateUserMutation.isPending}
                    className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50"
                  >
                    {updateUserMutation.isPending ? 'Guardando...' : 'Guardar Cambios'}
                  </button>
                  <button
                    type="button"
                    onClick={handleCancelEdit}
                    className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors"
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            ) : (
              <>
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">{user.displayName}</h1>
                    <p className="text-gray-600">@{user.username}</p>
                  </div>
                  {isOwnProfile && (
                    <div className="flex gap-3">
                      <button
                        onClick={handleEditClick}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
                      >
                        Editar Perfil
                      </button>
                      <button
                        onClick={() => setShowCreatePodcastModal(true)}
                        className="px-4 py-2 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 transition-colors"
                      >
                        Crear Podcast
                      </button>
                    </div>
                  )}
                </div>

                {user.bio && (
                  <p className="text-gray-700 mb-4">{user.bio}</p>
                )}

                <div className="flex items-center gap-4 text-sm text-gray-600">
                  <span className="flex items-center gap-1">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                      />
                    </svg>
                    {user.email}
                  </span>
                  <span className="flex items-center gap-1">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                      />
                    </svg>
                    Miembro desde {new Date(user.createdAt).toLocaleDateString('es-ES', { month: 'short', year: 'numeric' })}
                  </span>
                </div>

                {isCreator && (
                  <div className="mt-4">
                    <span className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-700 rounded-full font-medium text-sm">
                      <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                      Creador de Contenido
                    </span>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* User's Podcasts */}
      {userPodcasts && userPodcasts.data.length > 0 && (
        <div className="bg-white rounded-xl shadow-md p-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Podcasts de {user.displayName}</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {userPodcasts.data.map((podcast) => (
              <div key={podcast.id} className="group bg-gray-50 rounded-xl overflow-hidden hover:shadow-lg transition-all relative">
                {/* Image - clickable to podcast detail */}
                <Link to={`/podcasts/${podcast.id}`} className="block">
                  <div className="aspect-square overflow-hidden bg-gradient-to-br from-blue-100 to-purple-100">
                    <img
                      src={podcast.coverImageUrl || '/Imagenes/fotopodcast.jpg'}
                      alt={podcast.title}
                      className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                      onError={(e) => {
                        e.currentTarget.src = '/Imagenes/fotopodcast.jpg';
                      }}
                    />
                  </div>
                </Link>

                {/* Content */}
                <div className="p-4">
                  <Link to={`/podcasts/${podcast.id}`}>
                    <h3 className="font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                      {podcast.title}
                    </h3>
                    <p className="text-gray-600 text-sm line-clamp-2 mt-1">{podcast.description}</p>
                  </Link>

                  {/* Action buttons - only for own profile */}
                  {isOwnProfile && (
                    <div className="flex gap-2 mt-3">
                      <button
                        onClick={() => {
                          setPodcastToEdit(podcast);
                          setShowEditModal(true);
                        }}
                        className="flex-1 px-3 py-2 bg-blue-100 text-blue-700 rounded-lg text-sm font-medium hover:bg-blue-200 transition-colors"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => setPodcastToDelete(podcast.id)}
                        className="flex-1 px-3 py-2 bg-red-100 text-red-700 rounded-lg text-sm font-medium hover:bg-red-200 transition-colors"
                      >
                        Eliminar
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Create Podcast Modal */}
      <CreatePodcastModal
        isOpen={showCreatePodcastModal}
        onClose={() => setShowCreatePodcastModal(false)}
        userId={userId}
      />

      {/* Edit Podcast Modal */}
      {showEditModal && podcastToEdit && (
        <EditPodcastModal
          isOpen={showEditModal}
          onClose={() => {
            setShowEditModal(false);
            setPodcastToEdit(null);
          }}
          podcast={podcastToEdit}
          userId={userId}
        />
      )}

      {/* Delete Confirmation Dialog */}
      {podcastToDelete && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6">
            <h3 className="text-xl font-bold text-gray-900 mb-4">Confirmar Eliminación</h3>
            <p className="text-gray-600 mb-6">
              ¿Estás seguro que deseas eliminar este podcast? Esta acción no se puede deshacer.
              Todos los episodios y comentarios asociados también podrían verse afectados.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setPodcastToDelete(null)}
                disabled={deletePodcastMutation.isPending}
                className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition-colors disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={() => deletePodcastMutation.mutate(podcastToDelete)}
                disabled={deletePodcastMutation.isPending}
                className="px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {deletePodcastMutation.isPending ? 'Eliminando...' : 'Eliminar'}
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


