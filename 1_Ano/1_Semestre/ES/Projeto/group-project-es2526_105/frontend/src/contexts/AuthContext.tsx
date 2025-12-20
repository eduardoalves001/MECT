import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import keycloak from '@/config/keycloak';
import type Keycloak from 'keycloak-js';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  keycloak: Keycloak | null;
  login: () => void;
  logout: () => void;
  getToken: () => string | undefined;
  user: {
    username?: string;
    email?: string;
    firstName?: string;
    lastName?: string;
  } | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

let keycloakInitialized = false;

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<AuthContextType['user']>(null);

  useEffect(() => {
    const initKeycloak = async () => {
      if (keycloakInitialized) {
        setIsLoading(false);
        return;
      }

      try {
        keycloakInitialized = true;
        const authenticated = await keycloak.init({
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
          pkceMethod: 'S256',
        });

        setIsAuthenticated(authenticated);

        if (authenticated) {
          await keycloak.loadUserProfile();
          setUser({
            username: keycloak.profile?.username,
            email: keycloak.profile?.email,
            firstName: keycloak.profile?.firstName,
            lastName: keycloak.profile?.lastName,
          });
        }

        // Token refresh
        setInterval(() => {
          keycloak.updateToken(70).catch(() => {
            console.error('Failed to refresh token');
          });
        }, 60000);
      } catch (error) {
        console.error('Failed to initialize Keycloak', error);
      } finally {
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const login = () => {
    if (!keycloakInitialized) return;
    keycloak.login({
      redirectUri: window.location.origin,
    });
  };

  const logout = () => {
    if (!keycloakInitialized) return;
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  };

  const getToken = () => {
    if (!keycloakInitialized) return undefined;
    return keycloak.token;
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        keycloak,
        login,
        logout,
        getToken,
        user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
