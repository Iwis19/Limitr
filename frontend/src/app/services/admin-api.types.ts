export interface RuleConfig {
  id?: number;
  baseLimitPerMinute: number;
  throttledLimitPerMinute: number;
  warnThreshold: number;
  throttleThreshold: number;
  banThreshold: number;
  banMinutes: number;
}

export interface TopOffender {
  principalId: string;
  incidents: number;
}

export type SystemStatusLevel = 'operational' | 'warning' | 'degraded';

export interface SystemStatus {
  level: SystemStatusLevel;
  label: string;
  detail: string;
  serverErrorsLastHour: number;
  activeBans: number;
}

export interface AdminStats {
  requestsPerMinute: number;
  activeBans: number;
  topOffenders: TopOffender[];
  rules: RuleConfig;
  systemStatus: SystemStatus;
}

export interface RequestLogItem {
  id: number;
  timestamp: string;
  principalId: string;
  ipAddress: string;
  httpMethod: string;
  path: string;
  statusCode: number;
  latencyMs: number;
}

export interface IncidentItem {
  id: number;
  timestamp: string;
  principalId: string;
  ruleTriggered: string;
  score: number;
  actionTaken: string;
  expiresAt: string | null;
}

export interface ListResponse<T> {
  items: T[];
}

export interface RulesUpdatePayload {
  baseLimitPerMinute: number | null;
  throttledLimitPerMinute: number | null;
  warnThreshold: number | null;
  throttleThreshold: number | null;
  banThreshold: number | null;
  banMinutes: number | null;
}

export interface BanActionResponse {
  message: string;
  principalId: string;
  minutes: number;
}

export interface UnbanActionResponse {
  message: string;
  principalId: string;
}

export interface LogsFilter {
  principalId?: string;
  statusCode?: string;
}
