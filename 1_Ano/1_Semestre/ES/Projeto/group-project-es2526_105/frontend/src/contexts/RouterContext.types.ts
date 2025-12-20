import { createContext } from "react";

export type Tab = "description" | "overview" | "components" | "vulnerabilities";

export type Page =
  | "login"
  | "home"
  | "models"
  | "threats"
  | "threat-detail"
  | "add-model"
  | "edit-model"
  | "model-detail"
  | "add-component"
  | "edit-component"
  | "component-detail"
  | "chat";

export interface RouterContextType {
  currentPage: Page;
  navigateTo: (page: Page, modelId?: string, componentId?: string, threatId?: string, tab?: Tab) => void;
  modelId?: string;
  componentId?: string;
  threatId?: string;
  activeTab?: Tab;
}

export const RouterContext = createContext<RouterContextType | undefined>(undefined);