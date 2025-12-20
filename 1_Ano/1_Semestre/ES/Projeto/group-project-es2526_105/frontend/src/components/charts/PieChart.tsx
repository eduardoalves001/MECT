import type { ThreatsByCategory } from "@/api/types";

interface PieChartProps {
  data: ThreatsByCategory[];
  title: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  SPOOFING: "#ef4444",
  TAMPERING: "#f97316", 
  REPUDIATION: "#eab308",
  INFORMATION_DISCLOSURE: "#22c55e",
  DENIAL_OF_SERVICE: "#3b82f6",
  ELEVATION_OF_PRIVILEGE: "#8b5cf6"
};

const CATEGORY_LABELS: Record<string, string> = {
  SPOOFING: "Spoofing",
  TAMPERING: "Tampering",
  REPUDIATION: "Repudiation", 
  INFORMATION_DISCLOSURE: "Information Disclosure",
  DENIAL_OF_SERVICE: "Denial of Service",
  ELEVATION_OF_PRIVILEGE: "Elevation of Privilege"
};

function createPieSlice(percentage: number, offset: number, color: string) {
  const radius = 90;
  const circumference = 2 * Math.PI * radius;
  const strokeDasharray = `${(percentage / 100) * circumference} ${circumference}`;
  const strokeDashoffset = -offset * circumference / 100;
  
  return {
    strokeDasharray,
    strokeDashoffset,
    stroke: color
  };
}

export function PieChart({ data, title }: PieChartProps) {
  const total = data.reduce((sum, item) => sum + item.count, 0);
  
  if (total === 0) {
    return (
      <div className="rounded-lg border p-6">
        <h3 className="text-lg font-semibold mb-4">{title}</h3>
        <div className="flex items-center justify-center h-64 text-gray-500">
          No data available
        </div>
      </div>
    );
  }

  let offset = 0;
  const slices = data.map((item) => {
    const percentage = (item.count / total) * 100;
    const slice = {
      ...item,
      percentage,
      offset,
      color: CATEGORY_COLORS[item.category] || "#6b7280"
    };
    offset += percentage;
    return slice;
  });

  return (
    <div className="rounded-lg border p-6 relative">
      <h3 className="text-lg font-semibold mb-4">{title}</h3>
      <div className="flex items-center gap-6">
        {/* Pie Chart */}
        <div className="relative">
          <svg width="200" height="200" className="transform -rotate-90">
            <circle
              cx="100"
              cy="100"
              r="90"
              fill="none"
              stroke="#f3f4f6"
              strokeWidth="20"
            />
            {slices.map((slice, index) => (
              <circle
                key={index}
                cx="100"
                cy="100"
                r="90"
                fill="none"
                strokeWidth="20"
                {...createPieSlice(slice.percentage, slice.offset, slice.color)}
                className="transition-all duration-300"
              />
            ))}
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <div className="text-2xl font-bold">{total}</div>
              <div className="text-sm text-gray-600">Total</div>
            </div>
          </div>
        </div>

        {/* Legend */}
        <div className="flex-1 space-y-0.5">
          {slices.map((slice, index) => (
            <div key={index} className="grid grid-cols-[12px_1fr_auto] gap-2 items-center">
              <div
                className="w-3 h-3 rounded"
                style={{ backgroundColor: slice.color }}
              />
              <span className="text-[10px] leading-tight">
                {CATEGORY_LABELS[slice.category] || slice.category}
              </span>
              <span className="text-[10px] font-medium text-gray-600 text-right whitespace-nowrap">
                {slice.count} ({slice.percentage.toFixed(1)}%)
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}