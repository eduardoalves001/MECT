import { useState, useEffect, useCallback } from 'react';
import { featureFlagApi } from '@/api';

interface FeatureFlagsState {
  flags: Record<string, boolean>;
  isLoading: boolean;
  error: string | null;
}


let globalFeatureFlags: Record<string, boolean> = {};
let globalLoading = true;
let globalError: string | null = null;
let subscribers: Set<() => void> = new Set();


const initializeFeatureFlags = async () => {
  try {
    globalLoading = true;
    globalError = null;
    const response = await featureFlagApi.getAllFeatureFlags();
    
    if (response.success && response.data) {
      globalFeatureFlags = response.data;
    } else {
      globalError = response.message || 'Failed to load feature flags';
    }
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : 'Network error';
    globalError = errorMessage;
    console.error('Failed to fetch feature flags:', err);
  } finally {
    globalLoading = false;
    notifySubscribers();
  }
};


const notifySubscribers = () => {
  subscribers.forEach(callback => callback());
};


initializeFeatureFlags();


export function useFeatureFlags() {
  const [state, setState] = useState<FeatureFlagsState>({
    flags: globalFeatureFlags,
    isLoading: globalLoading,
    error: globalError,
  });

  useEffect(() => {
    setState({
      flags: globalFeatureFlags,
      isLoading: globalLoading,
      error: globalError,
    });


    const updateState = () => {
      setState({
        flags: globalFeatureFlags,
        isLoading: globalLoading,
        error: globalError,
      });
    };

    subscribers.add(updateState);

    return () => {
      subscribers.delete(updateState);
    };
  }, []);

  const isFeatureEnabled = useCallback((featureName: string): boolean => {
    if (globalLoading) {
      console.warn(`Feature flag '${featureName}' checked while loading - returning false`);
      return false;
    }
    
    if (globalError) {
      console.warn(`Feature flag '${featureName}' checked with error state - returning false`);
      return false;
    }

    const isEnabled = globalFeatureFlags[featureName] ?? false;
    return isEnabled;
  }, [state.flags, state.isLoading, state.error]);


  const refreshFeatureFlags = useCallback(async () => {
    await initializeFeatureFlags();
  }, []);

  return {
    isFeatureEnabled,
    refreshFeatureFlags,
    isLoading: state.isLoading,
    error: state.error,
    allFlags: state.flags,
  };
}

