import type { StrideCategory } from '@/api/types';

/**
 * Format STRIDE category enum value to display name
 */
export function formatStrideCategory(category: StrideCategory | null | undefined): string {
  if (!category) return 'N/A';
  
  const categoryMap: Record<StrideCategory, string> = {
    SPOOFING: 'Spoofing',
    TAMPERING: 'Tampering',
    REPUDIATION: 'Repudiation',
    INFORMATION_DISCLOSURE: 'Information Disclosure',
    DENIAL_OF_SERVICE: 'Denial of Service',
    ELEVATION_OF_PRIVILEGE: 'Elevation of Privilege',
  };
  
  return categoryMap[category] || category;
}

/**
 * Get description for a STRIDE category
 */
export function getStrideCategoryDescription(category: StrideCategory): string {
  const descriptions: Record<StrideCategory, string> = {
    SPOOFING: 'Pretending to be something or someone other than yourself',
    TAMPERING: 'Modifying data or code',
    REPUDIATION: 'Claiming you didn\'t do something or denying an action',
    INFORMATION_DISCLOSURE: 'Exposing information to unauthorized parties',
    DENIAL_OF_SERVICE: 'Making a system unavailable or unusable',
    ELEVATION_OF_PRIVILEGE: 'Gaining unauthorized access or permissions',
  };
  
  return descriptions[category];
}

/**
 * Get color class for a STRIDE category badge
 */
export function getStrideCategoryColor(category: StrideCategory | null | undefined): string {
  if (!category) return 'bg-gray-100 text-gray-800';
  
  const colorMap: Record<StrideCategory, string> = {
    SPOOFING: 'bg-purple-100 text-purple-800',
    TAMPERING: 'bg-orange-100 text-orange-800',
    REPUDIATION: 'bg-yellow-100 text-yellow-800',
    INFORMATION_DISCLOSURE: 'bg-red-100 text-red-800',
    DENIAL_OF_SERVICE: 'bg-blue-100 text-blue-800',
    ELEVATION_OF_PRIVILEGE: 'bg-pink-100 text-pink-800',
  };
  
  return colorMap[category] || 'bg-gray-100 text-gray-800';
}
