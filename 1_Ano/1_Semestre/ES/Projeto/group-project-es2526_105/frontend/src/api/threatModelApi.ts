import type { ThreatModel, ThreatModelRequest, ThreatModelStats, ThreatsByCategory, RiskDistribution, ApiResponse, ThreatModelFilter } from './types';
import { apiRequest, apiBlobRequest } from './apiClient';

class ThreatModelApi {

  async getAllThreatModels(search?: string, filter?: ThreatModelFilter): Promise<ApiResponse<ThreatModel[]>> {
    try {
      const params = new URLSearchParams();
      if (search && search.trim() !== '') {
        params.append('search', search.trim());
      }
      if (filter && filter !== 'ALL') {
        params.append('filter', filter);
      }
      const queryString = params.toString();
      const url = `/api/v1/threat-models${queryString ? `?${queryString}` : ''}`;
      return await apiRequest<ApiResponse<ThreatModel[]>>(url);
    } catch (error) {
      console.error('Error fetching threat models:', error);
      throw error;
    }
  }

  async getThreatModelById(id: string): Promise<ApiResponse<ThreatModel>> {
    try {
      return await apiRequest<ApiResponse<ThreatModel>>(`/api/v1/threat-models/${id}`);
    } catch (error) {
      console.error('Error fetching threat model:', error);
      throw error;
    }
  }

  async createThreatModel(model: ThreatModelRequest): Promise<ApiResponse<ThreatModel>> {
    try {
      return await apiRequest<ApiResponse<ThreatModel>>('/api/v1/threat-models', {
        method: 'POST',
        body: JSON.stringify(model),
      });
    } catch (error) {
      console.error('Error creating threat model:', error);
      throw error;
    }
  }

  async updateThreatModel(id: string, model: ThreatModelRequest): Promise<ApiResponse<ThreatModel>> {
    try {
      return await apiRequest<ApiResponse<ThreatModel>>(`/api/v1/threat-models/${id}`, {
        method: 'PUT',
        body: JSON.stringify(model),
      });
    } catch (error) {
      console.error('Error updating threat model:', error);
      throw error;
    }
  }

  async deleteThreatModel(id: string): Promise<ApiResponse<null>> {
    try {
      return await apiRequest<ApiResponse<null>>(`/api/v1/threat-models/${id}`, {
        method: 'DELETE',
      });
    } catch (error) {
      console.error('Error deleting threat model:', error);
      throw error;
    }
    }

  async getThreatModelStats(id: string): Promise<ApiResponse<ThreatModelStats>> {
    try {
      return await apiRequest<ApiResponse<ThreatModelStats>>(`/api/v1/threat-models/${id}/stats`);
    } catch (error) {
      console.error('Error fetching threat model stats:', error);
      throw error;
    }
  }

  async getThreatsByCategory(id: string): Promise<ApiResponse<ThreatsByCategory[]>> {
    try {
      return await apiRequest<ApiResponse<ThreatsByCategory[]>>(`/api/v1/threat-models/${id}/threats-by-category`);
    } catch (error) {
      console.error('Error fetching threats by category:', error);
      throw error;
    }
  }

  async getRiskDistribution(id: string): Promise<ApiResponse<RiskDistribution[]>> {
    try {
      return await apiRequest<ApiResponse<RiskDistribution[]>>(`/api/v1/threat-models/${id}/risk-distribution`);
    } catch (error) {
      console.error('Error fetching risk distribution:', error);
      throw error;
    }
  }

  async exportToPdf(id: string, filename?: string): Promise<void> {
    try {
      const blob = await apiBlobRequest(`/api/v1/threat-models/${id}/export/pdf`);
      this.downloadFile(blob, filename || `threat-model-${new Date().toISOString().split('T')[0]}.pdf`);
    } catch (error) {
      console.error('Error exporting to PDF:', error);
      throw error;
    }
  }

  async exportToCsv(id: string, filename?: string): Promise<void> {
    try {
      const blob = await apiBlobRequest(`/api/v1/threat-models/${id}/export/csv`);
      this.downloadFile(blob, filename || `threat-model-${new Date().toISOString().split('T')[0]}.csv`);
    } catch (error) {
      console.error('Error exporting to CSV:', error);
      throw error;
    }
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}

export const threatModelApi = new ThreatModelApi();
export { ThreatModelApi };
