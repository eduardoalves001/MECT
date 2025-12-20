import { apiRequest } from './apiClient';
import type { ApiResponse, Notification } from './types';

export const notificationApi = {
  getAllNotifications: () =>
    apiRequest<ApiResponse<Notification[]>>('/api/v1/notifications'),

  getUnreadNotifications: () =>
    apiRequest<ApiResponse<Notification[]>>('/api/v1/notifications/unread'),

  markAsRead: (notificationId: string) =>
    apiRequest<ApiResponse<void>>(`/api/v1/notifications/${notificationId}/read`, {
      method: 'PUT',
    }),

  markAllAsRead: () =>
    apiRequest<ApiResponse<void>>('/api/v1/notifications/read-all', {
      method: 'PUT',
    }),
};
