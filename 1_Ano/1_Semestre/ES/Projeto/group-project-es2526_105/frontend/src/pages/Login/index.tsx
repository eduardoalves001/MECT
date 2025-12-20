import { useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useRouter } from "@/hooks/useRouter";
import { Button } from "@/components/ui/button";
import { ShieldLogo } from "@/components/ui/shield-logo";

export default function LoginPage() {
  const { isAuthenticated, isLoading, login } = useAuth();
  const { navigateTo } = useRouter();

  useEffect(() => {
    if (isAuthenticated && !isLoading) {
      navigateTo("home");
    }
  }, [isAuthenticated, isLoading, navigateTo]);

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

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="max-w-md w-full bg-white rounded-lg shadow-xl p-8">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <ShieldLogo className="h-16 w-16" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Welcome to RTMP</h1>
          <p className="text-gray-600 mt-2">Risk & Threat Modelling Platform</p>
        </div>

        <div className="space-y-4">
          <Button
            onClick={login}
            className="w-full"
            size="lg"
          >
            Sign In
          </Button>

          <div className="text-center text-sm text-gray-600">
            <p>Don't have an account? Contact admin for registration</p>
          </div>
        </div>

        <div className="mt-8 text-center text-sm text-gray-500">
          <p>Secure authentication powered by Keycloak</p>
        </div>
      </div>
    </div>
  );
}
