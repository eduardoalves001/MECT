import type { RiskDistribution } from "@/api/types";

interface BarChartProps {
  data: RiskDistribution[];
  title: string;
}

const RISK_COLORS: Record<string, string> = {
  Critical: "var(--color-red-600)",
  High: "var(--color-orange-600)", 
  Medium: "var(--color-amber-600)",
  Low: "var(--color-green-600)",
  Minimal: "var(--color-gray-400)"
};

const RISK_ORDER = ["Critical", "High", "Medium", "Low", "Minimal"];

export function BarChart({ data, title }: BarChartProps) {
  // Sort data by risk order and ensure all levels are present
  const sortedData = RISK_ORDER.map(level => {
    const item = data.find(d => d.riskLevel === level);
    return {
      riskLevel: level,
      count: item?.count || 0
    };
  });

  const maxCount = Math.max(...sortedData.map(item => item.count), 1);
  const chartHeight = 200;
  const chartWidth = 300;
  const barWidth = 50;
  const spacing = 60;
  const paddingLeft = 40;
  const paddingBottom = 40;
  const paddingTop = 20;
  
  // Calculate Y-axis ticks
  const yTicks = [];
  const tickCount = 5;
  for (let i = 0; i <= tickCount; i++) {
    yTicks.push(Math.ceil((maxCount * i) / tickCount));
  }
  
  return (
    <div className="rounded-lg border p-6">
      <h3 className="text-lg font-semibold mb-4">{title}</h3>
      
      {sortedData.every(item => item.count === 0) ? (
        <div className="flex items-center justify-center h-64 text-gray-500">
          No data available
        </div>
      ) : (
        <div className="flex justify-center">
          <svg 
            width={chartWidth + paddingLeft + 20} 
            height={chartHeight + paddingBottom + paddingTop}
            className="overflow-visible"
          >
            {/* Y-axis */}
            <line
              x1={paddingLeft}
              y1={paddingTop}
              x2={paddingLeft}
              y2={chartHeight + paddingTop}
              stroke="#6b7280"
              strokeWidth="2"
            />
            
            {/* X-axis */}
            <line
              x1={paddingLeft}
              y1={chartHeight + paddingTop}
              x2={chartWidth + paddingLeft}
              y2={chartHeight + paddingTop}
              stroke="#6b7280"
              strokeWidth="2"
            />
            
            {/* Y-axis ticks and labels */}
            {yTicks.map((tick, index) => {
              const y = chartHeight + paddingTop - (tick / maxCount) * chartHeight;
              return (
                <g key={index}>
                  <line
                    x1={paddingLeft - 5}
                    y1={y}
                    x2={paddingLeft}
                    y2={y}
                    stroke="#6b7280"
                    strokeWidth="1"
                  />
                  <text
                    x={paddingLeft - 10}
                    y={y + 4}
                    textAnchor="end"
                    className="text-xs fill-gray-600"
                  >
                    {tick}
                  </text>
                </g>
              );
            })}
            
            {/* Bars */}
            {sortedData.map((item, index) => {
              const barHeight = (item.count / maxCount) * chartHeight;
              const x = paddingLeft + index * spacing + (spacing - barWidth) / 2;
              const y = chartHeight + paddingTop - barHeight;
              const color = RISK_COLORS[item.riskLevel];
              
              return (
                <g key={index}>
                  {/* Bar */}
                  <rect
                    x={x}
                    y={y}
                    width={barWidth}
                    height={barHeight}
                    fill={color}
                    className="transition-all duration-300 hover:opacity-80"
                  />
                  
                  {/* Value label on top of bar */}
                  {item.count > 0 && (
                    <text
                      x={x + barWidth / 2}
                      y={y - 5}
                      textAnchor="middle"
                      className="text-xs font-medium fill-gray-700"
                    >
                      {item.count}
                    </text>
                  )}
                  
                  {/* X-axis label */}
                  <text
                    x={x + barWidth / 2}
                    y={chartHeight + paddingTop + 20}
                    textAnchor="middle"
                    className="text-xs fill-gray-600"
                  >
                    {item.riskLevel}
                  </text>
                </g>
              );
            })}
            
            {/* Y-axis label */}
            <text
              x={15}
              y={chartHeight / 2 + paddingTop}
              textAnchor="middle"
              className="text-xs fill-gray-600"
              transform={`rotate(-90 15 ${chartHeight / 2 + paddingTop})`}
            >
              Number of Threats
            </text>
            
            {/* X-axis label */}
            <text
              x={chartWidth / 2 + paddingLeft}
              y={chartHeight + paddingBottom + paddingTop + 4}
              textAnchor="middle"
              className="text-xs fill-gray-600"
            >
              Risk Level
            </text>
          </svg>
        </div>
      )}
    </div>
  );
}