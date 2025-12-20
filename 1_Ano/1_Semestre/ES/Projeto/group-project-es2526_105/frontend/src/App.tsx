import { Layout } from "@/components/layout/Layout";
import {
  HomePage,
  ModelsPage,
  ModelFormPage,
  ModelDetailPage,
  ComponentFormPage,
  ComponentDetailPage,
  ChatPage,
} from "@/pages";
import { ThreatsPage } from "@/pages/ThreatsPage";
import { ThreatDetailPage } from "@/pages/ThreatDetailPage";
import LoginPage from "@/pages/Login";
import { RouterProvider } from "@/contexts/RouterContext";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { useRouter } from "@/hooks/useRouter";
import { Toaster } from "@/components/ui/sonner";
import { useEffect } from "react";

function AppContent() {
  const { currentPage, modelId, componentId, threatId, navigateTo } = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated && currentPage !== "login") {
      navigateTo("login");
    }
  }, [isAuthenticated, isLoading, currentPage, navigateTo]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  const renderPage = () => {
    // Show login page if not authenticated
    if (!isAuthenticated) {
      return <LoginPage />;
    }

    switch (currentPage) {
      case "login":
        return <LoginPage />;
      case "home":
        return <HomePage />;
      case "models":
        return <ModelsPage />;
      case "add-model":
        return <ModelFormPage />;
      case "edit-model":
        return <ModelFormPage modelId={modelId} isEdit />;
      case "model-detail":
        return <ModelDetailPage modelId={modelId} />;
      case "threats":
        return <ThreatsPage />;
      case "threat-detail":
        return <ThreatDetailPage threatId={threatId} />;
      case "add-component":
        return <ComponentFormPage modelId={modelId} />;
      case "edit-component":
        return <ComponentFormPage modelId={modelId} componentId={componentId} isEdit />;
      case "component-detail":
        return <ComponentDetailPage modelId={modelId} componentId={componentId} />;
      case "chat":
        return <ChatPage />;
      default:
        return <HomePage />;
    }
  };

  // Don't wrap login page in Layout
  if (!isAuthenticated || currentPage === "login") {
    return renderPage();
  }

  return (
    <Layout>
      {renderPage()}
    </Layout>
  );
}

function App() {
  return (
    <AuthProvider>
      <RouterProvider>
        <AppContent />
        <Toaster position="top-right" />
      </RouterProvider>
    </AuthProvider>
  );
}

export default App;
