"use client";

import { useState } from "react";
import SidebarWrapper from "../common/SidebarWrapper";
import { adminNavigation } from "@/constants/navigation";

export default function AdminSidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  return (
    <SidebarWrapper
      items={adminNavigation}
      isCollapsed={isCollapsed}
      onToggle={toggleSidebar}
    />
  );
}