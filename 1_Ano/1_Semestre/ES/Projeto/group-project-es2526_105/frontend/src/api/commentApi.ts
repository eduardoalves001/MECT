import { apiRequest } from './apiClient';
import type { ApiResponse, Comment, CommentRequest } from './types';

export const commentApi = {
  createComment: (request: CommentRequest) =>
    apiRequest<ApiResponse<Comment>>('/api/v1/comments', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  getCommentsByVulnerability: (vulnerabilityId: string) =>
    apiRequest<ApiResponse<Comment[]>>(`/api/v1/comments/vulnerability/${vulnerabilityId}`),

  getCommentsByComponent: (componentId: string) =>
    apiRequest<ApiResponse<Comment[]>>(`/api/v1/comments/component/${componentId}`),

  deleteComment: (commentId: string) =>
    apiRequest<ApiResponse<void>>(`/api/v1/comments/${commentId}`, {
      method: 'DELETE',
    }),
};
