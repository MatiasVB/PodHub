import React, { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { subscriptionsAPI, usersAPI } from '../api/endpoints';

interface SubscribersModalProps {
  isOpen: boolean;
  onClose: () => void;
  podcastId: string;
}

export const SubscribersModal: React.FC<SubscribersModalProps> = ({ isOpen, onClose, podcastId }) => {
  // Fetch subscribers
  const { data: subscriptionsData, isLoading } = useQuery({
    queryKey: ['podcast-subscribers', podcastId],
    queryFn: async () => {
      const response = await subscriptionsAPI.listSubscriptionsByPodcast(podcastId, undefined, 100);
      return response.data;
    },
    enabled: isOpen,
  });

  // Extract unique user IDs from subscriptions
  const userIds = subscriptionsData?.data.map((sub) => sub.userId) || [];

  // Fetch user data for all subscribers
  const { data: usersData } = useQuery({
    queryKey: ['subscribers-users', userIds],
    queryFn: async () => {
      if (userIds.length === 0) return [];
      // Fetch all users in parallel
      const userPromises = userIds.map((userId) =>
        usersAPI.getUser(userId).then((res) => res.data)
      );
      return Promise.all(userPromises);
    },
    enabled: isOpen && userIds.length > 0,
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

  if (!isOpen) return null;

  return (
    <div className="modal-overlay fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full max-h-[80vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 className="text-xl font-bold text-gray-900">
            Suscriptores {subscriptionsData && `(${subscriptionsData.data.length})`}
          </h3>
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
          {isLoading ? (
            <div className="flex justify-center py-10">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
            </div>
          ) : !usersData || usersData.length === 0 ? (
            <div className="text-center py-10">
              <svg
                className="w-16 h-16 mx-auto text-gray-300 mb-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
                />
              </svg>
              <p className="text-gray-500">Aún no hay suscriptores</p>
            </div>
          ) : (
            <div className="space-y-2">
              {usersData.map((user) => (
                <Link
                  key={user.id}
                  to={`/users/${user.id}`}
                  onClick={onClose}
                  className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors group"
                >
                  {/* Avatar */}
                  <img
                    src={user.avatarUrl || '/Imagenes/default-avatar.svg'}
                    alt={user.username}
                    className="w-10 h-10 rounded-full object-cover flex-shrink-0"
                    onError={(e) => {
                      e.currentTarget.src = '/Imagenes/default-avatar.svg';
                    }}
                  />
                  {/* User Info */}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 group-hover:text-blue-600 truncate">
                      {user.username}
                    </p>
                    {user.displayName && user.displayName !== user.username && (
                      <p className="text-sm text-gray-500 truncate">{user.displayName}</p>
                    )}
                  </div>
                  {/* Arrow icon */}
                  <svg
                    className="w-5 h-5 text-gray-400 group-hover:text-blue-600 flex-shrink-0"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
