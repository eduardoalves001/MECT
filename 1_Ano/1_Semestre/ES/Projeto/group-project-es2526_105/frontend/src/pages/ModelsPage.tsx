import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableRow } from '@/components/ui/table';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { RefreshCw, Shield, Plus, MoreHorizontal, Edit, Trash2, Search, X, AlertTriangle, CheckCircle, Activity } from 'lucide-react';
import { threatModelApi } from '@/api';
import { useRouter } from '@/hooks/useRouter';
import type { ThreatModel, ThreatModelFilter } from '@/api';
import { THREAT_MODEL_FILTER_LABELS } from '@/api';
import { toast } from 'sonner';
import { usePermissions } from "@/hooks/usePermissions";
import { PermissionGuard } from "@/components/custom/PermissionGuard";
import { useFeatureFlags } from "@/hooks/useFeatureFlags";

export function ModelsPage() {
  const { navigateTo } = useRouter();
  const { hasPermission } = usePermissions();
  const { isFeatureEnabled } = useFeatureFlags();
  const [models, setModels] = useState<ThreatModel[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<ThreatModelFilter>("ALL");

  const fetchModels = async (searchTerm?: string, filterValue?: ThreatModelFilter) => {
    try {
      setIsLoading(true);
      const response = await threatModelApi.getAllThreatModels(
        searchTerm ?? search, 
        filterValue ?? filter
      );

      if (response.success && response.data) {
        setModels(response.data);
      } else {
        toast.error('Failed to load threat models');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchModels();
  }, []);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      fetchModels(search, filter);
    }, 300);
    return () => clearTimeout(timeoutId);
  }, [search, filter]);

  const handleSearchChange = (value: string) => {
    setSearch(value);
  };

  const handleAddModel = () => {
    navigateTo("add-model");
  };

  const handleModelClick = (id: string) => {
    navigateTo("model-detail", id);
  };

  const handleEditModel = (id: string) => {
    navigateTo("edit-model", id);
  };

  const handleDeleteModel = async (id: string, name: string) => {
    if (!window.confirm(`Are you sure you want to delete "${name}"?\n\nThis action cannot be undone.`)) {
      return;
    }

    try {
      const response = await threatModelApi.deleteThreatModel(id);

      if (response.success) {
        toast.success(`"${name}" has been successfully deleted`);
        // Refresh the models list
        fetchModels();
      } else {
        toast.error(`Failed to delete "${name}": ${response.message || 'Unknown error occurred'}`);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network error occurred';
      toast.error(`Unable to delete "${name}": ${errorMessage}`);
    }
  };

  const getEmptyStateMessage = () => {
    if (search && filter !== "ALL") {
      return `No threat models found matching "${search}" in ${THREAT_MODEL_FILTER_LABELS[filter].toLowerCase()}`;
    }
    if (search) {
      return `No threat models found matching "${search}"`;
    }
    if (filter !== "ALL") {
      return `No threat models found with ${THREAT_MODEL_FILTER_LABELS[filter].toLowerCase()}`;
    }
    return 'No threat models found';
  };

  return (
    <div className="container mx-auto py-6">
      {/* Page Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Threat Models</h1>
          <p className="text-muted-foreground mt-2">
            Manage and organize your security threat models
          </p>
        </div>
        <PermissionGuard permission="threatmodel:create">
          <Button onClick={handleAddModel} size="sm" className="h-8">
            <Plus className="w-4 h-4 mr-2" />
            Add Model
          </Button>
        </PermissionGuard>
      </div>

      {/* Filter Tabs */}
      {isFeatureEnabled('enable_threat_model_filtering') && (
        <div className="mb-6">
          <Tabs value={filter} onValueChange={(value) => setFilter(value as ThreatModelFilter)} className="w-full">
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="ALL" className="flex items-center gap-2">
                <Shield className="w-4 h-4" />
                {THREAT_MODEL_FILTER_LABELS.ALL}
              </TabsTrigger>
              <TabsTrigger value="ACTIVE_THREATS" className="flex items-center gap-2">
                <Activity className="w-4 h-4" />
                {THREAT_MODEL_FILTER_LABELS.ACTIVE_THREATS}
              </TabsTrigger>
              <TabsTrigger value="HIGH_RISK_THREATS" className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4" />
                {THREAT_MODEL_FILTER_LABELS.HIGH_RISK_THREATS}
              </TabsTrigger>
              <TabsTrigger value="MITIGATED_THREATS" className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                {THREAT_MODEL_FILTER_LABELS.MITIGATED_THREATS}
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      )}

      {/* Search Bar */}
      {isFeatureEnabled('enable_threat_model_search') && (
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
            <Input
              placeholder="Search threat models..."
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="pl-9 pr-9"
            />
            {search && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => handleSearchChange('')}
                className="absolute right-1 top-1/2 transform -translate-y-1/2 h-7 w-7 p-0"
              >
                <X className="w-4 h-4" />
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Models Table */}
      <Table>
        <TableBody>
          {isLoading ? (
            <TableRow>
              <TableCell colSpan={3} className="text-center">
                <RefreshCw className="w-4 h-4 animate-spin inline-block mr-2" />
                Loading threat models...
              </TableCell>
            </TableRow>
          ) : models.length === 0 ? (
            <TableRow>
              <TableCell colSpan={3} className="text-center text-muted-foreground py-8">
                <Shield className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>{getEmptyStateMessage()}</p>
                {(search || filter !== "ALL") && (
                  <div className="mt-4 space-x-2">
                    {search && (
                      <Button variant="outline" onClick={() => handleSearchChange('')}>
                        Clear search
                      </Button>
                    )}
                    {filter !== "ALL" && (
                      <Button variant="outline" onClick={() => setFilter("ALL")}>
                        Show all models
                      </Button>
                    )}
                  </div>
                )}
              </TableCell>
            </TableRow>
          ) : (
            models.map((model) => (
              <TableRow key={model.id} className="hover:bg-muted/50">
                <TableCell className="font-medium">
                  <Button
                    variant="link"
                    className="p-0 h-auto font-medium text-primary"
                    onClick={() => handleModelClick(model.id)}
                  >
                    {model.name}
                  </Button>
                </TableCell>
                <TableCell className="text-right text-muted-foreground">
                  {model.createdAt ? new Date(model.createdAt).toLocaleDateString() : 'N/A'}
                </TableCell>
                <TableCell className="w-[40px]">
                  {(hasPermission('threatmodel:update') || hasPermission('threatmodel:delete')) && (
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                          <span className="sr-only">Open menu</span>
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <PermissionGuard permission="threatmodel:update">
                          <DropdownMenuItem onClick={() => handleEditModel(model.id)}>
                            <Edit className="mr-2 h-4 w-4" />
                            Edit
                          </DropdownMenuItem>
                        </PermissionGuard>
                        <PermissionGuard permission="threatmodel:delete">
                          <DropdownMenuItem
                            onClick={() => handleDeleteModel(model.id, model.name)}
                            className="text-red-600"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Delete
                          </DropdownMenuItem>
                        </PermissionGuard>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}
