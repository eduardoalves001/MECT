import { type ReactNode } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { ShieldLogo } from "@/components/ui/shield-logo";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger
} from "@/components/ui/sidebar";
import { Box, Home, LogOut, User, MessageSquare } from "lucide-react";
import { useRouter } from "@/hooks/useRouter";
import { useAuth } from "@/contexts/AuthContext";
import { useFeatureFlags } from "@/hooks/useFeatureFlags";
import { usePermissions } from "@/hooks/usePermissions";
import { NotificationBell } from "@/components/custom/NotificationBell";

// Sidebar component using only shadcn components
function AppSidebar() {
  const { currentPage, navigateTo } = useRouter();
  const { isFeatureEnabled } = useFeatureFlags();
  const { hasPermission } = usePermissions();

  const showChat = isFeatureEnabled("enable_chatbot") && hasPermission("chatbot:use");

  return (
    <Sidebar>
      <SidebarContent className="pt-2">
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton
                  isActive={currentPage === "home"}
                  onClick={() => navigateTo("home")}
                >
                  <Home />
                  <span>Home</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton
                  isActive={currentPage === "models"}
                  onClick={() => navigateTo("models")}
                >
                  <Box />
                  <span>Models</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton
                  isActive={currentPage === "threats"}
                  onClick={() => navigateTo("threats")}
                >
                  <ShieldLogo />
                  <span>Threats</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
              {showChat && (
                <SidebarMenuItem>
                  <SidebarMenuButton
                    isActive={currentPage === "chat"}
                    onClick={() => navigateTo("chat")}
                  >
                    <MessageSquare />
                    <span>Chat</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}

// Navbar component
function Navbar() {
  const { user, logout } = useAuth();

  const getUserInitials = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    if (user?.username) {
      return user.username.substring(0, 2).toUpperCase();
    }
    return "U";
  };

  return (
    <div className="flex items-center justify-between px-4 py-3 border-b bg-background">
      <div className="flex items-center gap-3 min-w-0">
        <SidebarTrigger className="flex-shrink-0" />
        <div className="flex items-center gap-2 min-w-0">
          <ShieldLogo className="w-7 h-7 flex-shrink-0" />
          <span className="text-lg font-semibold text-primary truncate">
            RTMP
          </span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <NotificationBell />
        <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="relative h-8 w-8 rounded-full p-0">
            <Avatar className="h-8 w-8">
              <AvatarImage src="" alt={user?.username || "User"} />
              <AvatarFallback className="bg-primary text-primary-foreground text-sm">
                {getUserInitials()}
              </AvatarFallback>
            </Avatar>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-56" align="end" forceMount>
          <DropdownMenuLabel className="font-normal">
            <div className="flex flex-col space-y-1">
              <p className="text-sm font-medium leading-none">
                {user?.firstName && user?.lastName
                  ? `${user.firstName} ${user.lastName}`
                  : user?.username || "User"}
              </p>
              <p className="text-xs leading-none text-muted-foreground">
                {user?.email || "No email"}
              </p>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem>
            <User className="mr-2 h-4 w-4" />
            <span>Profile</span>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={logout} className="text-red-600">
            <LogOut className="mr-2 h-4 w-4" />
            <span>Log out</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}

// Main Layout component
interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  return (
    <SidebarProvider>
      <AppSidebar />
      <main className="flex-1">
        <Navbar />
        <div className="p-6 mx-3">
          {children}
        </div>
      </main>
    </SidebarProvider>
  );
}