import { type ReactNode } from 'react';
import { usePermissions, type Permission } from '@/hooks/usePermissions';

interface PermissionGuardProps {
  children: ReactNode;
  permission?: Permission;
  anyOf?: Permission[];
  allOf?: Permission[];
  fallback?: ReactNode;
}

export function PermissionGuard({
  children,
  permission,
  anyOf,
  allOf,
  fallback = null,
}: PermissionGuardProps) {
  const { hasPermission, hasAnyPermission, hasAllPermissions } = usePermissions();

  let hasAccess = false;

  if (permission) {
    hasAccess = hasPermission(permission);
  } else if (anyOf && anyOf.length > 0) {
    hasAccess = hasAnyPermission(...anyOf);
  } else if (allOf && allOf.length > 0) {
    hasAccess = hasAllPermissions(...allOf);
  }

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
