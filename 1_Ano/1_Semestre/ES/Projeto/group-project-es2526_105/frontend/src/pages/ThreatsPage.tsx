import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableRow } from '@/components/ui/table';
import { RefreshCw, ShieldAlert } from 'lucide-react';
import { threatApi } from '@/api';
import { useRouter } from '@/hooks/useRouter';
import type { Threat } from '@/api';
import { toast } from 'sonner';
import { formatStrideCategory, getStrideCategoryColor } from '@/utils/strideCategory';

export function ThreatsPage() {
  const { navigateTo } = useRouter();
  const [threats, setThreats] = useState<Threat[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchThreats = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await threatApi.getAllThreats();
      if (response.success && response.data) {
        setThreats(response.data);
      } else {
        setError(response.message || 'Failed to fetch threats');
        toast.error('Failed to load threats');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Network connection failed';
      setError(errorMessage);
      toast.error('Connection failed');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchThreats();
  }, []);

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-muted-foreground">Loading threats...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center py-12">
          <p className="text-red-600 mb-4">{error}</p>
          <Button onClick={fetchThreats} variant="outline">
            <RefreshCw className="w-4 h-4 mr-2" />
            Retry
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Threats</h1>
          <p className="text-muted-foreground mt-2">Known threats</p>
        </div>
      </div>

      <Table>
        <TableBody>
          {threats.map((t) => (
            <TableRow key={t.id} className="hover:bg-muted/50">
              <TableCell className="font-medium">
                <Button
                  variant="link"
                  className="p-0 h-auto font-medium text-primary"
                  onClick={() => navigateTo('threat-detail', undefined, undefined, t.id)}
                >
                  {t.name}
                </Button>
              </TableCell>
              <TableCell className="text-right">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStrideCategoryColor(t.category)}`}>
                  {formatStrideCategory(t.category)}
                </span>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {threats.length === 0 && (
        <div className="text-center py-12">
          <ShieldAlert className="w-12 h-12 mx-auto mb-3 text-muted-foreground opacity-50" />
          <p className="text-muted-foreground text-lg">No threats found</p>
        </div>
      )}
    </div>
  );
}
