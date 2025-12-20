import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useRouter } from "@/hooks/useRouter";
import { ArrowLeft, Save } from "lucide-react";
import { threatModelApi } from "@/api";
import { toast } from "sonner";

interface ModelFormPageProps {
  modelId?: string;
  isEdit?: boolean;
}

export function ModelFormPage({ modelId, isEdit = false }: ModelFormPageProps) {
  const { navigateTo } = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEdit);

  // Load existing model data when editing
  useEffect(() => {
    if (isEdit && modelId) {
      const fetchModel = async () => {
        try {
          setInitialLoading(true);
          const response = await threatModelApi.getThreatModelById(modelId);

          if (response.success && response.data) {
            setName(response.data.name);
            setDescription(response.data.description || "");
          } else {
            toast.error('Model not found');
            navigateTo("models");
          }
        } catch (err) {
          const errorMessage = err instanceof Error ? err.message : 'Failed to load model';
          toast.error(errorMessage);
          navigateTo("models");
        } finally {
          setInitialLoading(false);
        }
      };

      fetchModel();
    }
  }, [isEdit, modelId, navigateTo]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    const modelName = name.trim();

    try {
      const modelData = {
        name: modelName,
        description: description.trim() || null,
      };

      let response;
      if (isEdit && modelId) {
        response = await threatModelApi.updateThreatModel(modelId, modelData);
      } else {
        response = await threatModelApi.createThreatModel(modelData);
      }

      if (response.success) {
        toast.success(isEdit ? 'Model updated' : 'Model created');
        navigateTo("models");
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
    navigateTo("models");
  };

  const isFormValid = name.trim().length > 0;

  if (initialLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-muted-foreground">Loading model...</p>
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
            {isEdit ? "Edit Model" : "Add New Model"}
          </h1>
          <p className="text-muted-foreground mt-2">
            {isEdit
              ? "Update your threat model details"
              : "Create a new threat model to analyze security risks"
            }
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="space-y-2">
          <label htmlFor="name" className="text-sm font-medium leading-none">
            Model Name *
          </label>
          <Input
            id="name"
            type="text"
            placeholder="Enter model name..."
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
            placeholder="Describe the purpose and scope of this threat model..."
            value={description}
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setDescription(e.target.value)}
            className="w-full min-h-[120px] resize-none"
            required
          />
          <p className="text-xs text-muted-foreground">
            Provide a detailed description of what this threat model covers
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
                ? "Update Model"
                : "Create Model"
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