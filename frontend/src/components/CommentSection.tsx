import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { commentsAPI } from '../api/endpoints';
import { useAuth } from '../app/AuthProvider';
import { useUserCache } from '../hooks/useUserCache';
import type { Comment, CommentTarget, UserResponse } from '../api/types';

// Helper functions
const buildCommentTree = (comments: Comment[]): Comment[] => {
  const topLevel = comments.filter(c => !c.parentId);
  return topLevel.sort((a, b) =>
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
};

const getReplies = (commentId: string, allComments: Comment[]): Comment[] => {
  return allComments
    .filter(c => c.parentId === commentId)
    .sort((a, b) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
};

// Permission checking functions
const canEditComment = (comment: Comment, currentUser: UserResponse | null): boolean => {
  if (!currentUser) return false;
  return comment.userId === currentUser.id;
};

const canDeleteComment = (
  comment: Comment,
  currentUser: UserResponse | null,
  podcastCreatorId?: string
): boolean => {
  if (!currentUser) return false;
  if (comment.userId === currentUser.id) return true;
  if (podcastCreatorId && podcastCreatorId === currentUser.id) return true;
  return false;
};

interface CommentItemProps {
  comment: Comment;
  allComments: Comment[];
  target: CommentTarget;
  queryKey: string[];
  podcastCreatorId?: string;
  depth?: number;
}

const CommentItem: React.FC<CommentItemProps> = ({
  comment,
  allComments,
  target,
  queryKey,
  podcastCreatorId,
  depth = 0,
}) => {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const { data: user } = useUserCache(comment.userId);

  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyContent, setReplyContent] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);

  const replies = getReplies(comment.id, allComments);
  const isDeleted = comment.status === 'DELETED';

  // Reply mutation
  const createReplyMutation = useMutation({
    mutationFn: async (content: string) => {
      if (!currentUser) throw new Error('Usuario no autenticado');
      return commentsAPI.createComment({
        userId: currentUser.id,
        target,
        content,
        parentId: comment.id,
        status: 'VISIBLE',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setReplyContent('');
      setShowReplyForm(false);
    },
  });

  // Update mutation
  const updateCommentMutation = useMutation({
    mutationFn: async (content: string) => {
      return commentsAPI.updateComment(comment.id, {
        ...comment,
        content,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setIsEditing(false);
    },
  });

  // Delete mutation
  const deleteCommentMutation = useMutation({
    mutationFn: async () => {
      if (window.confirm('¿Estás seguro de que quieres eliminar este comentario?')) {
        return commentsAPI.deleteComment(comment.id);
      }
      throw new Error('Cancelado');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const handleReplySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (replyContent.trim()) {
      createReplyMutation.mutate(replyContent);
    }
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editContent.trim() && editContent !== comment.content) {
      updateCommentMutation.mutate(editContent);
    } else {
      setIsEditing(false);
    }
  };

  const handleDelete = () => {
    deleteCommentMutation.mutate();
  };

  const showEditButton = canEditComment(comment, currentUser) && !isDeleted;
  const showDeleteButton = canDeleteComment(comment, currentUser, podcastCreatorId) && !isDeleted;
  const showReplyButton = !isDeleted && depth < 3; // Limit nesting to 3 levels

  const paddingClass = depth > 0 ? 'pl-8' : '';
  const backgroundClass = depth > 0 ? 'bg-gray-100' : 'bg-gray-50';

  return (
    <div className={`${paddingClass}`}>
      <div className={`flex gap-3 p-4 ${backgroundClass} rounded-lg mb-3`}>
        <Link to={`/users/${comment.userId}`}>
          <img
            src={user?.avatarUrl || '/Imagenes/avatar.svg'}
            alt={user?.displayName || 'Usuario'}
            className="w-10 h-10 rounded-full object-cover flex-shrink-0"
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

          {/* Comment Content or Edit Form */}
          {isDeleted ? (
            <p className="text-gray-400 italic mt-1">{comment.content}</p>
          ) : isEditing ? (
            <form onSubmit={handleEditSubmit} className="mt-2 space-y-2">
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                rows={3}
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={!editContent.trim() || updateCommentMutation.isPending}
                  className="px-4 py-1 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                >
                  {updateCommentMutation.isPending ? 'Guardando...' : 'Guardar'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsEditing(false);
                    setEditContent(comment.content);
                  }}
                  className="px-4 py-1 text-sm bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </form>
          ) : (
            <p className="text-gray-700 mt-1">{comment.content}</p>
          )}

          {/* Timestamp and Actions */}
          <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
            <span>
              {new Date(comment.createdAt).toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>

            {!isDeleted && (
              <>
                {showReplyButton && (
                  <>
                    <span>•</span>
                    <button
                      onClick={() => setShowReplyForm(!showReplyForm)}
                      className="hover:text-blue-600 transition-colors"
                    >
                      Responder
                    </button>
                  </>
                )}

                {showEditButton && (
                  <>
                    <span>•</span>
                    <button
                      onClick={() => setIsEditing(true)}
                      className="hover:text-blue-600 transition-colors"
                    >
                      Editar
                    </button>
                  </>
                )}

                {showDeleteButton && (
                  <>
                    <span>•</span>
                    <button
                      onClick={handleDelete}
                      disabled={deleteCommentMutation.isPending}
                      className="hover:text-red-600 transition-colors disabled:opacity-50"
                    >
                      {deleteCommentMutation.isPending ? 'Eliminando...' : 'Eliminar'}
                    </button>
                  </>
                )}
              </>
            )}
          </div>

          {/* Reply Form */}
          {showReplyForm && (
            <form onSubmit={handleReplySubmit} className="mt-3 space-y-2">
              <textarea
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder="Escribe tu respuesta..."
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none text-sm"
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={!replyContent.trim() || createReplyMutation.isPending}
                  className="px-4 py-1 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                >
                  {createReplyMutation.isPending ? 'Enviando...' : 'Responder'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowReplyForm(false);
                    setReplyContent('');
                  }}
                  className="px-4 py-1 text-sm bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      {/* Nested Replies */}
      {replies.length > 0 && (
        <div className="space-y-0">
          {replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              allComments={allComments}
              target={target}
              queryKey={queryKey}
              podcastCreatorId={podcastCreatorId}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
};

interface CommentSectionProps {
  comments: Comment[];
  target: CommentTarget;
  queryKey: string[];
  podcastCreatorId?: string;
}

export const CommentSection: React.FC<CommentSectionProps> = ({
  comments,
  target,
  queryKey,
  podcastCreatorId,
}) => {
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

  const topLevelComments = buildCommentTree(comments);

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
      <div className="space-y-0">
        {topLevelComments.length === 0 ? (
          <p className="text-center text-gray-500 py-8">
            Aún no hay comentarios. ¡Sé el primero en comentar!
          </p>
        ) : (
          topLevelComments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              allComments={comments}
              target={target}
              queryKey={queryKey}
              podcastCreatorId={podcastCreatorId}
              depth={0}
            />
          ))
        )}
      </div>
    </div>
  );
};
