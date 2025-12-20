import { apiRequest } from './apiClient';
import type { ApiResponse } from './types';

export async function sendChatMessage(message: string, sessionId: string): Promise<ApiResponse<string>> {
  return await apiRequest<ApiResponse<string>>('/api/v1/agent/chat', {
    method: 'POST',
    body: JSON.stringify({ message, sessionId }),
  });
}
