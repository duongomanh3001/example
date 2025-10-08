"use client";

import { useState } from "react";
import SidebarWrapper from "../common/SidebarWrapper";
import { studentNavigation } from "@/constants/navigation";

export default function StudentSidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  return (
    <SidebarWrapper
      items={studentNavigation}
      isCollapsed={isCollapsed}
      onToggle={toggleSidebar}
    />
  );
}