import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableRow } from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Edit, Plus, MoreHorizontal, RefreshCw, Trash2, Grid2X2, Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import type { Component } from "@/api/types";
import { usePermissions } from "@/hooks/usePermissions";
import { PermissionGuard } from "@/components/custom/PermissionGuard";
import { useFeatureFlags } from "@/hooks/useFeatureFlags";

interface ComponentsTabProps {
  components: Component[];
  isLoading: boolean;
  searchTerm: string;
  onSearchChange: (search: string) => void;
  onAddComponent: () => void;
  onEditComponent: (id: string) => void;
  onViewComponent: (id: string) => void;
  onDeleteComponent: (id: string, name: string) => void;
}

export function ComponentsTab({
  components,
  isLoading,
  searchTerm,
  onSearchChange,
  onAddComponent,
  onEditComponent,
  onViewComponent,
  onDeleteComponent,
}: ComponentsTabProps) {
  const { hasPermission } = usePermissions();
  const { isFeatureEnabled } = useFeatureFlags();

  return (
    <div className="grid gap-6">
      <div className="rounded-lg border p-6">
        <div className="flex justify-between items-center mb-4">
          <div className="flex items-center gap-2">
            <Grid2X2 className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Components</h2>
          </div>
          <PermissionGuard permission="component:create">
            <Button onClick={onAddComponent} size="sm" className="h-8">
              <Plus className="w-4 h-4 mr-2" />
              Add Component
            </Button>
          </PermissionGuard>
        </div>

        <div>
          <div className="mb-6">
            <p className="text-muted-foreground mb-4">
              Manage components within this threat model
            </p>
            {isFeatureEnabled('enable_component_search') && (
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
                <Input
                  placeholder="Search components..."
                  value={searchTerm}
                  onChange={(e) => onSearchChange(e.target.value)}
                  className="pl-9 pr-9"
                />
                {searchTerm && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="absolute right-1 top-1/2 transform -translate-y-1/2 h-7 w-7 p-0"
                    onClick={() => onSearchChange('')}
                  >
                    <X className="w-4 h-4" />
                  </Button>
                )}
              </div>
            )}
          </div>

          <Table>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center">
                    <RefreshCw className="w-4 h-4 animate-spin inline-block mr-2" />
                    Loading components...
                  </TableCell>
                </TableRow>
              ) : components.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground py-8">
                    <Grid2X2 className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>No components found</p>
                  </TableCell>
                </TableRow>
              ) : (
                components.map((component) => (
                  <TableRow key={component.id} className="hover:bg-muted/50">
                    <TableCell className="font-medium">
                      <Button
                        variant="link"
                        className="p-0 h-auto font-medium text-primary"
                        onClick={() => onViewComponent(component.id)}
                      >
                        {component.name}
                      </Button>
                    </TableCell>
                    <TableCell className="w-[40px]">
                      {(hasPermission('component:update') || hasPermission('component:delete')) && (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <span className="sr-only">Open menu</span>
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <PermissionGuard permission="component:update">
                              <DropdownMenuItem onClick={() => onEditComponent(component.id)}>
                                <Edit className="mr-2 h-4 w-4" />
                                Edit
                              </DropdownMenuItem>
                            </PermissionGuard>
                            <PermissionGuard permission="component:delete">
                              <DropdownMenuItem
                                onClick={() => onDeleteComponent(component.id, component.name)}
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
      </div>
    </div>
  );
}