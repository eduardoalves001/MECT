import { useMemo } from 'react';
import { useAuth } from '@/contexts/AuthContext';

export type Permission = 
  | 'threatmodel:create' | 'threatmodel:read' | 'threatmodel:update' | 'threatmodel:delete'
  | 'component:create' | 'component:read' | 'component:update' | 'component:delete'
  | 'vulnerability:create' | 'vulnerability:read' | 'vulnerability:update' | 'vulnerability:delete'
  | 'threat:create' | 'threat:read'
  | 'chatbot:use';

export function usePermissions() {
  const { keycloak, isAuthenticated } = useAuth();

  const userPermissions = useMemo<Set<Permission>>(() => {
    if (!isAuthenticated || !keycloak?.tokenParsed) {
      return new Set();
    }

    const tokenParsed = keycloak.tokenParsed as {
      realm_access?: {
        roles?: string[];
      };
    };

    const roles = tokenParsed.realm_access?.roles || [];
    
    return new Set(roles.filter(role => 
      role.includes(':')
    ) as Permission[]);
  }, [isAuthenticated, keycloak?.tokenParsed]);

  const hasPermission = (permission: Permission): boolean => {
    return userPermissions.has(permission);
  };

  const hasAnyPermission = (...permissions: Permission[]): boolean => {
    return permissions.some(permission => userPermissions.has(permission));
  };

  const hasAllPermissions = (...permissions: Permission[]): boolean => {
    return permissions.every(permission => userPermissions.has(permission));
  };

  return {
    permissions: userPermissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  };
}
