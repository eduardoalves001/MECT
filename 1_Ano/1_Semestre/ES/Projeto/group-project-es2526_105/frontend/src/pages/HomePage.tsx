import { Button } from "@/components/ui/button";
import { useRouter } from "@/hooks/useRouter";

export function HomePage() {
  const { navigateTo } = useRouter();

  const handleClick = () => {
    navigateTo("models");
  };

  return (
    <div className="flex min-h-[calc(100vh-120px)] flex-col items-center justify-center space-y-4">
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold text-primary">
          Welcome to RTMP
        </h1>
        <p className="text-xl text-muted-foreground">
          Risk & Threat Modelling Platform
        </p>
        <Button onClick={handleClick} size="lg">
          Get Started
        </Button>
      </div>
    </div>
  );
}