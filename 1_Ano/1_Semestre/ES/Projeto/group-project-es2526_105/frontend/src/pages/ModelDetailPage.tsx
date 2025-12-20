import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";

import { useRouter } from "@/hooks/useRouter";
import { ArrowLeft } from "lucide-react";
import { threatModelApi, componentApi, vulnerabilityApi } from "@/api";
import type { ThreatModel, Component, Vulnerability } from "@/api";
import { toast } from "sonner";
import type { Tab } from "@/contexts/RouterContext.types";

import { Header } from "./ModelDetail/Header";
import { OverviewTab } from "./ModelDetail/Overview";
import { DescriptionTab } from "./ModelDetail/Description";
import { ComponentsTab } from "./ModelDetail/Components";
import { VulnerabilitiesTab } from "./ModelDetail/Vulnerabilities";

interface ModelDetailPageProps {
  modelId?: string;
}

export function ModelDetailPage({ modelId }: ModelDetailPageProps) {
  const { navigateTo, activeTab } = useRouter();
  const [model, setModel] = useState<ThreatModel | null>(null);
  const [components, setComponents] = useState<Component[]>([]);
  const [vulnerabilities, setVulnerabilities] = useState<Vulnerability[]>([]);
  const [currentTab, setCurrentTab] = useState<Tab>(activeTab || 'description');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isLoadingComponents, setIsLoadingComponents] = useState(false);
  const [isLoadingVulnerabilities, setIsLoadingVulnerabilities] = useState(false);
  const [componentSearchTerm, setComponentSearchTerm] = useState('');
  const [selectedRiskLevels, setSelectedRiskLevels] = useState<string[]>([]);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);

  useEffect(() => {
    if (activeTab) {
      setCurrentTab(activeTab);
    }
  }, [activeTab]);

  useEffect(() => {
    const fetchModel = async () => {
      if (!modelId) {
        setError('No model ID provided');
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setError(null);
        const response = await threatModelApi.getThreatModelById(modelId);

        if (response.success && response.data) {
          setModel(response.data);
        } else {
          const errorMsg = response.message || 'Threat model not found';
          setError(errorMsg);
          toast.error('Model not found');
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
        setError(errorMessage);
        toast.error('Failed to load model');
      } finally {
        setIsLoading(false);
      }
    };

    fetchModel();
  }, [modelId]);

  const fetchComponents = async (search?: string) => {
    if (!modelId) return;

    try {
      setIsLoadingComponents(true);
      setError(null);
      const response = await componentApi.getAllComponents(modelId, search);

      if (response.success && response.data) {
        setComponents(response.data);
      } else {
        toast.error('Failed to load components');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
      toast.error(errorMessage);
    } finally {
      setIsLoadingComponents(false);
    }
  };

  const fetchVulnerabilities = async (riskLevels?: string[], statuses?: string[]) => {
    if (!modelId) return;

    try {
      setIsLoadingVulnerabilities(true);
      setError(null);
      const response = await vulnerabilityApi.getAllVulnerabilitiesByThreatModel(
        modelId,
        riskLevels,
        statuses
      );

      if (response.success && response.data) {
        setVulnerabilities(response.data);
      } else {
        toast.error('Failed to load vulnerabilities');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
      toast.error(errorMessage);
    } finally {
      setIsLoadingVulnerabilities(false);
    }
  };

  useEffect(() => {
    if (currentTab === 'components') {
      fetchComponents();
    } else if (currentTab === 'vulnerabilities') {
      fetchVulnerabilities();
      fetchComponents();
    } else if (currentTab === 'overview') {
      fetchVulnerabilities();
    }
  }, [currentTab, modelId]);

  useEffect(() => {
    if (currentTab === 'components') {
      const timeoutId = setTimeout(() => {
        fetchComponents(componentSearchTerm);
      }, 300);
      return () => clearTimeout(timeoutId);
    }
  }, [componentSearchTerm, currentTab]);

  const handleComponentSearchChange = (search: string) => {
    setComponentSearchTerm(search);
  };

  const handleAddComponent = () => {
    if (model) {
      navigateTo("add-component", model.id, undefined, undefined, currentTab);
    }
  };

  const handleComponentClick = (id: string) => {
    if (model) {
      navigateTo("component-detail", model.id, id, undefined, currentTab);
    }
  };

  const handleEditComponent = (id: string) => {
    if (model) {
      navigateTo("edit-component", model.id, id, undefined, currentTab);
    }
  };

  const handleDeleteComponent = async (id: string, name: string) => {
    if (!model) return;

    if (!window.confirm(`Are you sure you want to delete "${name}"?\n\nThis action cannot be undone.`)) {
      return;
    }

    try {
      const response = await componentApi.deleteComponent(model.id, id);

      if (response.success) {
        toast.success(`"${name}" has been successfully deleted`);
        fetchComponents();
      } else {
        toast.error(`Failed to delete "${name}": ${response.message || 'Unknown error occurred'}`);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network error occurred';
      toast.error(`Unable to delete "${name}": ${errorMessage}`);
    }
  };

  const handleEdit = () => {
    if (model) {
      navigateTo("edit-model", model.id);
    }
  };

  const handleDelete = async () => {
    if (!model) return;

    if (!window.confirm(`Are you sure you want to delete "${model.name}"?\n\nThis action cannot be undone.`)) {
      return;
    }

    try {
      const response = await threatModelApi.deleteThreatModel(model.id);

      if (response.success) {
        toast.success('Model deleted');
        navigateTo("models");
      } else {
        toast.error('Failed to delete');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to delete';
      toast.error(errorMessage);
    }
  };

  const handleBack = () => {
    navigateTo("models");
  };

  const handleExportPdf = async () => {
    if (!model) return;

    try {
      const filename = `${model.name.replace(/\s+/g, '-').toLowerCase()}-${new Date().toISOString().split('T')[0]}.pdf`;
      await threatModelApi.exportToPdf(model.id, filename);
      toast.success('PDF exported successfully');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to export PDF';
      toast.error(errorMessage);
    }
  };

  const handleExportCsv = async () => {
    if (!model) return;

    try {
      const filename = `${model.name.replace(/\s+/g, '-').toLowerCase()}-${new Date().toISOString().split('T')[0]}.csv`;
      await threatModelApi.exportToCsv(model.id, filename);
      toast.success('CSV exported successfully');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to export CSV';
      toast.error(errorMessage);
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-muted-foreground">Loading model details...</p>
        </div>
      </div>
    );
  }

  if (error || !model) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-lg font-medium mb-2">
            {error ? 'Error Loading Model' : 'Model not found'}
          </p>
          <p className="text-muted-foreground mb-4">
            {error || 'The requested threat model could not be found.'}
          </p>
          <Button onClick={handleBack}>
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Models
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-4xl">
      <Header
        model={model}
        onBack={handleBack}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onExportPdf={handleExportPdf}
        onExportCsv={handleExportCsv}
      />

      <Tabs value={currentTab} className="mb-6" onValueChange={(value) => setCurrentTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="description">Description</TabsTrigger>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="components">Components</TabsTrigger>
          <TabsTrigger value="vulnerabilities">Vulnerabilities</TabsTrigger>
        </TabsList>

        <TabsContent value="description">
          <DescriptionTab model={model} />
        </TabsContent>

        <TabsContent value="overview">
          <OverviewTab
            threatModelId={model.id}
            isLoading={isLoadingVulnerabilities}
          />
        </TabsContent>

        <TabsContent value="components">
          <ComponentsTab
            components={components}
            isLoading={isLoadingComponents}
            searchTerm={componentSearchTerm}
            onSearchChange={handleComponentSearchChange}
            onAddComponent={handleAddComponent}
            onEditComponent={handleEditComponent}
            onViewComponent={handleComponentClick}
            onDeleteComponent={handleDeleteComponent}
          />
        </TabsContent>

        <TabsContent value="vulnerabilities">
          <VulnerabilitiesTab
            threatModelId={model.id}
            vulnerabilities={vulnerabilities}
            components={components}
            isLoading={isLoadingVulnerabilities}
            onRefresh={fetchVulnerabilities}
            onComponentClick={handleComponentClick}
            selectedRiskLevels={selectedRiskLevels}
            setSelectedRiskLevels={setSelectedRiskLevels}
            selectedStatuses={selectedStatuses}
            setSelectedStatuses={setSelectedStatuses}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}