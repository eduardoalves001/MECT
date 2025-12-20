import { BarChart3, Shield, AlertTriangle, CheckCircle, Grid2X2, TrendingUp } from "lucide-react";
import { useState, useEffect } from "react";
import { threatModelApi } from "@/api";
import type { ThreatModelStats, ThreatsByCategory, RiskDistribution } from "@/api/types";
import { toast } from "sonner";
import { PieChart } from "@/components/charts/PieChart";
import { BarChart } from "@/components/charts/BarChart";

interface OverviewTabProps {
  threatModelId: string;
  isLoading?: boolean;
}

interface SummaryCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  description: string;
  color?: "blue" | "red" | "yellow" | "green" | "gray";
}

function SummaryCard({ title, value, icon, description, color = "gray" }: SummaryCardProps) {
  const colorClasses = {
    blue: "border-blue-200 bg-blue-50",
    red: "border-red-200 bg-red-50",
    yellow: "border-yellow-200 bg-yellow-50",
    green: "border-green-200 bg-green-50",
    gray: "border-gray-200 bg-gray-50"
  };

  const iconColorClasses = {
    blue: "text-blue-600",
    red: "text-red-600",
    yellow: "text-yellow-600",
    green: "text-green-600",
    gray: "text-gray-600"
  };

  return (
    <div className={`rounded-lg border p-6 h-full flex flex-col ${colorClasses[color]}`}>
      <div className="flex items-center gap-3 mb-4 h-8">
        <div className={`${iconColorClasses[color]}`}>
          {icon}
        </div>
        <h3 className="font-medium text-gray-900 leading-tight">{title}</h3>
      </div>
      <div className="text-3xl font-bold text-gray-900 mb-2">{value}</div>
      <p className="text-sm text-gray-600 flex-1">{description}</p>
    </div>
  );
}

export function OverviewTab({ threatModelId, isLoading }: OverviewTabProps) {
  const [stats, setStats] = useState<ThreatModelStats | null>(null);
  const [threatsByCategory, setThreatsByCategory] = useState<ThreatsByCategory[]>([]);
  const [riskDistribution, setRiskDistribution] = useState<RiskDistribution[]>([]);
  const [isLoadingStats, setIsLoadingStats] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoadingStats(true);
        const response = await threatModelApi.getThreatModelStats(threatModelId);
        if (response.success && response.data) {
          setStats(response.data);
        } else {
          toast.error('Failed to load threat model statistics');
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Network connection failed';
        toast.error(errorMessage);
      } finally {
        setIsLoadingStats(false);
      }
    };

    const fetchChartsData = async () => {
      try {
        const [categoryResponse, distributionResponse] = await Promise.all([
          threatModelApi.getThreatsByCategory(threatModelId),
          threatModelApi.getRiskDistribution(threatModelId)
        ]);

        if (categoryResponse.success && categoryResponse.data) {
          setThreatsByCategory(categoryResponse.data);
        }
        if (distributionResponse.success && distributionResponse.data) {
          setRiskDistribution(distributionResponse.data);
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Network connection failed';
        toast.error(errorMessage);
      } finally {
      }
    };

    fetchStats();
    fetchChartsData();
  }, [threatModelId]);

  if (isLoading || isLoadingStats) {
    return (
      <div className="grid gap-6">
        <div className="rounded-lg border p-6">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Project Summary</h2>
          </div>
          <p className="text-muted-foreground">Loading overview...</p>
        </div>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="grid gap-6">
        <div className="rounded-lg border p-6">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="w-5 h-5" />
            <h2 className="text-xl font-semibold">Project Summary</h2>
          </div>
          <p className="text-muted-foreground">Failed to load statistics.</p>
        </div>
      </div>
    );
  }

  // Use stats from API instead of calculating from components
  const { totalComponents, activeThreats, highRiskThreats, mitigatedThreats } = stats;

  return (
    <div className="grid gap-6">
      {/* Risk Summary */}
      <div className="rounded-lg border p-6">
        <div className="flex items-center gap-2 mb-6">
          <BarChart3 className="w-5 h-5" />
          <h2 className="text-xl font-semibold">Project Summary</h2>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 auto-rows-fr">
          <SummaryCard
            title="Total Components"
            value={totalComponents}
            icon={<Grid2X2 className="w-6 h-6" />}
            description="System components modeled"
            color="blue"
          />
          
          <SummaryCard
            title="Active Threats"
            value={activeThreats}
            icon={<Shield className="w-6 h-6" />}
            description="Identified security threats"
            color="yellow"
          />
          
          <SummaryCard
            title="High Risk Threats"
            value={highRiskThreats}
            icon={<AlertTriangle className="w-6 h-6" />}
            description="Requiring immediate attention"
            color="red"
          />
          
          <SummaryCard
            title="Mitigated Threats"
            value={mitigatedThreats}
            icon={<CheckCircle className="w-6 h-6" />}
            description="Successfully addressed threats"
            color="green"
          />
        </div>
      </div>

      {/* Risk Dashboards */}
      <div className="rounded-lg border p-6">
        <div className="flex items-center gap-2 mb-6">
          <TrendingUp className="w-5 h-5" />
          <h2 className="text-xl font-semibold">Risk Dashboards</h2>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <PieChart
            data={threatsByCategory}
            title="Threats by Category"
          />
          
          <BarChart
            data={riskDistribution}
            title="Risk Distribution"
          />
        </div>
      </div>
    </div>
  );
}