import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
import { useRouter } from '@/hooks/useRouter';
import { threatApi } from '@/api';
import type { Threat } from '@/api';
import { toast } from 'sonner';
import { formatStrideCategory, getStrideCategoryColor } from '@/utils/strideCategory';

interface ThreatDetailPageProps {
  threatId?: string;
}

export function ThreatDetailPage({ threatId }: ThreatDetailPageProps) {
  const { navigateTo, modelId, activeTab } = useRouter();
  const [threat, setThreat] = useState<Threat | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchThreat = async () => {
      if (!threatId) {
        setError('No threat ID provided');
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        const response = await threatApi.getThreatById(threatId);
        if (response.success && response.data) {
          setThreat(response.data);
        } else {
          setError(response.message || 'Threat not found');
          toast.error('Threat not found');
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Network error';
        setError(errorMessage);
        toast.error('Failed to load threat');
      } finally {
        setIsLoading(false);
      }
    };

    fetchThreat();
  }, [threatId]);

  const handleBack = () => {
    if (modelId && activeTab === 'vulnerabilities') {
      navigateTo('model-detail', modelId, undefined, undefined, 'vulnerabilities');
    } else {
      navigateTo('threats');
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">Loading threat...</div>
      </div>
    );
  }

  if (error || !threat) {
    return (
      <div className="container mx-auto py-6 text-center">
        <p className="text-lg font-medium mb-2">{error ? 'Error' : 'Not found'}</p>
        <p className="text-muted-foreground mb-4">{error || 'The requested threat could not be found.'}</p>
        <Button onClick={handleBack}>
          <ArrowLeft className="w-4 h-4 mr-2" /> Back to Threats
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-4xl">
      <div className="flex items-center gap-4 mb-8">
        <Button variant="ghost" size="sm" onClick={handleBack} className="p-2">
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">{threat.name}</h1>
          <div className="flex items-center gap-2 mt-2">
            <span className="text-muted-foreground">Category:</span>
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStrideCategoryColor(threat.category)}`}>
              {formatStrideCategory(threat.category)}
            </span>
          </div>
        </div>
      </div>

      <div className="rounded-lg border p-6">
        <h2 className="text-xl font-semibold mb-2">Description</h2>
        <p className="text-muted-foreground leading-relaxed">{threat.description}</p>
      </div>
    </div>
  );
}
