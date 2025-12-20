import keycloak from '@/config/keycloak';

const API_BASE_URL = import.meta.env.VITE_BACKEND_URL;

export class ApiError extends Error {
  status: number;
  errorType?: string;
  retryAfterSeconds?: number;

  constructor(
    message: string,
    status: number,
    errorType?: string,
    retryAfterSeconds?: number
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorType = errorType;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export interface ApiRequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean>;
}

async function getAuthHeaders(): Promise<HeadersInit> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (keycloak.token) {
    try {
      // Refresh token if needed
      await keycloak.updateToken(30);
      headers['Authorization'] = `Bearer ${keycloak.token}`;
    } catch (error) {
      console.error('Failed to refresh token', error);
      // If token refresh fails, user will need to re-login
      keycloak.login();
    }
  }

  return headers;
}

export async function apiRequest<T>(
  endpoint: string,
  options: ApiRequestOptions = {}
): Promise<T> {
  const { params, ...fetchOptions } = options;

  let url = `${API_BASE_URL}${endpoint}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      searchParams.append(key, String(value));
    });
    url += `?${searchParams.toString()}`;
  }

  const headers = await getAuthHeaders();

  const response = await fetch(url, {
    ...fetchOptions,
    headers: {
      ...headers,
      ...fetchOptions.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: response.statusText,
    }));
    
    const errorMessage = error.message || error.error || 'API request failed';
    const errorType = error.data?.errorType;
    const retryAfterSeconds = error.data?.retryAfterSeconds;
    
    throw new ApiError(errorMessage, response.status, errorType, retryAfterSeconds);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export async function apiBlobRequest(
  endpoint: string,
  options: ApiRequestOptions = {}
): Promise<Blob> {
  const { params, ...fetchOptions } = options;

  let url = `${API_BASE_URL}${endpoint}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      searchParams.append(key, String(value));
    });
    url += `?${searchParams.toString()}`;
  }

  const headers = await getAuthHeaders();

  const response = await fetch(url, {
    ...fetchOptions,
    headers: {
      ...headers,
      ...fetchOptions.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: response.statusText,
    }));
    
    const errorMessage = error.message || error.error || 'API request failed';
    const errorType = error.data?.errorType;
    const retryAfterSeconds = error.data?.retryAfterSeconds;
    
    throw new ApiError(errorMessage, response.status, errorType, retryAfterSeconds);
  }

  return response.blob();
}

export { API_BASE_URL };
