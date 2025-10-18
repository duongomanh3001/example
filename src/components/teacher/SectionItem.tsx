"use client";

import { useState } from "react";
import { SectionResponse, AssignmentResponse } from "@/types/api";

interface SectionItemProps {
  section: SectionResponse;
  assignments: AssignmentResponse[];
  onUpdateSection: (sectionId: number, name: string, description?: string) => void;
  onDeleteSection: (sectionId: number) => void;
  onToggleCollapse: (sectionId: number) => void;
  renderAssignmentCard: (assignment: AssignmentResponse) => React.ReactNode;
}

export default function SectionItem({
  section,
  assignments,
  onUpdateSection,
  onDeleteSection,
  onToggleCollapse,
  renderAssignmentCard,
}: SectionItemProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState(section.name);
  const [editDescription, setEditDescription] = useState(section.description || "");
  const [showMenu, setShowMenu] = useState(false);

  const handleSave = () => {
    if (editName.trim()) {
      onUpdateSection(section.id, editName.trim(), editDescription.trim() || undefined);
      setIsEditing(false);
    }
  };

  const handleCancel = () => {
    setEditName(section.name);
    setEditDescription(section.description || "");
    setIsEditing(false);
  };

  return (
    <div className="border border-slate-200 rounded-lg overflow-hidden bg-white">
      {/* Section Header */}
      <div className="bg-slate-50 border-b border-slate-200">
        <div className="flex items-center justify-between p-3">
          <div className="flex items-center gap-2 flex-1">
            {/* Toggle Collapse Button */}
            <button
              onClick={() => onToggleCollapse(section.id)}
              className="p-1 hover:bg-slate-200 rounded transition-colors"
              aria-label={section.isCollapsed ? "Mở rộng" : "Thu gọn"}
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="currentColor"
                className={`transition-transform duration-200 ${
                  section.isCollapsed ? "-rotate-90" : ""
                }`}
              >
                <path d="M4 6l4 4 4-4H4z" />
              </svg>
            </button>

            {/* Section Name/Edit Form */}
            {isEditing ? (
              <div className="flex-1 flex items-center gap-2">
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="flex-1 px-2 py-1 border border-slate-300 rounded text-sm"
                  placeholder="Tên phân mục"
                  autoFocus
                />
                <input
                  type="text"
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  className="flex-1 px-2 py-1 border border-slate-300 rounded text-sm"
                  placeholder="Mô tả (tùy chọn)"
                />
                <button
                  onClick={handleSave}
                  className="px-3 py-1 bg-[#ff6a00] text-white rounded text-sm hover:bg-[#e55a00]"
                >
                  Lưu
                </button>
                <button
                  onClick={handleCancel}
                  className="px-3 py-1 bg-slate-200 text-slate-700 rounded text-sm hover:bg-slate-300"
                >
                  Hủy
                </button>
              </div>
            ) : (
              <div className="flex-1">
                <h3 className="font-semibold text-slate-900 text-sm">
                  {section.name}
                  <span className="ml-2 text-xs text-slate-500 font-normal">
                    ({assignments.length} bài tập)
                  </span>
                </h3>
                {section.description && (
                  <p className="text-xs text-slate-600 mt-0.5">
                    {section.description}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* Section Actions */}
          {!isEditing && (
            <div className="relative">
              <button
                onClick={() => setShowMenu(!showMenu)}
                className="p-1 hover:bg-slate-200 rounded transition-colors"
                aria-label="Menu"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                  <circle cx="8" cy="3" r="1.5" />
                  <circle cx="8" cy="8" r="1.5" />
                  <circle cx="8" cy="13" r="1.5" />
                </svg>
              </button>

              {showMenu && (
                <>
                  <div
                    className="fixed inset-0 z-10"
                    onClick={() => setShowMenu(false)}
                  />
                  <div className="absolute right-0 mt-1 w-40 bg-white border border-slate-200 rounded-lg shadow-lg z-20">
                    <button
                      onClick={() => {
                        setIsEditing(true);
                        setShowMenu(false);
                      }}
                      className="w-full text-left px-4 py-2 text-sm hover:bg-slate-50 transition-colors"
                    >
                      Chỉnh sửa
                    </button>
                    <button
                      onClick={() => {
                        if (
                          confirm(
                            `Bạn có chắc muốn xóa phân mục "${section.name}"?\nCác bài tập trong phân mục sẽ được chuyển về "Chưa phân loại".`
                          )
                        ) {
                          onDeleteSection(section.id);
                        }
                        setShowMenu(false);
                      }}
                      className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors border-t border-slate-100"
                    >
                      Xóa phân mục
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Section Content - Assignments */}
      {!section.isCollapsed && (
        <div className="p-3 space-y-2 bg-slate-25">
          {assignments.length === 0 ? (
            <div className="text-center py-6 text-slate-400 text-sm">
              Chưa có bài tập trong phân mục này
            </div>
          ) : (
            assignments.map((assignment) => renderAssignmentCard(assignment))
          )}
        </div>
      )}
    </div>
  );
}
