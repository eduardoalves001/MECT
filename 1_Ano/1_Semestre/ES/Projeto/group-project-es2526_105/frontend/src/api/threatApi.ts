import type { Threat, ApiResponse } from './types';
import { apiRequest } from './apiClient';

class ThreatApi {
  async getAllThreats(): Promise<ApiResponse<Threat[]>> {
    try {
      return await apiRequest<ApiResponse<Threat[]>>('/api/v1/threats');
    } catch (error) {
      console.error('Error fetching threats:', error);
      throw error;
    }
  }

  async getThreatById(id: string): Promise<ApiResponse<Threat>> {
    try {
      return await apiRequest<ApiResponse<Threat>>(`/api/v1/threats/${id}`);
    } catch (error) {
      console.error('Error fetching threat:', error);
      throw error;
    }
  }
}

export const threatApi = new ThreatApi();
export { ThreatApi };
