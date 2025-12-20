import { apiRequest } from './apiClient';
import type { ApiResponse } from './types';

class FeatureFlagApi {
  async getAllFeatureFlags(): Promise<ApiResponse<Record<string, boolean>>> {
    try {
      return await apiRequest<ApiResponse<Record<string, boolean>>>('/api/v1/feature-flags');
    } catch (error) {
      console.error('Error fetching feature flags:', error);
      throw error;
    }
  }

  async isFeatureEnabled(featureName: string): Promise<ApiResponse<boolean>> {
    try {
      return await apiRequest<ApiResponse<boolean>>(`/api/v1/feature-flags/${featureName}`);
    } catch (error) {
      console.error(`Error checking feature flag '${featureName}':`, error);
      throw error;
    }
  }
}

export const featureFlagApi = new FeatureFlagApi();

