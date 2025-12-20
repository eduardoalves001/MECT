import { useState, type ReactNode } from "react";
import { RouterContext, type Page, type Tab } from "./RouterContext.types";

export function RouterProvider({ children }: { children: ReactNode }) {
  const [currentPage, setCurrentPage] = useState<Page>("home");
  const [modelId, setModelId] = useState<string | undefined>();
  const [componentId, setComponentId] = useState<string | undefined>();
  const [threatId, setThreatId] = useState<string | undefined>();
  const [activeTab, setActiveTab] = useState<Tab | undefined>();

  const navigateTo = (page: Page, modelId?: string, componentId?: string, threatIdArg?: string, tab?: Tab) => {
    setCurrentPage(page);
    setModelId(modelId);
    setComponentId(componentId);
    setThreatId(threatIdArg);
    setActiveTab(tab);
  };

  return (
    <RouterContext.Provider value={{ currentPage, navigateTo, modelId, componentId, threatId, activeTab }}>
      {children}
    </RouterContext.Provider>
  );
}