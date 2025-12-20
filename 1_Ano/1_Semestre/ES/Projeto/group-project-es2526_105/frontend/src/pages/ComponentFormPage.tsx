import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useRouter } from "@/hooks/useRouter";
import { ArrowLeft, Save } from "lucide-react";
import { componentApi } from "@/api";
import { toast } from "sonner";

interface ComponentFormPageProps {
  modelId?: string;
  componentId?: string;
  isEdit?: boolean;
}

export function ComponentFormPage({ modelId, componentId, isEdit = false }: ComponentFormPageProps) {
  const { navigateTo, activeTab } = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEdit);

  // Load existing component data when editing
  useEffect(() => {
    if (isEdit && modelId && componentId) {
      const fetchComponent = async () => {
        try {
          setInitialLoading(true);
          const response = await componentApi.getComponentById(modelId, componentId);

          if (response.success && response.data) {
            setName(response.data.name);
            setDescription(response.data.description || "");
          } else {
            toast.error('Component not found');
            navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
          }
        } catch (err) {
          const errorMessage = err instanceof Error ? err.message : 'Failed to load component';
          toast.error(errorMessage);
          navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
        } finally {
          setInitialLoading(false);
        }
      };

      fetchComponent();
    }
  }, [isEdit, modelId, componentId, navigateTo, activeTab]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!modelId) {
      toast.error('No model ID provided');
      return;
    }

    setIsLoading(true);
    const componentName = name.trim();

    try {
      const componentData = {
        name: componentName,
        description: description.trim() || null,
      };

      let response;
      if (isEdit && componentId) {
        response = await componentApi.updateComponent(modelId, componentId, componentData);
      } else {
        response = await componentApi.createComponent(modelId, componentData);
      }

      if (response.success) {
        toast.success(isEdit ? 'Component updated' : 'Component created');
        navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
      } else {
        toast.error(isEdit ? 'Failed to update' : 'Failed to create');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : (isEdit ? 'Failed to update' : 'Failed to create');
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    if (modelId) {
      navigateTo("model-detail", modelId, undefined, undefined, activeTab || "components");
    } else {
      navigateTo("models");
    }
  };

  const isFormValid = name.trim().length > 0;

  if (initialLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-muted-foreground">Loading component...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-2xl">
      <div className="flex items-center gap-4 mb-8">
        <Button
          variant="ghost"
          size="sm"
          onClick={handleCancel}
          className="p-2"
        >
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {isEdit ? "Edit Component" : "Add New Component"}
          </h1>
          <p className="text-muted-foreground mt-2">
            {isEdit
              ? "Update your component details"
              : "Create a new component to analyze security risks"
            }
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="space-y-2">
          <label htmlFor="name" className="text-sm font-medium leading-none">
            Component Name *
          </label>
          <Input
            id="name"
            type="text"
            placeholder="Enter component name..."
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full"
            required
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="description" className="text-sm font-medium leading-none">
            Description *
          </label>
          <Textarea
            id="description"
            placeholder="Describe the purpose and functionality of this component..."
            value={description}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setDescription(e.target.value)}
            className="w-full min-h-[120px] resize-none"
            required
          />
          <p className="text-xs text-muted-foreground">
            Provide a detailed description of what this component represents
          </p>
        </div>

        <div className="flex gap-3 pt-4">
          <Button
            type="submit"
            disabled={!isFormValid || isLoading}
            className="flex items-center gap-2"
          >
            <Save className="w-4 h-4" />
            {isLoading
              ? "Saving..."
              : isEdit
                ? "Update Component"
                : "Create Component"
            }
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={handleCancel}
            disabled={isLoading}
          >
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}