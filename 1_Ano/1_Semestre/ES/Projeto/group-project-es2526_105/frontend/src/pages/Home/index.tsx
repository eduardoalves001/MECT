import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { ShieldLogo } from "@/components/ui/shield-logo";

export default function HomePage() {
  const { user, isAuthenticated } = useAuth();

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-12">
          <div className="flex justify-center mb-6">
            <ShieldLogo className="h-24 w-24" />
          </div>
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            Risk & Threat Modelling Platform
          </h1>
          {isAuthenticated && user && (
            <p className="text-xl text-gray-600">
              Welcome back, {user.firstName || user.username || 'User'}!
            </p>
          )}
        </div>

        <div className="grid md:grid-cols-3 gap-6 mb-12">
          <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
            <div className="text-3xl mb-4">🛡️</div>
            <h3 className="text-xl font-semibold mb-2">Threat Models</h3>
            <p className="text-gray-600">
              Create and manage comprehensive threat models for your systems
            </p>
          </div>

          <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
            <div className="text-3xl mb-4">⚠️</div>
            <h3 className="text-xl font-semibold mb-2">Threat Catalog</h3>
            <p className="text-gray-600">
              Access a comprehensive catalog of security threats
            </p>
          </div>

          <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
            <div className="text-3xl mb-4">🔍</div>
            <h3 className="text-xl font-semibold mb-2">Vulnerability Analysis</h3>
            <p className="text-gray-600">
              Identify and track vulnerabilities in your components
            </p>
          </div>
        </div>

        <div className="bg-gradient-to-r from-blue-500 to-indigo-600 rounded-lg shadow-xl p-8 text-white text-center">
          <h2 className="text-2xl font-bold mb-4">Get Started</h2>
          <p className="mb-6">
            Begin by creating your first threat model or explore the threat catalog
          </p>
          <div className="flex gap-4 justify-center">
            <Button variant="secondary" size="lg">
              Create Threat Model
            </Button>
            <Button variant="outline" size="lg" className="bg-white text-indigo-600 hover:bg-gray-100">
              Browse Threats
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
