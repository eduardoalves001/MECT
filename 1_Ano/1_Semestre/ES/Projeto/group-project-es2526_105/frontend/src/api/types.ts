// API Types based on the backend ThreatModel and Component entities and ApiResponse
export interface ThreatModel {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
}

export interface ThreatModelRequest {
  name: string;
  description?: string | null;
}

export interface ThreatModelStats {
  totalComponents: number;
  activeThreats: number;
  highRiskThreats: number;
  mitigatedThreats: number;
}

export type ThreatModelFilter = 
  | "ALL"
  | "ACTIVE_THREATS"
  | "HIGH_RISK_THREATS"
  | "MITIGATED_THREATS";

export const THREAT_MODEL_FILTER_LABELS: Record<ThreatModelFilter, string> = {
  ALL: "All Models",
  ACTIVE_THREATS: "Models with Active Threats", 
  HIGH_RISK_THREATS: "Models with High Risk Threats",
  MITIGATED_THREATS: "Models with Mitigated Threats"
};

export interface ThreatsByCategory {
  category: StrideCategory;
  count: number;
}

export interface RiskDistribution {
  riskLevel: string;
  count: number;
}

export interface Component {
  id: string;
  name: string;
  description: string | null;
}

export interface ComponentRequest {
  name: string;
  description?: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  error: string | null;
}

export interface ApiError {
  success: false;
  message: string;
  data: null;
  error: string;
}


export type StrideCategory = 
  | "SPOOFING"
  | "TAMPERING"
  | "REPUDIATION"
  | "INFORMATION_DISCLOSURE"
  | "DENIAL_OF_SERVICE"
  | "ELEVATION_OF_PRIVILEGE";

export const STRIDE_LABELS: Record<StrideCategory, string> = {
  SPOOFING: "Spoofing",
  TAMPERING: "Tampering",
  REPUDIATION: "Repudiation",
  INFORMATION_DISCLOSURE: "Information Disclosure",
  DENIAL_OF_SERVICE: "Denial of Service",
  ELEVATION_OF_PRIVILEGE: "Elevation of Privilege"
};

export interface Threat {
  id: string;
  name: string;
  description?: string | null;
  category?: StrideCategory | null;
}

export type VulnerabilityStatus = 
  | "IDENTIFIED"
  | "ANALYSED"
  | "IN_PROGRESS"
  | "MITIGATED"
  | "CLOSED";

export const VULNERABILITY_STATUS_LABELS: Record<VulnerabilityStatus, string> = {
  IDENTIFIED: "Identified",
  ANALYSED: "Analysed",
  IN_PROGRESS: "In Progress",
  MITIGATED: "Mitigated",
  CLOSED: "Closed"
};

export interface Vulnerability {
  id: string;
  component: {
    id: string;
    name: string;
  };
  threat: {
    id: string;
    name: string;
    description?: string | null;
    category?: StrideCategory | null;
  };
  likelihood: number;
  impact: number;
  riskScore: number;
  status: VulnerabilityStatus;
  mitigationStrategies?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VulnerabilityRequest {
  threatId: string;
  likelihood: number;
  impact: number;
  status?: VulnerabilityStatus;
  mitigationStrategies?: string | null;
}

export interface Comment {
  id: string;
  content: string;
  authorUserId: string;
  authorUsername: string;
  vulnerabilityId?: string | null;
  componentId?: string | null;
  parentCommentId?: string | null;
  replies: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface CommentRequest {
  content: string;
  vulnerabilityId?: string | null;
  componentId?: string | null;
  parentCommentId?: string | null;
}

export interface Notification {
  id: string;
  recipientUserId: string;
  message: string;
  type: string;
  commentId?: string | null;
  isRead: boolean;
  createdAt: string;
}