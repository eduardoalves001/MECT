import { Button } from "@/components/ui/button";
import { ArrowLeft, Edit, Trash2, MoreVertical, FileDown } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import type { ThreatModel } from "@/api/types";
import { usePermissions } from "@/hooks/usePermissions";
import { PermissionGuard } from "@/components/custom/PermissionGuard";
import { useFeatureFlags } from "@/hooks/useFeatureFlags";

interface HeaderProps {
  model: ThreatModel;
  onBack: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onExportPdf: () => void;
  onExportCsv: () => void;
}

export function Header({ model, onBack, onEdit, onDelete, onExportPdf, onExportCsv }: HeaderProps) {
  const { hasPermission } = usePermissions();
  const { isFeatureEnabled } = useFeatureFlags();

  // Check if there are any visible menu items
  const hasEditPermission = hasPermission('threatmodel:update');
  const hasDeletePermission = hasPermission('threatmodel:delete');
  const hasExportPermission = hasPermission('threatmodel:read') && isFeatureEnabled('enable_export_features');
  const showDropdown = hasEditPermission || hasDeletePermission || hasExportPermission;

  return (
    <div className="flex items-center gap-4 mb-8">
      <Button
        variant="ghost"
        size="sm"
        onClick={onBack}
        className="p-2"
      >
        <ArrowLeft className="w-4 h-4" />
      </Button>
      <div className="flex-1">
        <div className="flex items-center gap-3 mb-2">
          <h1 className="text-3xl font-bold tracking-tight">{model.name}</h1>
        </div>
        <p className="text-muted-foreground">
          Created on {model.createdAt ? new Date(model.createdAt).toLocaleDateString() : 'N/A'}
        </p>
      </div>
      {showDropdown && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
              <span className="sr-only">Open menu</span>
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <PermissionGuard permission="threatmodel:update">
              <DropdownMenuItem onClick={onEdit}>
                <Edit className="mr-2 h-4 w-4" />
                Edit
              </DropdownMenuItem>
            </PermissionGuard>
            {isFeatureEnabled('enable_export_features') && (
              <>
                <PermissionGuard permission="threatmodel:read">
                  <DropdownMenuItem onClick={onExportPdf}>
                    <FileDown className="mr-2 h-4 w-4" />
                    Export as PDF
                  </DropdownMenuItem>
                </PermissionGuard>
                <PermissionGuard permission="threatmodel:read">
                  <DropdownMenuItem onClick={onExportCsv}>
                    <FileDown className="mr-2 h-4 w-4" />
                    Export as CSV
                  </DropdownMenuItem>
                </PermissionGuard>
              </>
            )}
            <PermissionGuard permission="threatmodel:delete">
              <DropdownMenuItem onClick={onDelete} className="text-red-600">
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </PermissionGuard>
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </div>
  )
}