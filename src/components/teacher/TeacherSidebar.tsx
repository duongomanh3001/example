"use client";

import { useState } from "react";
import SidebarWrapper from "../common/SidebarWrapper";
import { teacherNavigation } from "@/constants/navigation";

export default function TeacherSidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  return (
    <SidebarWrapper
      items={teacherNavigation}
      isCollapsed={isCollapsed}
      onToggle={toggleSidebar}
    />
  );
}