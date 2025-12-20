import type { Component, ComponentRequest, ApiResponse } from './types';
import { apiRequest } from './apiClient';

class ComponentApi {
  async getAllComponents(threatModelId: string, search?: string): Promise<ApiResponse<Component[]>> {
    try {
      const params = new URLSearchParams();
      if (search && search.trim() !== '') {
        params.append('search', search.trim());
      }
      const queryString = params.toString();
      const url = `/api/v1/threat-models/${threatModelId}/components${queryString ? `?${queryString}` : ''}`;
      return await apiRequest<ApiResponse<Component[]>>(url);
    } catch (error) {
      console.error('Error fetching components:', error);
      throw error;
    }
  }

  async getComponentById(threatModelId: string, id: string): Promise<ApiResponse<Component>> {
    try {
      return await apiRequest<ApiResponse<Component>>(`/api/v1/threat-models/${threatModelId}/components/${id}`);
    } catch (error) {
      console.error('Error fetching component:', error);
      throw error;
    }
  }

  async createComponent(threatModelId: string, component: ComponentRequest): Promise<ApiResponse<Component>> {
    try {
      return await apiRequest<ApiResponse<Component>>(`/api/v1/threat-models/${threatModelId}/components`, {
        method: 'POST',
        body: JSON.stringify(component),
      });
    } catch (error) {
      console.error('Error creating component:', error);
      throw error;
    }
  }

  async updateComponent(threatModelId: string, id: string, component: ComponentRequest): Promise<ApiResponse<Component>> {
    try {
      return await apiRequest<ApiResponse<Component>>(`/api/v1/threat-models/${threatModelId}/components/${id}`, {
        method: 'PUT',
        body: JSON.stringify(component),
      });
    } catch (error) {
      console.error('Error updating component:', error);
      throw error;
    }
  }

  async deleteComponent(threatModelId: string, id: string): Promise<ApiResponse<null>> {
    try {
      return await apiRequest<ApiResponse<null>>(`/api/v1/threat-models/${threatModelId}/components/${id}`, {
        method: 'DELETE',
      });
    } catch (error) {
      console.error('Error deleting component:', error);
      throw error;
    }
  }
}

export const componentApi = new ComponentApi();
export { ComponentApi };
