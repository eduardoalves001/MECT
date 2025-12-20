import { useState, useMemo, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Plus, RefreshCw, AlertTriangle, Filter, ChevronDown, Check, ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import type { Vulnerability, Component, VulnerabilityStatus } from "@/api/types";
import { VULNERABILITY_STATUS_LABELS } from "@/api/types";
import { 
  getRiskLevelColor, 
  formatVulnerabilityStatus,
  calculateRiskLevel,
  getVulnerabilityStatusColor
} from "@/utils/vulnerability";
import { vulnerabilityApi } from "@/api/vulnerabilityApi";
import { AddVulnerabilityModal } from "@/components/custom/AddVulnerabilityModal";
import { VulnerabilityDetailModal } from "@/components/custom/VulnerabilityDetailModal";
import { toast } from "sonner";
import { PermissionGuard } from "@/components/custom/PermissionGuard";
import { useFeatureFlags } from "@/hooks/useFeatureFlags";

interface VulnerabilitiesTabProps {
  threatModelId: string;
  vulnerabilities: Vulnerability[];
  components: Component[];
  isLoading: boolean;
  onRefresh: (riskLevels?: string[], statuses?: string[]) => void;
  onComponentClick?: (componentId: string) => void;
  selectedRiskLevels: string[];
  setSelectedRiskLevels: (levels: string[]) => void;
  selectedStatuses: string[];
  setSelectedStatuses: (statuses: string[]) => void;
}

export function VulnerabilitiesTab({
  vulnerabilities,
  components,
  isLoading,
  onRefresh,
  selectedRiskLevels,
  setSelectedRiskLevels,
  selectedStatuses,
  setSelectedStatuses,
}: VulnerabilitiesTabProps) {
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedVulnerabilityId, setSelectedVulnerabilityId] = useState<string | null>(null);
  const [componentSort, setComponentSort] = useState<'asc' | 'desc' | null>(null);
  const [threatSort, setThreatSort] = useState<'asc' | 'desc' | null>(null);
  const [isRiskDropdownOpen, setIsRiskDropdownOpen] = useState(false);
  const [isStatusDropdownOpen, setIsStatusDropdownOpen] = useState(false);
  const { isFeatureEnabled } = useFeatureFlags();

  const uniqueRiskLevels = useMemo(() => {
    return ["CRITICAL", "HIGH", "MEDIUM", "LOW", "MINIMAL"];
  }, []);

  const uniqueStatuses = useMemo(() => {
    return ["IDENTIFIED", "ANALYSED", "IN_PROGRESS", "MITIGATED", "CLOSED"] as VulnerabilityStatus[];
  }, []);

  const sortedVulnerabilities = useMemo(() => {
    let result = [...vulnerabilities];

    if (componentSort) {
      result.sort((a, b) => {
        const comparison = a.component.name.localeCompare(b.component.name);
        return componentSort === 'asc' ? comparison : -comparison;
      });
    }

    if (threatSort) {
      result.sort((a, b) => {
        const comparison = a.threat.name.localeCompare(b.threat.name);
        return threatSort === 'asc' ? comparison : -comparison;
      });
    }

    return result;
  }, [vulnerabilities, componentSort, threatSort]);

  const activeFiltersCount = selectedRiskLevels.length + selectedStatuses.length;

  useEffect(() => {
    onRefresh(
      selectedRiskLevels.length > 0 ? selectedRiskLevels : undefined,
      selectedStatuses.length > 0 ? selectedStatuses : undefined
    );
  }, [selectedRiskLevels, selectedStatuses]);

  const handleToggleComponentSort = () => {
    if (componentSort === null) {
      setComponentSort('asc');
    } else if (componentSort === 'asc') {
      setComponentSort('desc');
    } else {
      setComponentSort(null);
    }
    setThreatSort(null); 
  };

  const handleToggleThreatSort = () => {
    if (threatSort === null) {
      setThreatSort('asc');
    } else if (threatSort === 'asc') {
      setThreatSort('desc');
    } else {
      setThreatSort(null);
    }
    setComponentSort(null); 
  };

  const handleToggleRiskLevel = (level: string) => {
    const newLevels = selectedRiskLevels.includes(level)
      ? selectedRiskLevels.filter(l => l !== level)
      : [...selectedRiskLevels, level];
    setSelectedRiskLevels(newLevels);
    setIsRiskDropdownOpen(false);
  };

  const handleToggleStatus = (status: VulnerabilityStatus) => {
    const newStatuses = selectedStatuses.includes(status)
      ? selectedStatuses.filter(s => s !== status)
      : [...selectedStatuses, status];
    setSelectedStatuses(newStatuses);
    setIsStatusDropdownOpen(false);
  };

  const handleClearFilters = () => {
    setSelectedRiskLevels([]);
    setSelectedStatuses([]);
  };

  const handleAddVulnerability = () => {
    setIsAddModalOpen(true);
  };

  const handleRowClick = (vulnerabilityId: string) => {
    setSelectedVulnerabilityId(vulnerabilityId);
    setIsDetailModalOpen(true);
  };

  const handleCloseDetailModal = () => {
    setIsDetailModalOpen(false);
    setSelectedVulnerabilityId(null);
  };

  const handleStatusChange = async (vulnerabilityId: string, newStatus: VulnerabilityStatus, vulnerability: Vulnerability) => {
    try {
      const response = await vulnerabilityApi.updateVulnerability(vulnerabilityId, {
        threatId: vulnerability.threat.id,
        likelihood: vulnerability.likelihood,
        impact: vulnerability.impact,
        status: newStatus,
        mitigationStrategies: vulnerability.mitigationStrategies || undefined,
      });

      if (response.success) {
        toast.success("Status updated successfully");
        onRefresh(selectedRiskLevels, selectedStatuses);
      } else {
        toast.error(response.message || "Failed to update status");
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to update status");
    }
  };

  return (
    <div className="grid gap-6">
      <div className="rounded-lg border p-6">
        <div className="flex justify-between items-center mb-4">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Vulnerabilities</h2>
          </div>
          <div className="flex gap-2">
            <Button onClick={() => onRefresh(selectedRiskLevels, selectedStatuses)} variant="outline" size="sm" className="h-8">
              <RefreshCw className="w-4 h-4 mr-2" />
              Refresh
            </Button>
            <PermissionGuard permission="vulnerability:create">
              <Button onClick={handleAddVulnerability} size="sm" className="h-8">
                <Plus className="w-4 h-4 mr-2" />
                Add Vulnerability
              </Button>
            </PermissionGuard>
          </div>
        </div>

        <div>
          <div className="mb-6 flex items-center justify-between">
            <p className="text-muted-foreground">
              Track and manage vulnerability instances for threats identified in your components
            </p>
            {activeFiltersCount > 0 && (
              <div className="flex items-center gap-2">
                <Badge variant="secondary" className="text-xs">
                  {activeFiltersCount} {activeFiltersCount === 1 ? 'filter' : 'filters'} active
                </Badge>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClearFilters}
                  className="h-8 text-xs"
                >
                  Clear filters
                </Button>
              </div>
            )}
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <div className="flex items-center gap-2">
                    <span>Component</span>
                    {isFeatureEnabled('enable_vulnerability_filtering') && (
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        className="h-6 w-6 p-0"
                        onClick={handleToggleComponentSort}
                      >
                        {componentSort === null && <ArrowUpDown className="h-4 w-4" />}
                        {componentSort === 'asc' && <ArrowUp className="h-4 w-4" />}
                        {componentSort === 'desc' && <ArrowDown className="h-4 w-4" />}
                      </Button>
                    )}
                  </div>
                </TableHead>
                <TableHead>
                  <div className="flex items-center gap-2">
                    <span>Threat</span>
                    {isFeatureEnabled('enable_vulnerability_filtering') && (
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        className="h-6 w-6 p-0"
                        onClick={handleToggleThreatSort}
                      >
                        {threatSort === null && <ArrowUpDown className="h-4 w-4" />}
                        {threatSort === 'asc' && <ArrowUp className="h-4 w-4" />}
                        {threatSort === 'desc' && <ArrowDown className="h-4 w-4" />}
                      </Button>
                    )}
                  </div>
                </TableHead>
                <TableHead className="w-[150px] text-center">
                  <div className="flex items-center justify-center gap-2">
                    <span>Risk</span>
                    {isFeatureEnabled('enable_vulnerability_filtering') && (
                      <DropdownMenu open={isRiskDropdownOpen} onOpenChange={setIsRiskDropdownOpen}>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm" className="h-6 w-6 p-0 relative">
                            <Filter className={`h-4 w-4 ${selectedRiskLevels.length > 0 ? 'text-primary' : ''}`} />
                            {selectedRiskLevels.length > 0 && (
                              <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-primary text-primary-foreground text-[10px] font-medium flex items-center justify-center">
                                {selectedRiskLevels.length}
                              </span>
                            )}
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="center" className="min-w-[120px] w-auto">
                          {uniqueRiskLevels.map((level) => (
                            <DropdownMenuItem
                              key={level}
                              onSelect={(e) => {
                                e.preventDefault();
                                handleToggleRiskLevel(level);
                              }}
                              className="flex items-center gap-2 cursor-pointer"
                            >
                              <div className="w-4 h-4 flex items-center justify-center">
                                {selectedRiskLevels.includes(level) && (
                                  <Check className="h-4 w-4" />
                                )}
                              </div>
                              <span>{level}</span>
                            </DropdownMenuItem>
                          ))}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    )}
                  </div>
                </TableHead>
                <TableHead className="w-[180px] text-center">
                  <div className="flex items-center justify-center gap-2">
                    <span>Status</span>
                    {isFeatureEnabled('enable_vulnerability_filtering') && (
                      <DropdownMenu open={isStatusDropdownOpen} onOpenChange={setIsStatusDropdownOpen}>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm" className="h-6 w-6 p-0 relative">
                            <Filter className={`h-4 w-4 ${selectedStatuses.length > 0 ? 'text-primary' : ''}`} />
                            {selectedStatuses.length > 0 && (
                              <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-primary text-primary-foreground text-[10px] font-medium flex items-center justify-center">
                                {selectedStatuses.length}
                              </span>
                            )}
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="center" className="min-w-[140px] w-auto">
                          {uniqueStatuses.map((status) => (
                            <DropdownMenuItem
                              key={status}
                              onSelect={(e) => {
                                e.preventDefault();
                                handleToggleStatus(status);
                              }}
                              className="flex items-center gap-2 cursor-pointer"
                            >
                              <div className="w-4 h-4 flex items-center justify-center">
                                {selectedStatuses.includes(status) && (
                                  <Check className="h-4 w-4" />
                                )}
                              </div>
                              <span>{formatVulnerabilityStatus(status)}</span>
                            </DropdownMenuItem>
                          ))}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    )}
                  </div>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center">
                    <RefreshCw className="w-4 h-4 animate-spin inline-block mr-2" />
                    Loading vulnerabilities...
                  </TableCell>
                </TableRow>
              ) : sortedVulnerabilities.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                    <AlertTriangle className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>{activeFiltersCount > 0 ? 'No vulnerabilities found matching the selected filters' : 'No vulnerabilities found'}</p>
                  </TableCell>
                </TableRow>
              ) : (
                sortedVulnerabilities.map((vulnerability) => (
                  <TableRow 
                    key={vulnerability.id} 
                    className={`hover:bg-muted/50 cursor-pointer transition-colors relative ${
                      vulnerability.status === 'MITIGATED' || vulnerability.status === 'CLOSED' 
                        ? 'opacity-60 after:content-[""] after:absolute after:left-0 after:right-0 after:top-1/2 after:h-[1px] after:bg-foreground/40' 
                        : ''
                    }`}
                    onClick={() => handleRowClick(vulnerability.id)}
                  >
                    <TableCell>
                      {vulnerability.component.name}
                    </TableCell>
                    <TableCell>
                      {vulnerability.threat.name}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-center">
                        <Badge 
                          className={`text-xs font-normal transition-colors ${getRiskLevelColor(calculateRiskLevel(vulnerability.likelihood, vulnerability.impact))}`}
                        >
                          {calculateRiskLevel(vulnerability.likelihood, vulnerability.impact)}
                        </Badge>
                      </div>
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-center">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Badge 
                              className={`cursor-pointer h-8 px-2 text-xs transition-colors ${getVulnerabilityStatusColor(vulnerability.status)}`}
                            >
                              {formatVulnerabilityStatus(vulnerability.status)}
                              <ChevronDown className="ml-1 h-3 w-3" />
                            </Badge>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {(Object.keys(VULNERABILITY_STATUS_LABELS) as VulnerabilityStatus[]).map((status) => (
                              <DropdownMenuItem
                                key={status}
                                onClick={() => handleStatusChange(vulnerability.id, status, vulnerability)}
                                disabled={status === vulnerability.status}
                              >
                                {VULNERABILITY_STATUS_LABELS[status]}
                              </DropdownMenuItem>
                            ))}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      {/* Add Vulnerability Modal */}
      <AddVulnerabilityModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        components={components}
        onSuccess={onRefresh}
      />

      {/* Vulnerability Detail Modal */}
      <VulnerabilityDetailModal
        isOpen={isDetailModalOpen}
        onClose={handleCloseDetailModal}
        vulnerabilityId={selectedVulnerabilityId}
        onSuccess={onRefresh}
      />
    </div>
  );
}
