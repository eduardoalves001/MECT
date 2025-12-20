import { FileText } from "lucide-react";
import type { ThreatModel } from "@/api/types";

interface DescriptionTabProps {
  model: ThreatModel;
}

export function DescriptionTab({ model }: DescriptionTabProps) {
  return (
    <div className="grid gap-6">
      <div className="rounded-lg border p-6">
        <div className="flex items-center gap-2 mb-4">
          <FileText className="w-5 h-5" />
          <h2 className="text-xl font-semibold">Description</h2>
        </div>
        <p className="text-muted-foreground leading-relaxed">
          {model.description}
        </p>
      </div>
    </div>
  );
}