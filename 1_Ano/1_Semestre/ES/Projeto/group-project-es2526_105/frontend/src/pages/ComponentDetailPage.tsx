import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { useRouter } from "@/hooks/useRouter";
import { ArrowLeft, Edit, Trash2, FileText, MoreVertical } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { componentApi } from "@/api";
import type { Component } from "@/api";
import { toast } from "sonner";
import { usePermissions } from "@/hooks/usePermissions";
import { PermissionGuard } from "@/components/custom/PermissionGuard";
import { CommentThread } from "@/components/custom/CommentThread";

interface ComponentDetailPageProps {
  modelId?: string;
  componentId?: string;
}

export function ComponentDetailPage({ modelId, componentId }: ComponentDetailPageProps) {
  const { navigateTo, activeTab } = useRouter();
  const { hasPermission } = usePermissions();
  const [component, setComponent] = useState<Component | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchComponent = async () => {
      if (!modelId || !componentId) {
        setError('No model or component ID provided');
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setError(null);
        const response = await componentApi.getComponentById(modelId, componentId);

        if (response.success && response.data) {
          setComponent(response.data);
        } else {
          const errorMsg = response.message || 'Component not found';
          setError(errorMsg);
          toast.error('Component not found');
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
        setError(errorMessage);
        toast.error('Failed to load component');
      } finally {
        setIsLoading(false);
      }
    };

    fetchComponent();
  }, [modelId, componentId]);

  const handleEdit = () => {
    if (modelId && component) {
      navigateTo("edit-component", modelId, component.id, undefined, activeTab || "components");
    }
  };

  const handleDelete = async () => {
    if (!modelId || !component) return;

    if (!window.confirm(`Are you sure you want to delete "${component.name}"?\n\nThis action cannot be undone.`)) {
      return;
    }

    try {
      const response = await componentApi.deleteComponent(modelId, component.id);

      if (response.success) {
        toast.success('Component deleted');
        navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
      } else {
        toast.error('Failed to delete');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to delete';
      toast.error(errorMessage);
    }
  };

  const handleBack = () => {
    if (modelId) {
      navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
    } else {
      navigateTo("models");
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-muted-foreground">Loading component details...</p>
        </div>
      </div>
    );
  }

  if (error || !component) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-lg font-medium mb-2">
            {error ? 'Error Loading Component' : 'Component not found'}
          </p>
          <p className="text-muted-foreground mb-4">
            {error || 'The requested component could not be found.'}
          </p>
          <Button onClick={handleBack}>
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Components
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-4xl">
      {/* Header */}
      <div className="flex items-center gap-4 mb-8">
        <Button
          variant="ghost"
          size="sm"
          onClick={handleBack}
          className="p-2"
        >
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">{component.name}</h1>
        </div>
        {(hasPermission('component:update') || hasPermission('component:delete')) && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <span className="sr-only">Open menu</span>
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <PermissionGuard permission="component:update">
                <DropdownMenuItem onClick={handleEdit}>
                  <Edit className="mr-2 h-4 w-4" />
                  Edit
                </DropdownMenuItem>
              </PermissionGuard>
              <PermissionGuard permission="component:delete">
                <DropdownMenuItem onClick={handleDelete} className="text-red-600">
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </DropdownMenuItem>
              </PermissionGuard>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>

      {/* Component Details */}
      <div className="grid gap-6">
        {/* Basic Information */}
        <div className="rounded-lg border p-6">
          <div className="flex items-center gap-2 mb-4">
            <FileText className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Description</h2>
          </div>
          <p className="text-muted-foreground leading-relaxed">
            {component.description}
          </p>
        </div>

        {/* Comments Section */}
        <div className="rounded-lg border p-6">
          <CommentThread componentId={component.id} />
        </div>
      </div>
    </div>
  );
}