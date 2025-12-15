import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { commentsAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { useUserCache } from '../hooks/useUserCache';
import type { Comment, CommentTarget } from '../api/types';

interface CommentItemProps {
  comment: Comment;
}

const CommentItem: React.FC<CommentItemProps> = ({ comment }) => {
  const { data: user } = useUserCache(comment.userId);

  return (
    <div className="flex gap-3 p-4 bg-gray-50 rounded-lg">
      <Link to={`/users/${comment.userId}`}>
        <img
          src={user?.avatarUrl || '/Imagenes/avatar.svg'}
          alt={user?.displayName || 'Usuario'}
          className="w-10 h-10 rounded-full object-cover"
          onError={(e) => {
            e.currentTarget.src = '/Imagenes/avatar.svg';
          }}
        />
      </Link>
      <div className="flex-1">
        <Link
          to={`/users/${comment.userId}`}
          className="font-semibold text-gray-900 hover:text-blue-600"
        >
          {user?.displayName || 'Cargando...'}
        </Link>
        <p className="text-gray-700 mt-1">{comment.content}</p>
        <span className="text-xs text-gray-500 mt-2 block">
          {new Date(comment.createdAt).toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>
      </div>
    </div>
  );
};

interface CommentSectionProps {
  comments: Comment[];
  target: CommentTarget;
  queryKey: string[];
}

export const CommentSection: React.FC<CommentSectionProps> = ({ comments, target, queryKey }) => {
  const { user } = useAuth();
  const [newComment, setNewComment] = useState('');
  const queryClient = useQueryClient();

  const createCommentMutation = useMutation({
    mutationFn: async (content: string) => {
      if (!user) throw new Error('Usuario no autenticado');
      return commentsAPI.createComment({
        userId: user.id,
        target,
        content,
        status: 'VISIBLE',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setNewComment('');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (newComment.trim()) {
      createCommentMutation.mutate(newComment);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-md p-6 space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">
        Comentarios ({comments.length})
      </h2>

      {/* Add Comment Form */}
      <form onSubmit={handleSubmit} className="space-y-3">
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Escribe tu comentario..."
          rows={3}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
        />
        <button
          type="submit"
          disabled={!newComment.trim() || createCommentMutation.isPending}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {createCommentMutation.isPending ? 'Publicando...' : 'Publicar Comentario'}
        </button>
      </form>

      {/* Comments List */}
      <div className="space-y-4">
        {comments.length === 0 ? (
          <p className="text-center text-gray-500 py-8">
            Aún no hay comentarios. ¡Sé el primero en comentar!
          </p>
        ) : (
          comments.map((comment) => <CommentItem key={comment.id} comment={comment} />)
        )}
      </div>
    </div>
  );
};


